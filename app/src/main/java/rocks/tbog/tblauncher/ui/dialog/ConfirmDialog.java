package rocks.tbog.tblauncher.ui.dialog;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.DialogFragment;

public class ConfirmDialog extends DialogFragment<Void> {
    @Override
    protected int layoutRes() {
        return R.layout.pref_confirm;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = requireDialog().getContext();

        setupDefaultButtonOkCancel(context);

        // make sure we use the dialog context
        LayoutInflater contextInflater = inflater.cloneInContext(context);
        return super.onCreateView(contextInflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        CharSequence descriptionText = args.getCharSequence("descriptionText", "");
        CharSequence titleText = args.getCharSequence("titleText", "");

        ((TextView) view.findViewById(android.R.id.text1)).setText(titleText);
        ((TextView) view.findViewById(android.R.id.text2)).setText(descriptionText);
    }
}
