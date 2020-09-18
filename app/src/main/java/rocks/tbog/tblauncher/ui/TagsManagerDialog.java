package rocks.tbog.tblauncher.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TagsManager;

public class TagsManagerDialog extends DialogFragment<Void> {

    private final TagsManager mManager = new TagsManager();

    @Override
    protected int layoutRes() {
        return R.layout.tags_manager;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        assert root != null;
        Window window = requireDialog().getWindow();
        if (window != null)
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        root.findViewById(R.id.ok_cancel_button_bar).setVisibility(View.VISIBLE);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mManager.bindView(view);

        // OK button
        {
            View button = view.findViewById(android.R.id.button1);
            button.setOnClickListener(v -> {
                mManager.applyChanges(requireContext());
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

    @Override
    public void onStart() {
        super.onStart();
        mManager.onStart();
    }
}
