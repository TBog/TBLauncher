package rocks.tbog.tblauncher.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

import rocks.tbog.tblauncher.R;

public class FileUtils {
    private static final String TAG = "FileUtils";
    private static final String SETTINGS_FOLDER = "settings";
    private static final String SETTINGS_EXT = ".xml";

    @Nullable
    public static FileDescriptor getFileDescriptor(@NonNull Context context, @NonNull Uri uri) {
        try {
            return context.getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "openFileDescriptor " + uri, e);
        }
        return null;
    }

    @Nullable
    public static String getPath(@NonNull Context context, @NonNull Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"}; // MediaStore.MediaColumns.DATA is deprecated


            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @Nullable
    public static InputStream getInputStream(Context context, Uri uri) {
        FileDescriptor fd = getFileDescriptor(context, uri);
        if (fd != null)
            return new FileInputStream(fd);
        return null;
    }

    private static void sendFile(@NonNull Activity activity, @NonNull String directory, @NonNull String filename, @NonNull String extension, @Nullable ContentGenerator content) {
        Context context = activity.getApplicationContext();
        File cacheDir = new File(context.getCacheDir(), directory);
        File cacheFile = new File(cacheDir, filename + extension);

        if (content != null) {
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
                Log.e(TAG, "Failed to write " + filename, e);
            }
        }

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", cacheFile);

        try {
            String type = "text/plain";
            if (extension.endsWith(SETTINGS_EXT))
                type = ("text/xml");

            Intent intent = ShareCompat.IntentBuilder.from(activity)
                    .setType(type)
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
            Log.d(TAG, "startChooserIntent", e);
            Toast.makeText(activity, context.getString(R.string.error, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }

    public static void sendSettingsFile(@NonNull Activity activity, @NonNull String filename) {
        sendFile(activity, SETTINGS_FOLDER, filename, SETTINGS_EXT, null);
    }

    public static void writeSettingsFile(@NonNull Context context, @NonNull String filename, @NonNull ContentGenerator generator) {
        writeFile(context, SETTINGS_FOLDER, filename, SETTINGS_EXT, generator);
    }

    private static File writeFile(Context context, String directory, String filename, String extension, @NonNull ContentGenerator content) {
        File cacheDir = new File(context.getCacheDir(), directory);
        File cacheFile = new File(cacheDir, filename + extension);

        try {
            cacheDir.mkdirs();
        } catch (Exception ignored) {
        }
        try {
            OutputStream os = new FileOutputStream(cacheFile);
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            content.generate(bw);
            bw.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write " + filename, e);
        }
        return cacheFile;
    }

//    @Nullable
//    private static InputStream getInputStream(Context context, String directory, String filename, String extension) {
//        File cacheDir = new File(context.getCacheDir(), directory);
//        File cacheFile = new File(cacheDir, filename + extension);
//        try {
//            return new FileInputStream(cacheFile);
//        } catch (FileNotFoundException e) {
//            Log.e(TAG, "new FileInputStream " + filename, e);
//        }
//        return null;
//    }

//    @Nullable
//    public static XmlPullParser getSettingsFromFile(@NonNull Context context, @NonNull String filename) {
//        XmlPullParser parser;
//        try {
//            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
//            //xppf.setNamespaceAware(true);
//            parser = xppf.newPullParser();
//        } catch (XmlPullParserException e) {
//            //TODO: implement custom parser if this ever happens
//            Log.e(TAG, "XmlPullParserFactory::newPullParser", e);
//            return null;
//        }
//        InputStream inputStream = getInputStream(context, SETTINGS_FOLDER, filename, SETTINGS_EXT);
//        if (inputStream == null)
//            return null;
//        try {
//            parser.setInput(inputStream, StandardCharsets.UTF_8.name());
//        } catch (XmlPullParserException e) {
//            Log.e(TAG, "XmlPullParser.setInput", e);
//            parser = null;
//        }
//        return parser;
//    }

    @Nullable
    public static XmlPullParser getXmlParser(@NonNull Context context, @Nullable Uri uri) {
        return getXmlParser(context, getInputStream(context, uri));
    }

    @Nullable
    public static XmlPullParser getXmlParser(@NonNull Context context, @Nullable InputStream inputStream) {
        if (inputStream == null)
            return null;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inputStream, StandardCharsets.UTF_8.name());
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParser.setInput", e);
            parser = null;
        }
        return parser;
    }

    public static void chooseFile(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setTypeAndNormalize("text/plain; charset=utf-8");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            activity.startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(activity, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean copyFile(@NonNull Context context, @Nullable Uri uri, @NonNull String filename) {
        if (uri == null)
            return false;
        InputStream is = getInputStream(context, uri);
        if (!(is instanceof FileInputStream)) {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ignored) {
            }
            return false;
        }
        File cacheFile = new File(context.getCacheDir(), filename);
        boolean bCopyOk = false;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            FileOutputStream os = new FileOutputStream(cacheFile);

            inChannel = ((FileInputStream) is).getChannel();
            outChannel = os.getChannel();
            long amount = 8 * 1024;
            final long size = inChannel.size();
            long position = 0;
            while (position < size)
                position += inChannel.transferTo(position, amount, outChannel);

            inChannel.close();
            outChannel.close();
            is.close();
            os.close();

            bCopyOk = true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy " + uri, e);
        }
        try {
            if (inChannel != null)
                inChannel.close();
            else
                is.close();
        } catch (Exception ignored) {
        }
        try {
            if (outChannel != null)
                outChannel.close();
        } catch (Exception ignored) {
        }
        return bCopyOk;
    }

//    private static BufferedReader loadFile(Context context, String directory, String filename, String extension) {
//        File cacheDir = new File(context.getCacheDir(), directory);
//        File cacheFile = new File(cacheDir, filename + extension);
//
//        try {
//            InputStream is = new FileInputStream(cacheFile);
//            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
//            BufferedReader br = new BufferedReader(isr);
//            return br;
////            String line;
////            while ((line = br.readLine()) != null) {
////                text.append(line);
////                text.append('\n');
////            }
////            br.close();
//        }
//        catch (IOException ignored) {
//        }
//        return null;
//    }

    public interface ContentGenerator {
        void generate(Writer writer) throws IOException;
    }
}
