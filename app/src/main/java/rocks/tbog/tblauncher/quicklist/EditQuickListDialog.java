package rocks.tbog.tblauncher.quicklist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.DialogFragment;

public class EditQuickListDialog extends DialogFragment<Void> {

    private final EditQuickList mEditor = new EditQuickList();

    @Override
    protected int layoutRes() {
        return R.layout.quick_list_editor;
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
        mEditor.bindView(view);

        setOnPositiveClickListener((dialog, button) -> {
            mEditor.applyChanges(requireContext());
            onConfirm(null);
        });
    }
}
