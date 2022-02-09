package rocks.tbog.tblauncher.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import rocks.tbog.tblauncher.CustomizeUI;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.ui.dialog.EditTextDialog;

public class DialogHelper {

    private static final String TAG = DialogHelper.class.getSimpleName();

    public static void setCustomTitle(AlertDialog.Builder builder, CharSequence title) {
        // Dialog doesn't provide a view we can send as root
        @SuppressLint("InflateParams")
        View customTitle = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_title, null, false);
        TextView titleView = customTitle.findViewById(android.R.id.title);
        titleView.setText(title);
        builder.setCustomTitle(customTitle);
    }

    public static void setButtonBarBackground(Dialog dialog) {
        Context ctx = dialog.getContext();
        View buttonLayout;
        @IdRes int buttonPanel;

        // try to find the `buttonPanel` defined in the android layout
        buttonPanel = ctx.getResources().getIdentifier("buttonPanel", "id", "android");
        buttonLayout = dialog.findViewById(buttonPanel);

        // try to find the `buttonPanel` defined in the androidx layout
        if (buttonLayout == null) {
            buttonPanel = ctx.getResources().getIdentifier("buttonPanel", "id", ctx.getPackageName());
            buttonLayout = dialog.findViewById(buttonPanel);
        }

        // hack: can't find the button container by id, get the parent of one button
        if (buttonLayout == null) {
            View button = dialog.findViewById(android.R.id.button1);
            ViewParent parent = button == null ? null : button.getParent();
            if (parent instanceof View) {
                buttonLayout = (View) parent;

                // assuming the buttonPanel is inflated from `abc_alert_dialog_button_bar_material.xml`
                if (buttonLayout instanceof androidx.appcompat.widget.ButtonBarLayout) {
                    parent = buttonLayout.getParent();
                    if (parent instanceof android.widget.ScrollView)
                        buttonLayout = (View) parent;
                }
            }
        }

        // apply the background
        if (buttonLayout != null) {
            Drawable background = CustomizeUI.getDialogButtonBarBackgroundDrawable(ctx.getTheme());
            if (background != null)
                buttonLayout.setBackground(background);
        }
    }

    public interface OnRename {
        void rename(Dialog dialog, String name);
    }

    public static EditTextDialog.Builder makeRenameDialog(@NonNull Context ctx, CharSequence currentName, @NonNull OnRename callback) {
//        // get activity theme for this dialog
//        Context themeWrapper = UITheme.getDialogThemedContext(ctx);
        EditTextDialog.Builder builder = new EditTextDialog.Builder(ctx)
            .setInitialText(currentName)
            .setPositiveButton(R.string.menu_action_rename, (dialog, button) -> {
                EditText input = dialog.findViewById(R.id.rename);
                dialog.onConfirm(input != null ? input.getText() : null);
            })
            .setNegativeButton(android.R.string.cancel, null);

        EditTextDialog dialog = builder.getDialog();
        dialog.setOnConfirmListener(newName -> {
            Log.d(TAG, "rename confirm: `" + newName + "`");
            if (newName == null)
                return;
            callback.rename(dialog.getDialog(), newName.toString().trim());
        });

        return builder;
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
}
