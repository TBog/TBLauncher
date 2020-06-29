package rocks.tbog.tblauncher.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.AppProvider;

public class LocaleChangedReceiver extends BroadcastReceiver {

    @Override
    @SuppressWarnings("CatchAndPrintStackTrace")
    public void onReceive(Context ctx, Intent intent) {
        // Only handle system broadcasts
        if (!"android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
            return;
        }

        //TODO: Enable when we have tags
//        try {
//            // If new locale, then reset tags to load the correct aliases
//            TBApplication.getApplication(ctx).getDataHandler().resetTagsHandler();
//        }
//        catch(IllegalStateException e) {
//            // Since Android 8.1, we're not allowed to create a new service
//            // when the app is not running
//            e.printStackTrace();
//        }

        // Reload application list
        final AppProvider provider = TBApplication.getApplication(ctx).getDataHandler().getAppProvider();
        if (provider != null) {
            provider.reload(true);
        }
    }
}
