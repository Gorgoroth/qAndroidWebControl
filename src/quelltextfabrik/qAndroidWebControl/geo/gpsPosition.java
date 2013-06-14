package quelltextfabrik.qAndroidWebControl.geo;

import java.util.List;
import java.util.Locale;

import quelltextfabrik.qAndroidWebControl.gpsPositionService;
import quelltextfabrik.qAndroidWebControl.XmppService;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;


public class gpsPosition {
    
    /** Starts the geolocation service */
    public static void start() {
        Intent intent = new Intent(XmppService.getInstance(), gpsPositionService.class);
        XmppService.getInstance().startService(intent);
    }

    /** Stops the geolocation service */
    public static void stop() {
        Intent intent = new Intent(XmppService.getInstance(), gpsPositionService.class);
        XmppService.getInstance().stopService(intent);
    }
    
    /** launches google maps on the specified url */
    public void maps(String url) {
        try {
            if(!url.startsWith("geo:")) {
                url = "geo:0,0?q=" + url.replace(" ", "+");
            }
            launchExternal(url);
            // send("Map on \"" + url + "\".");
        }
        catch(Exception ex)
        {
         // send("\"" + url + "\" not supported");
        }
    }

    /** launches navigate on the specified url */
    public void navigate(String url) {
        try
        {
            if(!url.startsWith("google.navigation:")) {
                url = "google.navigation:q=" + url.replace(" ", "+");
            }
            launchExternal(url);
         // send("Navigate to \"" + url + "\".");
        }
        catch(Exception ex) {
         // send("\"" + url + "\" not supported");
        }
    }

    /** launches streetview on the specified url */
    public void streetView(String url) {
        try {
            Geocoder geo = new Geocoder(XmppService.getInstance().getBaseContext(), Locale.getDefault());
            List<Address> addresses = geo.getFromLocationName(url, 10);
            if (addresses.size() > 1) {
             // send("Specify more details:");
                for (Address address : addresses) {
                    StringBuilder addr = new StringBuilder();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        addr.append(address.getAddressLine(i) + "\n");
                    }
                    //send(addr.toString());
                }
            }
            else if (addresses.size() == 1) {
                Address address = addresses.get(0);
                launchExternal("google.streetview:cbll=" + address.getLatitude() + "," + address.getLongitude());
                StringBuilder addr = new StringBuilder();
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addr.append(address.getAddressLine(i) + "\n");
                }
             // send("Street View on \"" + addr + "\".");
            }
        }
        catch(Exception ex) {
         // send("\"" + url + "\" not supported");
        }
    }

    /** launches an activity on the url */
    private void launchExternal(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        XmppService.getInstance().startActivity(intent);
    }
}
