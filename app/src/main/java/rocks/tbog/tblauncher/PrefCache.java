package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class PrefCache {

    private static int RESULT_HISTORY_SIZE = 0;
    private static int RESULT_HISTORY_ADAPTIVE = 0;

    private PrefCache() {
    }

    public static void resetCache() {
        RESULT_HISTORY_SIZE = 0;
        RESULT_HISTORY_ADAPTIVE = 0;
    }

    public static int getResultHistorySize(Context context) {
        if (RESULT_HISTORY_SIZE == 0)
        {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_result_history_size);
            RESULT_HISTORY_SIZE = pref.getInt("result-history-size", defaultSize);
        }
        return RESULT_HISTORY_SIZE;
    }

    public static int getHistoryAdaptive(Context context) {
        if (RESULT_HISTORY_ADAPTIVE == 0)
        {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_result_history_adaptive);
            RESULT_HISTORY_ADAPTIVE = pref.getInt("result-history-adaptive", defaultSize);
        }
        return RESULT_HISTORY_ADAPTIVE;
    }
}
