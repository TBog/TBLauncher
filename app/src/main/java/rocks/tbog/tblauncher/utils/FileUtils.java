package rocks.tbog.tblauncher.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import rocks.tbog.tblauncher.R;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public interface ContentGenerator {
        void generate(Writer writer) throws IOException;
    }

    public static void sendFile(@NonNull Activity activity, @NonNull String directory, @NonNull String filename, @NonNull String extension, @NonNull ContentGenerator content) {
        Context context = activity.getApplicationContext();
        File cacheDir = new File(context.getCacheDir(), directory);
        File cacheFile = new File(cacheDir, filename + extension);

        {
            try {
                cacheDir.mkdirs();
            } catch (Exception ignored) {
            }
            try {
                FileWriter fw = new FileWriter(cacheFile);
                BufferedWriter bw = new BufferedWriter(fw);
                //bw.write(content);
                content.generate(bw);
                bw.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to write", e);
            }
        }

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", cacheFile);

        try {
            Intent intent = ShareCompat.IntentBuilder.from(activity)
                    .setType("text/xml")
                    //.setSubject(context.getString(R.string.share_subject))
                    .setSubject("[subject]")
                    .setStream(uri)
                    //.setChooserTitle(R.string.share_title)
                    .setChooserTitle("[chooserTitle]")
                    .createChooserIntent()
                    .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // grant permission for all apps that can handle given intent
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            activity.startActivity(intent);
        } catch (Exception e) {
            Log.d(TAG, "startActivity", e);
            Toast.makeText(activity, context.getString(R.string.error, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }

    public static void sendSettingsFile(@NonNull Activity activity, @NonNull String filename, ContentGenerator generator) {
        sendFile(activity, "settings", filename, ".xml", generator);
    }

}
