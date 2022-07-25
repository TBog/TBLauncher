package rocks.tbog.tblauncher.customicon;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.ui.ListPopup;

public class ButtonHelper {

    public static final String BTN_ID_PHONE = "button://item_contact_action_phone";
    public static final String BTN_ID_MESSAGE = "button://item_contact_action_message";
    public static final String BTN_ID_OPEN = "button://item_contact_action_open";
    public static final String BTN_ID_LAUNCHER_PILL = "button://launcher_pill";
    public static final String BTN_ID_LAUNCHER_WHITE = "button://launcher_white";

    private ButtonHelper() {
        // don't instantiate a namespace
    }

    public static boolean showButtonPopup(@NonNull View view, @NonNull ListPopup buttonMenu) {
        final Context ctx = view.getContext();

        // check if menu contains elements and if yes show it
        if (!buttonMenu.getAdapter().isEmpty()) {
            TBApplication.getApplication(ctx).registerPopup(buttonMenu);
            buttonMenu.show(view);
            return true;
        }

        return false;
    }
}
