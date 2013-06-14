package quelltextfabrik.qAndroidWebControl;

import java.util.ArrayList;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import quelltextfabrik.qAndroidWebControl.geo.gpsPosition;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Toast;

public class XmppService extends Service {
    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;

    // Indicates the current state of the service (disconnected/connecting/connected)
    private int mStatus = DISCONNECTED;
    private int receiver = -1;
    
    // Service instance
    private static XmppService instance = null;
    
    // XMPP connection
    private String mLogin;
    private String mPassword;
    private String mTarget;
    private ConnectionConfiguration mConnectionConfiguration = null;
    private XMPPConnection mConnection = null;
    private PacketListener mPacketListener = null;
    private boolean permConnNotify;
    
	// ring
    private MediaPlayer mMediaPlayer = null;
    private String ringtone = null;


    //

    // battery
    private BroadcastReceiver mBatInfoReceiver = null;

	
    private void importPreferences() {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
 
        mTarget		= preferences.getString("jabberTarget", "") + "@qtf.dyndns.org";
        mLogin		= preferences.getString("jabberUser", "");
        mPassword	= preferences.getString("jabberPW", "");

        // ##todo## Check for sensible values in Settings
        
        permConnNotify	= preferences.getBoolean("permConnNotify", true);
                	        
        // ##todo## Retreive server settings from preferences
        mConnectionConfiguration = new ConnectionConfiguration("qtf.dyndns.org", 5222, "android");
        ringtone = Settings.System.DEFAULT_RINGTONE_URI.toString();
    }


    /** clears the XMPP connection */
    public void clearConnection() {
        if (mConnection != null) {
            if (mPacketListener != null) {
                mConnection.removePacketListener(mPacketListener);
            }
            // don't try to disconnect if already disconnected
            if (isConnected()) {
                mConnection.disconnect();
            }
        }
        mConnection = null;
        mPacketListener = null;
        mConnectionConfiguration = null;
        mStatus = DISCONNECTED;
    }

    /** init the XMPP connection */
    public void initConnection()
    {        	
    	mStatus = CONNECTING;
    	
        if (mConnectionConfiguration == null) { importPreferences(); }
        
        mConnection = new XMPPConnection(mConnectionConfiguration);
        
        try {
            mConnection.connect();
        } catch (XMPPException e) {
            Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show();
            mStatus = DISCONNECTED;
            return;
        }
        
        try {
            mConnection.login(mLogin, mPassword);
        } catch (XMPPException e) {
            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
            mStatus = DISCONNECTED;
            return;
        }
        
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        
        mPacketListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                
                if (    message.getFrom().toLowerCase().startsWith(mTarget.toLowerCase())
                    && !message.getFrom().equals(mConnection.getUser()) // filters self-messages
                ) {
                    if (message.getBody() != null) {
                        onCommandReceived(message.getBody());
                    }
                }
            }
        };
        
        mConnection.addPacketListener(mPacketListener, filter);
        
        mStatus = CONNECTED;
        
        // Send welcome message
        if(permConnNotify) { send("status::online"); }
    }

    /** returns true if the service is correctly connected */
    public boolean isConnected()
    {
        return    (mConnection != null
                && mConnection.isConnected()
                && mConnection.isAuthenticated());
    }

    private void _onStart()
    {
        // Get configuration
        if (instance == null) {
            instance = this;

            mStatus = DISCONNECTED;

            // first, clean everything
            clearConnection();
            clearMediaPlayer();
            clearBatteryMonitor();

            // then, re-import preferences
            importPreferences();

            initMediaPlayer();
            initBatteryMonitor();
            initConnection();

            if (!isConnected()) { onDestroy(); }
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
        }
    }

    public static XmppService getInstance() {
        return instance;
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onStart(Intent intent, int startId) {
        _onStart();
    };

    @Override
    public void onDestroy() {
        clearConnection();
        instance = null;
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
    }

    /** sends a message to the user */
    public void send(String message) {
        if (isConnected()) {
            Message msg = new Message(mTarget, Message.Type.chat);
            msg.setBody(message);
            mConnection.sendPacket(msg);
        }
    }

    /** handles the different commands */
    private void onCommandReceived(String commandLine) {
        try {
            String command;
            String args;
            int idx;
            
            // --- Split command and arguments here --------------------------
            if ((idx = commandLine.indexOf("::")) != (-1)) {
            	command = commandLine.substring(0, idx);
                args = commandLine.substring(commandLine.indexOf("::") + 2);
            } else {
            	command = commandLine;
                args = "";
            }

            // Not case sensitive commands
            command = command.toLowerCase();
            
            if (command.equals("sms")) {
                int separatorPos = args.indexOf("::");
                String number = null;
                String message = null;
                if (-1 != separatorPos) {
                	number = args.substring(0, separatorPos);
                    message = args.substring(separatorPos + 2);
                    sendSMS(number, message); 
                }
            } else if (command.equals("gps")) {  
            	if(args.equals("start")) {
            		send("gps::start");    
	                gpsPosition.start();    		
            	} else {
	                send("gps::stop");
	                gpsPosition.stop();
            	}
            } else if (command.equals("ring")) {
            	if(args.equals("start")) {
            		send("ring::start");
	            	startRing();            		
            	} else {
	                send("ring::stop");
	            	stopRing();
            	}
            } else {
                send("status::" + commandLine + ": unknown command");
            }
        } catch (Exception ex) {
            send("Error : " + ex);
        }
    }      
        
    private void sendSMS(String phoneNumber, String message)
    { 		
        SmsManager sms = SmsManager.getDefault();		
    	String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";
		String SMS_ADDRESS_PARAM = "SMS_ADDRESS_PARAM";
		
		ArrayList<PendingIntent> lSentPI		= new ArrayList<PendingIntent> (0); 
		ArrayList<PendingIntent> ldeliveredPI	= new ArrayList<PendingIntent> (0);
		
		if(receiver != 0) { // todo: we should outsource this into a seperate init method
	    	receiver = 0;
			//--- When a SMS has been sent -------------------------------------------------------------------
			registerReceiver(new BroadcastReceiver(){
			    @Override
			    public void onReceive(Context arg0, Intent arg1) {
			        switch (getResultCode())
			        {
			            case Activity.RESULT_OK:
			                send("sms::sent");
			                break;
			            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			                send("sms::Generic failure");
			                break;
			            case SmsManager.RESULT_ERROR_NO_SERVICE:
			                send("sms::No service");
			                break;
			            case SmsManager.RESULT_ERROR_NULL_PDU:
			                send("sms::Null PDU");
			                break;
			            case SmsManager.RESULT_ERROR_RADIO_OFF:
			                send("sms::Radio off");
			                break;
			        }
			    }
			}, new IntentFilter(SENT));
			
			//--- When a SMS has been delivered --------------------------------------------------------------
			registerReceiver(new BroadcastReceiver(){
			    @Override
			    public void onReceive(Context arg0, Intent arg1) {
			        switch (getResultCode())
			        {
			            case Activity.RESULT_OK:
			                send("sms::delivered");
			                break;
			            case Activity.RESULT_CANCELED:
			                send("sms::not delivered");
			                break;                        
			        }
			    }
			}, new IntentFilter(DELIVERED)); 
		}


		// --- Split message, if necessary ---------------------------------------------------------------
        ArrayList<String> parts = sms.divideMessage(message);
        send("sms::" + parts.size() + " parts");
		
        // --- Fill list of intents according to number of parts -----------------------------------------
        for (int i=0; i < parts.size(); i++){
        	// --- Sent intents ---
        	Intent sntInt = new Intent(SENT);
        	sntInt.putExtra(SMS_ADDRESS_PARAM, phoneNumber);
			PendingIntent spi = PendingIntent.getBroadcast(this, 0, sntInt, PendingIntent.FLAG_CANCEL_CURRENT); 
			lSentPI.add(spi);
			
			// --- Delivered intents ---
        	Intent dlvdInt = new Intent(DELIVERED);
        	dlvdInt.putExtra(SMS_ADDRESS_PARAM, phoneNumber);
			PendingIntent dpi = PendingIntent.getBroadcast(this, 0, dlvdInt, PendingIntent.FLAG_CANCEL_CURRENT); 
			ldeliveredPI.add(dpi);
		}
        
        
		// --- Send the sms ------------------------------------------------------------------------------
        sms.sendMultipartTextMessage(phoneNumber, null, parts, lSentPI, ldeliveredPI);
    } 
    
    // --- Clears the media player -------------------------------------------------------
    private void clearMediaPlayer()
    {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        mMediaPlayer = null;
    }

    // --- Initializes the media player --------------------------------------------------
    private void initMediaPlayer()
    {
        Uri alert = Uri.parse(ringtone);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, alert);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "ring::Error initializing ringer", Toast.LENGTH_LONG).show();
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mMediaPlayer.setLooping(true);
    }
    
    // Start ringing the phone -----------------------------------------------------------
    private void startRing()
    {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            try {
                mMediaPlayer.prepare();
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "ring::Error preparing ringer", Toast.LENGTH_LONG).show();
            }
            mMediaPlayer.start();
        }
    }
    
    // --- Stops ringing the phone -------------------------------------------------------
    private void stopRing()
    {
        mMediaPlayer.stop();
    }
    
    
    /** clear the battery monitor*/
    private void clearBatteryMonitor() {
        if (mBatInfoReceiver != null) {
            unregisterReceiver(mBatInfoReceiver);
        }
        mBatInfoReceiver = null;
    }
    
    /** init the battery stuff */
    private void initBatteryMonitor() {
        mBatInfoReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent intent) {
                int level = intent.getIntExtra("level", 0);
                int type = intent.getIntExtra("plugged", 0);
                notifyAndSavePercentage(level, type);
            }
            private void notifyAndSavePercentage(int level, int type) {
                send("battery::level::" + level);
                send("battery::type::" + type);
            }
        };
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
    }
}
