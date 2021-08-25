package rocks.tbog.tblauncher.shortcut;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.ShortcutsProvider;
import rocks.tbog.tblauncher.db.ShortcutRecord;

@TargetApi(Build.VERSION_CODES.O)
public class SaveSingleOreoShortcutAsync extends AsyncTask<Void, Integer, Boolean> {

    private static final String TAG = "OreoShortcutAsync";
    private final WeakReference<Context> context;
    private final WeakReference<DataHandler> dataHandler;
    private final Intent intent;

    public SaveSingleOreoShortcutAsync(@NonNull Context context, @NonNull Intent intent) {
        this.context = new WeakReference<>(context);
        this.dataHandler = new WeakReference<>(TBApplication.getApplication(context).getDataHandler());
        this.intent = intent;
    }


    @Override
    protected Boolean doInBackground(Void... voids) {

        final LauncherApps.PinItemRequest pinItemRequest = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final ShortcutInfo shortcutInfo = pinItemRequest != null ? pinItemRequest.getShortcutInfo() : null;

        if (shortcutInfo == null) {
            cancel(true);
            return null;
        }

        Context context = this.context.get();
        if (context == null) {
            cancel(true);
            return null;
        }

        // Create Pojo
        ShortcutRecord record = ShortcutUtil.createShortcutRecord(context, shortcutInfo, false);
        if (record == null) {
            return false;
        }

        final DataHandler dataHandler = this.dataHandler.get();
        if (dataHandler == null) {
            cancel(true);
            return null;
        }

        try {
            if (!pinItemRequest.accept())
                return false;
        } catch (IllegalStateException e) {
            return false;
        }

        // Add shortcut to the DataHandler
        return dataHandler.addShortcut(record);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == -1) {
            Toast.makeText(context.get(), R.string.cant_pin_shortcut, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPostExecute(@NonNull Boolean success) {
        if (success) {
            Log.i(TAG, "Shortcut added to KISS");

            final DataHandler dataHandler = this.dataHandler.get();
            if (dataHandler == null)
                return;
            ShortcutsProvider shortcutsProvider = dataHandler.getShortcutsProvider();
            if (shortcutsProvider != null)
                shortcutsProvider.reload(true);
        }
    }

}
