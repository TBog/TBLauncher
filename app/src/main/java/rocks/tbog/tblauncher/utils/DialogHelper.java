package rocks.tbog.tblauncher.utils;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.dialog.EditTextDialog;

public class DialogHelper {

    private static final String TAG = DialogHelper.class.getSimpleName();

    public interface OnRename {
        void rename(Dialog dialog, String name);
    }

    public static EditTextDialog.Builder makeRenameDialog(@NonNull Context ctx, CharSequence currentName, @NonNull OnRename callback) {
        DialogFragment.OnButtonClickListener<CharSequence> listener = (dialog, button) -> {
            if (button == DialogFragment.Button.POSITIVE) {
                EditText input = dialog.findViewById(R.id.rename);
                dialog.onConfirm(input != null ? input.getText() : null);
            }
            dialog.dismiss();
        };

        EditTextDialog.Builder builder = new EditTextDialog.Builder(ctx)
                .setInitialText(currentName)
                .setPositiveButton(R.string.menu_action_rename, listener)
                .setNegativeButton(android.R.string.cancel, listener);

        EditTextDialog dialog = builder.getDialog();
        dialog.setOnConfirmListener(newName -> {
            Log.d(TAG, "rename confirm: `" + newName + "`");
            if (newName == null)
                return;
            callback.rename(dialog.getDialog(), newName.toString().trim());
        });

        //TBApplication.behaviour(ctx).showDialog(dialog, "dialog_rename");
        return builder;
//        // get activity theme for this dialog
//        Context themeWrapper = UITheme.getDialogThemedContext(ctx);
//        return KeyboardDialogBuilder.withContext(themeWrapper)
//                .setView(R.layout.dialog_rename)
//                .setPositiveButton(R.string.menu_action_rename, (dlg, which) -> {
//                    Dialog dialog = (Dialog) dlg;
//                    EditText input = dialog.findViewById(R.id.rename);
//
//                    // Set new name
//                    String newName = input.getText().toString().trim();
//                    callback.rename(dialog, newName);
//
//                    dialog.dismiss();
//                })
//                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
//                .afterInflate(dialog -> {
//                    @SuppressLint("CutPasteId")
//                    EditText nameView = ((Dialog) dialog).findViewById(R.id.rename);
//                    nameView.setText(currentName);
//
////                    showKeyboard((Dialog) dialog, nameView);
////                    nameView.postDelayed(() -> showKeyboard((Dialog) dialog, nameView), 500);
//                    int color = 0xFFffd700;//UIColors.getThemeColor(((Dialog) dialog).getContext(), android.R.attr.textColor);
//                    Utilities.setTextSelectHandleColor(nameView, color);
//                });
    }

//    public static void showKeyboard(@NonNull Dialog dialog, @NonNull TextView textView) {
//        Log.i(TAG, "Keyboard - SHOW");
//        textView.requestFocus();
//
//        InputMethodManager mgr = (InputMethodManager) dialog.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        assert mgr != null;
//        mgr.showSoftInput(textView, InputMethodManager.SHOW_IMPLICIT);
//    }
}
