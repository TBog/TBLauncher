package rocks.tbog.tblauncher.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.ContextThemeWrapper;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.R;

public class DialogHelper {

    public interface OnRename {
        void rename(Dialog dialog, String name);
    }

    public static DialogBuilder makeRenameDialog(@NonNull Context ctx, CharSequence currentName, @NonNull OnRename callback) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        String theme = sharedPreferences.getString("settings-theme", null);
        // get activity theme for this dialog
        Context themeWrapper = ctx;
        if (theme != null) {
            if (theme.equals("AMOLED"))
                themeWrapper = new ContextThemeWrapper(ctx, R.style.SettingsTheme);
            else if (theme.equals("white"))
                themeWrapper = new ContextThemeWrapper(ctx, R.style.SettingsTheme_White);
            else
                themeWrapper = new ContextThemeWrapper(ctx, R.style.SettingsTheme_DarkBg);
        }
        return DialogBuilder.withContext(themeWrapper)
                .setView(R.layout.dialog_rename)
                .setPositiveButton(R.string.menu_action_rename, (dlg, which) -> {
                    Dialog dialog = (Dialog) dlg;
                    EditText input = dialog.findViewById(R.id.rename);

                    // Set new name
                    String newName = input.getText().toString().trim();
                    callback.rename(dialog, newName);

                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .afterInflate(dialog -> {
                    @SuppressLint("CutPasteId")
                    TextView nameView = ((Dialog) dialog).findViewById(R.id.rename);
                    nameView.setText(currentName);
                    nameView.requestFocus();
                });
    }
}
