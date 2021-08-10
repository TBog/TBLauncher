package rocks.tbog.tblauncher.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        Context context = requireDialog().getContext();

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        if (!isStateSaved()) {
            args.putCharSequence("btnPositiveText", context.getText(android.R.string.ok));
            args.putCharSequence("btnNegativeText", context.getText(android.R.string.cancel));
            setArguments(args);
        }

        // make sure we use the dialog context
        inflater = inflater.cloneInContext(context);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mManager.bindView(view);

        setOnPositiveClickListener((dialog, button) -> {
            mManager.applyChanges(requireContext());
            onConfirm(null);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mManager.onStart();
    }
}
