package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RootHandler {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private Boolean isRootAvailable = null;
    private Boolean isRootActivated = null;

    public RootHandler(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        resetRootHandler(prefs);
    }

    public boolean isRootActivated() {
        return this.isRootActivated;
    }

    public void resetRootHandler(SharedPreferences prefs) {
        isRootActivated = prefs.getBoolean("root-mode", false);
        isRootAvailable = null;
    }

    public boolean isRootAvailable() {

        if (isRootAvailable == null) {
            try {
                isRootAvailable = executeRootShell(null);
            } catch (Exception e) {
                isRootAvailable = false;
            }
        }

        return isRootAvailable;
    }

    public boolean hibernateApp(String packageName) {
        try {
            return executeRootShell("am force-stop " + packageName);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean executeRootShell(String command) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            //put command
            if (command != null && !command.trim().equals("")) {
                p.getOutputStream().write((command + "\n").getBytes(UTF_8));
            }
            //exit from su command
            p.getOutputStream().write("exit\n".getBytes(UTF_8));
            p.getOutputStream().flush();
            p.getOutputStream().close();
            int result = p.waitFor();
            if (result != 0)
                throw new Exception("Command execution failed " + result);
            return true;
        } catch (Exception e) {
            Log.e("RootHandler", command, e);
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return false;
    }

}
