package rocks.tbog.tblauncher.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;

public class DialogBuilder {
    final private AlertDialog.Builder builder;
    private DialogInterface.OnShowListener afterInflate = null;

    private DialogBuilder(@NonNull Context context) {
        builder = new AlertDialog.Builder(context);
    }

    public static DialogBuilder withContext(@NonNull Context context) {
        return new DialogBuilder(context);
    }

    public static DialogBuilder withContext(@NonNull Context context, @StyleRes int theme) {
        ContextThemeWrapper ctx = new ContextThemeWrapper(context, theme);
        return withContext(ctx);
    }

    /**
     * Set the title displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setTitle(@StringRes int titleId) {
        builder.setTitle(titleId);
        return this;
    }

    /**
     * Set the title displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setTitle(CharSequence title) {
        builder.setTitle(title);
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
        if (afterInflate != null)
            afterInflate.onShow(dialog);
        return dialog;
    }
}
