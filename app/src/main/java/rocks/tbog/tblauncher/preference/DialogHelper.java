package rocks.tbog.tblauncher.preference;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;

public class DialogHelper {
    public static void setCustomTitle(AlertDialog.Builder builder, CharSequence title) {
        // Dialog doesn't provide a view we can send as root
        @SuppressLint("InflateParams")
        View customTitle = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_title, null, false);
        TextView titleView = customTitle.findViewById(android.R.id.title);
        titleView.setText(title);
        builder.setCustomTitle(customTitle);
    }

    public static void setButtonBarBackground(Dialog dialog) {
        View buttonLayout;
        @IdRes
        int buttonPanel = dialog.getContext().getResources().getIdentifier("buttonPanel", "id", "android");
        buttonLayout = dialog.findViewById(buttonPanel);
        if (buttonLayout == null) {
            // hack: can't find the button container by id, get the parent of one button
            View button = dialog.findViewById(android.R.id.button1);
            ViewParent parent = button.getParent();
            if (parent instanceof View) {
                buttonLayout = (View) parent;
            }
        }
        if (buttonLayout != null) {
            Context ctx = dialog.getContext();
            Drawable background = TBApplication.ui(ctx).getDialogButtonBarBackgroundDrawable(ctx.getTheme());
            if (background != null)
                buttonLayout.setBackground(background);
        }
    }
}
