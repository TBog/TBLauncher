package rocks.tbog.tblauncher;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

public class TBApplication extends Application {

    public static final int TOUCH_DELAY = 120;
    private DataHandler dataHandler;
    private IconsHandler iconsPackHandler;

    public static TBApplication getApplication(Context context) {
        return (TBApplication) context.getApplicationContext();
    }

    public DataHandler getDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        return dataHandler;
    }

    public void initDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        else if(dataHandler.allProvidersHaveLoaded) {
            // Already loaded! We still need to fire the FULL_LOAD event
            Intent i = new Intent(TBLauncherActivity.FULL_LOAD_OVER);
            sendBroadcast(i);
        }
    }

    public IconsHandler getIconsHandler() {
        if (iconsPackHandler == null) {
            iconsPackHandler = new IconsHandler(this);
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
    }
}
