package rocks.tbog.tblauncher.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.EditQuickList;
import rocks.tbog.tblauncher.R;

public class EditQuickListDialog extends DialogFragment<Void> {

    private final EditQuickList mEditor = new EditQuickList();

    @Override
    protected int layoutRes() {
        return R.layout.pref_quick_list;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        Window window = requireDialog().getWindow();
        if (window != null)
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        // add button bar, just like the preferences dialog
        return inflater.inflate(R.layout.ok_cancel_button_bar, (ViewGroup) root, true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEditor.bindView(view);

        // OK button
        {
            View button = view.findViewById(android.R.id.button1);
            button.setOnClickListener(v -> {
                mEditor.applyChanges(requireContext());
                onConfirm(null);
                dismiss();
            });
        }

        // CANCEL button
        {
            View button = view.findViewById(android.R.id.button2);
            button.setOnClickListener(v -> dismiss());
        }
    }
}
