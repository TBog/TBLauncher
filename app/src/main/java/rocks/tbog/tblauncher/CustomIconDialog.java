package rocks.tbog.tblauncher;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class CustomIconDialog extends DialogFragment {
    private List<IconData> mIconData = new ArrayList<>();
    private Drawable mSelectedDrawable = null;
    private GridView mIconGrid;
    private TextView mSearch;
    private OnDismissListener mOnDismissListener = null;
    private OnConfirmListener mOnConfirmListener = null;

    public interface OnDismissListener {
        void onDismiss(@NonNull CustomIconDialog dialog);
    }

    public interface OnConfirmListener {
        void onConfirm(@NonNull Drawable icon);
    }

    void setOnDismissListener(OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    void setOnConfirmListener(OnConfirmListener listener) {
        mOnConfirmListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, 0);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (mOnDismissListener != null)
            mOnDismissListener.onDismiss(this);
        super.onDismiss(dialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.custom_icon_dialog, container, false);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
        lp.dimAmount = 0.7f;
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getDialog().setCanceledOnTouchOutside(true);

        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        mIconGrid = view.findViewById(R.id.iconGrid);
        IconAdapter iconAdapter = new IconAdapter(mIconData);
        mIconGrid.setAdapter(iconAdapter);

        iconAdapter.setOnItemClickListener((adapter, v, position) -> mSelectedDrawable = adapter.getItem(position).getIcon());

        mSearch = view.findViewById(R.id.searchIcon);
        mSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearch.post(() -> refreshList());
            }
        });

        // OK button
        {
            View button = view.findViewById(android.R.id.button1);
            button.setOnClickListener(v -> {
                if (mOnConfirmListener != null) {
                    if (mSelectedDrawable != null) {
                        mOnConfirmListener.onConfirm(mSelectedDrawable);
                    }
                }
                dismiss();
            });
        }

        // add default icon
        {
            Context context = getContext();
            assert context != null;
            String name = getArguments().getString("componentName");
            assert name != null;
            ImageView icon = view.findViewById(R.id.defaultIcon);
            IconsHandler iconsHandler = TBApplication.getApplication(context).getIconsHandler();
            IconPack iconPack = iconsHandler.getCurrentIconPack();
            ComponentName cn = UserHandleCompat.unflattenComponentName(name);
            Drawable drawable = iconPack != null ? iconPack.getComponentDrawable(cn.toString()) : null;
            if (drawable == null) {
                UserHandleCompat userHandle = UserHandleCompat.fromComponentName(context, name);
                drawable = iconsHandler.getSystemIconPack().getDefaultAppDrawable(context, cn, userHandle);
                if (iconPack != null)
                    drawable = iconPack.generateBitmap(context, drawable);
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    drawable = DrawableUtils.applyIconMaskShape(context, drawable);
                }
                if (!(drawable instanceof BitmapDrawable))
                    drawable = new BitmapDrawable(context.getResources(), DrawableUtils.drawableToBitmap(drawable));
            }

            icon.setImageDrawable(drawable);
            icon.setOnClickListener(v -> mSelectedDrawable = ((ImageView) v).getDrawable());

            ViewGroup parent = (ViewGroup) icon.getParent();

            // add getActivityIcon(componentName)
            drawable = null;
            try {
                drawable = context.getPackageManager().getActivityIcon(UserHandleCompat.unflattenComponentName(name));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                addQuickOption(drawable, parent);
                if (iconPack != null)
                    addQuickOption(iconPack.generateBitmap(context, drawable), parent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    addQuickOption(DrawableUtils.applyIconMaskShape(context, drawable), parent);
            }

            // add getApplicationIcon(packageName)
            drawable = null;
            try {
                drawable = context.getPackageManager().getApplicationIcon(UserHandleCompat.getPackageName(name));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                addQuickOption(drawable, parent);
                if (iconPack != null)
                    addQuickOption(iconPack.generateBitmap(context, drawable), parent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    addQuickOption(DrawableUtils.applyIconMaskShape(context, drawable), parent);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                UserHandleCompat userHandle = UserHandleCompat.fromComponentName(context, name);
                LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                List<LauncherActivityInfo> icons = launcher.getActivityList(cn.getPackageName(), userHandle.getRealHandle());
                for (LauncherActivityInfo info : icons) {
                    drawable = info.getBadgedIcon(0);

                    addQuickOption(drawable, parent);
                    if (iconPack != null)
                        addQuickOption(iconPack.generateBitmap(context, drawable), parent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        addQuickOption(DrawableUtils.applyIconMaskShape(context, drawable), parent);
                    break;
                }
            }
        }
    }

    private void addQuickOption(Drawable drawable, ViewGroup parent) {
        if (!(drawable instanceof BitmapDrawable))
            return;

        ImageView icon = (ImageView) LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_icon_item, parent, false);
        icon.setImageDrawable(drawable);
        icon.setOnClickListener(v -> mSelectedDrawable = ((ImageView) v).getDrawable());
        parent.addView(icon);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshList();
    }

    private void refreshList() {
        mIconData.clear();
        IconsHandler iconsHandler = TBApplication.getApplication(getActivity()).getIconsHandler();
        IconPack iconPack = iconsHandler.getCurrentIconPack();
        if (iconPack != null) {
            StringNormalizer.Result normalized = StringNormalizer.normalizeWithResult(mSearch.getText(), true);
            FuzzyScore fuzzyScore = new FuzzyScore(normalized.codePoints);
            Collection<IconPack.DrawableInfo> drawables = iconPack.getDrawableList();
            for (IconPack.DrawableInfo info : drawables) {
                if (fuzzyScore.match(info.drawableName).match)
                    mIconData.add(new IconData(iconPack, info));
            }
        }
        ((BaseAdapter)mIconGrid.getAdapter()).notifyDataSetChanged();
    }

    static class IconData {
        final IconPack.DrawableInfo drawableInfo;
        final IconPack iconPack;

        IconData(IconPack iconPack, IconPack.DrawableInfo drawableInfo) {
            this.iconPack = iconPack;
            this.drawableInfo = drawableInfo;
        }

        Drawable getIcon() {
            return iconPack.getDrawable(drawableInfo);
        }
    }

    static class IconAdapter extends BaseAdapter {
        private final List<IconData> mIcons;
        private OnItemClickListener mOnItemClickListener = null;

        public interface OnItemClickListener {
            void onItemClick(IconAdapter adapter, View view, int position);
        }

        IconAdapter(@NonNull List<IconData> objects) {
            mIcons = objects;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }

        @Override
        public IconData getItem(int position) {
            return mIcons.get(position);
        }

        @Override
        public int getCount() {
            return mIcons.size();
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_icon_item, parent, false);
            } else {
                view = convertView;
            }
            ViewHolder holder = view.getTag() instanceof ViewHolder ? (ViewHolder) view.getTag() : new ViewHolder(view);

            IconData contact = getItem(position);

            holder.icon.clearAnimation();
            holder.icon.setScaleX(1.f);
            holder.icon.setScaleY(1.f);
            holder.icon.setImageDrawable(contact.getIcon());
            holder.icon.setOnClickListener(v -> {
                v.animate().scaleX(1.5f).scaleY(1.5f).start();
                if (mOnItemClickListener != null)
                    mOnItemClickListener.onItemClick(IconAdapter.this, v, position);
            });
            holder.icon.setOnLongClickListener(v -> {
                displayToast(v, contact.drawableInfo.drawableName);
                return true;
            });

            return view;
        }

        /**
         * @param v       is the Button view that you want the Toast to appear above
         * @param message is the string for the message
         */

        private void displayToast(View v, CharSequence message) {
            int xOffset = 0;
            int yOffset = 0;
            Rect gvr = new Rect();

            View parent = (View) v.getParent();
            int parentHeight = parent.getHeight();

            if (v.getGlobalVisibleRect(gvr)) {
                View root = v.getRootView();

                int halfWidth = root.getRight() / 2;
                int halfHeight = root.getBottom() / 2;

                int parentCenterX = (gvr.width() / 2) + gvr.left;

                int parentCenterY = (gvr.height() / 2) + gvr.top;

                if (parentCenterY <= halfHeight) {
                    yOffset = -(halfHeight - parentCenterY);
                } else {
                    yOffset = (parentCenterY - halfHeight);
                }

                if (parentCenterX < halfWidth) {
                    xOffset = -(halfWidth - parentCenterX);
                }

                if (parentCenterX >= halfWidth) {
                    xOffset = parentCenterX - halfWidth;
                }
            }

            Toast toast = Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, xOffset, yOffset + v.getHeight());
            toast.show();
        }

        static class ViewHolder {
            ImageView icon;

            ViewHolder(View itemView) {
                itemView.setTag(this);
                icon = itemView.findViewById(android.R.id.icon);
            }
        }
    }
}
