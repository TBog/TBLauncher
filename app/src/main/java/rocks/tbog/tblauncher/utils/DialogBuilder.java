package rocks.tbog.tblauncher.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;

public class DialogBuilder {
    final private AlertDialog.Builder builder;
    private DialogInterface.OnShowListener afterInflate = null;

    private DialogBuilder(@NonNull Context context, @StyleRes int theme) {
        builder = new AlertDialog.Builder(context, theme);
    }

    public static DialogBuilder withContext(@NonNull Context context) {
        // resolve alert dialog theme
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.alertDialogTheme, outValue, true);
        int dialogStyle = outValue.resourceId;

        return new DialogBuilder(context, dialogStyle);
    }

    public static DialogBuilder withContext(@NonNull Context context, @StyleRes int theme) {
        ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, theme);
        return withContext(themeWrapper);
    }

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
            ViewParent parent = button == null ? null : button.getParent();
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

    /**
     * Set the title displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setTitle(@StringRes int titleId) {
        CharSequence title = builder.getContext().getText(titleId);
        setCustomTitle(builder, title);
        return this;
    }

    /**
     * Set the title displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setTitle(CharSequence title) {
        setCustomTitle(builder, title);
        return this;
    }

    /**
     * Set a custom view resource to be the contents of the Dialog. The
     * resource will be inflated, adding all top-level views to the screen.
     *
     * @param layoutResId Resource ID to be inflated.
     * @return this Builder object to allow for chaining of calls to set
     * methods
     */
    public DialogBuilder setView(@LayoutRes int layoutResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(layoutResId);
        } else {
            builder.setView(View.inflate(builder.getContext(), layoutResId, null));
        }
        return this;
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param textId   The resource id of the text to display in the positive button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setPositiveButton(@StringRes int textId, final DialogInterface.OnClickListener listener) {
        builder.setPositiveButton(textId, listener);
        return this;
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     *
     * @param textId   The resource id of the text to display in the negative button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setNegativeButton(@StringRes int textId, final DialogInterface.OnClickListener listener) {
        builder.setNegativeButton(textId, listener);
        return this;
    }

    /**
     * Set a listener to be invoked when the neutral button of the dialog is pressed.
     *
     * @param textId   The resource id of the text to display in the neutral button
     * @param listener The {@link DialogInterface.OnClickListener} to use.
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setNeutralButton(@StringRes int textId, final DialogInterface.OnClickListener listener) {
        builder.setNeutralButton(textId, listener);
        return this;
    }

    /**
     * Set a callback to be called after the view is inflated
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder afterInflate(DialogInterface.OnShowListener listener) {
        afterInflate = listener;
        return this;
    }

    /**
     * Start the dialog and display it on screen. The window is placed in the
     * application layer and opaque.
     */
    public AlertDialog show() {
        AlertDialog dialog = builder.show();
        setButtonBarBackground(dialog);
        if (afterInflate != null)
            afterInflate.onShow(dialog);
        return dialog;
    }

    /**
     * Creates an {@link AlertDialog} with the arguments supplied to this
     * builder.
     * <p>
     * Calling this method does not display the dialog. The afterInflate
     * callback is not called if you call show on the AlertDialog.
     */
    public AlertDialog create() {
        return builder.create();
    }
}
