package rocks.tbog.tblauncher.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.R;

public class DialogHelper {

    private static final String TAG = DialogHelper.class.getSimpleName();

    public interface OnRename {
        void rename(Dialog dialog, String name);
    }

    public static DialogBuilder makeRenameDialog(@NonNull Context ctx, CharSequence currentName, @NonNull OnRename callback) {
        // get activity theme for this dialog
        Context themeWrapper = UITheme.getDialogThemedContext(ctx);
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
                    EditText nameView = ((Dialog) dialog).findViewById(R.id.rename);
                    nameView.setText(currentName);

                    showKeyboard((Dialog) dialog, nameView);
                    nameView.postDelayed(() -> showKeyboard((Dialog) dialog, nameView), 500);
                });
    }

    static void showKeyboard(@NonNull Dialog dialog, @NonNull TextView textView) {
        Log.i(TAG, "Keyboard - SHOW");
        textView.requestFocus();

        InputMethodManager mgr = (InputMethodManager) dialog.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;
        mgr.showSoftInput(textView, InputMethodManager.SHOW_IMPLICIT);
    }
}
