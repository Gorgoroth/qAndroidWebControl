package quelltextfabrik.qAndroidWebControl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class qAndroidWebControl extends Activity {	
    // --- On creation of activity --------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);             
        setContentView(R.layout.main);
        
        // --- Menu ---------------------------------------------------
        
        // --- Start/Stop Service ---------------------------
        menuServHandler();
        
        // --- Preferences ----------------------------------
        menuPrefHandler();        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;
    }
    
    // This method is called once the menu is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// We have only one menu option
		case R.id.preferences:
			// Launch Preference activity
			Intent i = new Intent(qAndroidWebControl.this, Preferences.class);
			startActivity(i);
			// A toast is a view containing a quick little message for the user.
			Toast.makeText(qAndroidWebControl.this,
					"Here you can maintain your user credentials.",
					Toast.LENGTH_LONG).show();
			break;

		}
		return true;
	}
    
    /*						 |||||
    						( o o )
    +------------------.oooO--(_)--Oooo.------------------+
    |                                                     |
    |    Name:		menuServHandler()     			      |
    |    Parameter: none                                  |
    |    Returns:	none                                  |
    |                                                     |
    |    Desc:		Handles the Start/Stop menu function. |
    |                                                     |
    |                    .oooO                            |
    |                    (   )   Oooo.                    |
    +---------------------\ (----(   )--------------------+
                   		   \_)    ) /
                         		 (_/						*/
    private void menuServHandler()
    {
    	TextView servButton = (TextView) findViewById(R.id.myStartStopServiceButton);
    	servButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) 
            {
            	// ##todo## 
            	// Check server state before displaying text on this button
                TextView startStopServiceButton = (TextView) findViewById(R.id.myStartStopServiceButton);
                Intent intent = new Intent(".qAndroidWebControl.ACTION");
                if (XmppService.getInstance() == null) {
                    startService(intent);
            		startStopServiceButton.setText(R.string.StopService);
                }
                else {
                	stopService(intent);
            		startStopServiceButton.setText(R.string.StartService);
                }            	
            }
        });
    }
    
    
    /*						 |||||
							( o o )
	+------------------.oooO--(_)--Oooo.------------------+
	|                                                     |
	|    Name:		menuServHandler()     			      |
	|    Parameter: none                                  |
	|    Returns:	none                                  |
	|                                                     |
	|    Desc:		Handles the preferences menu func.    |
	|                                                     |
	|                    .oooO                            |
	|                    (   )   Oooo.                    |
	+---------------------\ (----(   )--------------------+
		   				   \_)    ) /
	 		 					 (_/						*/
	private void menuPrefHandler()
	{
		// Initialize preferences
        TextView button = (TextView) findViewById(R.id.myPreferenceButton);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
                Intent prefs = new Intent(getBaseContext(), Preferences.class);
				startActivity(prefs);
			}
		});
	}   
    
}

