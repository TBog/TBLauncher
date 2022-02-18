package rocks.tbog.tblauncher.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TagsManager;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.TagSearcher;
import rocks.tbog.tblauncher.ui.DialogFragment;

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

        setupDefaultButtonOkCancel(context);

        // make sure we use the dialog context
        inflater = inflater.cloneInContext(context);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mManager.bindView(view, (v, info) -> {
            if (info.staticEntry != null) {
                info.staticEntry.doLaunch(v, EntryItem.LAUNCHED_FROM_GESTURE);
            } else {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, info.tagName, TagSearcher.class);
            }
            // dismiss the dialog or else the result list will be covered
            dismiss();
        });

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
