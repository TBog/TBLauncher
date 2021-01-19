package rocks.tbog.tblauncher.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.R;

public class DialogHelper {

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
                    TextView nameView = ((Dialog) dialog).findViewById(R.id.rename);
                    nameView.setText(currentName);
                    nameView.requestFocus();
                });
    }
}
