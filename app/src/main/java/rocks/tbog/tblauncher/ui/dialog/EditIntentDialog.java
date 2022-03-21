package rocks.tbog.tblauncher.ui.dialog;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.DialogWrapper;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class EditIntentDialog extends DialogFragment<ShortcutRecord> {
    private final ShortcutRecord mShortcut = new ShortcutRecord();
    private TextView mName;
    private TextView mPackageName;
    private TextView mClassName;

    @Override
    protected int layoutRes() {
        return R.layout.dialog_edit_intent;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = requireDialog().getContext();
        setupDefaultButtonOkCancel(context);

        // make sure we use the dialog context
        inflater = inflater.cloneInContext(context);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null)
            return null;

        mName = view.findViewById(R.id.name);
        mPackageName = view.findViewById(R.id.packageName);
        mClassName = view.findViewById(R.id.className);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        if (args.containsKey("dbId")) {
            mShortcut.dbId = args.getLong("dbId", mShortcut.dbId);
            ShortcutRecord rec = DBHelper.getShortcutRecord(requireContext(), mShortcut.dbId);
            if (rec != null) {
                mName.setText(rec.displayName);
                if (rec.packageName != null) {
                    ComponentName cn = UserHandleCompat.unflattenComponentName(rec.packageName);
                    mPackageName.setText(cn.getPackageName());
                    mClassName.setText(cn.getClassName());
//                } else {
//                    Intent intent = Intent.parseUri(rec.infoData, Intent.URI_INTENT_SCHEME);
                }
            }
        }

        // OK button
        {
            View button = view.findViewById(android.R.id.button1);
            button.setOnClickListener(v -> {
                updateShortcut();
                onConfirm(mShortcut);
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
        Dialog dialog = getDialog();
        if (dialog instanceof DialogWrapper) {
            ((DialogWrapper) dialog).setOnWindowFocusChanged((dlg, hasFocus) -> {
                if (hasFocus) {
                    dlg.setOnWindowFocusChanged(null);

                    //hack: fix the height of the dialog so it doesn't flicker
                    setFixedHeight(getView());
                }
            });
        }
    }

    private void setFixedHeight(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = view.getMeasuredHeight();
        view.setLayoutParams(params);
    }

    private void updateShortcut() {
        Context ctx = requireContext();
        Drawable icon = AppCompatResources.getDrawable(ctx, R.drawable.ic_shortcuts);
        if (icon == null)
            icon = TBApplication.getApplication(ctx).iconsHandler().getDefaultActivityIcon(ctx);

        String pkg = mPackageName.getText().toString();
        String cls = mClassName.getText().toString();
        ComponentName component = UserHandleCompat.relativeComponentName(pkg, cls);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setComponent(component);

        mShortcut.displayName = mName.getText().toString();
        mShortcut.packageName = new ComponentName(ctx, TBLauncherActivity.class).flattenToString();
        mShortcut.infoData = intent.toUri(Intent.URI_INTENT_SCHEME);
        mShortcut.iconPng = ShortcutUtil.getIconBlob(icon);
        mShortcut.setFlags(ShortcutRecord.FLAG_CUSTOM_INTENT);
    }
}
