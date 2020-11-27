package rocks.tbog.tblauncher.CustomIcon;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;

import net.mm2d.color.chooser.DialogView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.ui.CodePointDrawable;
import rocks.tbog.tblauncher.ui.FourCodePointDrawable;
import rocks.tbog.tblauncher.ui.TextDrawable;
import rocks.tbog.tblauncher.ui.TwoCodePointDrawable;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class SystemPage extends PageAdapter.Page {
    private final ComponentName componentName;
    private final UserHandleCompat userHandle;
    private ShapedIconAdapter mShapesAdapter;
    private ShapedIconAdapter mShapedIconAdapter;
    private TextView mLettersView;
    private int mShape;
    private float mScale = 1.f;
    private int mBackground;
    private int mLetters;

    SystemPage(CharSequence name, View view, ComponentName cn, UserHandleCompat uh) {
        super(name, view);
        componentName = cn;
        userHandle = uh;
        final Context ctx = view.getContext();
        mShape = TBApplication.iconsHandler(ctx).getSystemIconPack().getAdaptiveShape();
        mLetters = UIColors.getContactActionColor(ctx);
        mBackground = (UIColors.luminance(UIColors.getResultListBackground(ctx)) > .666) ? Color.BLACK : Color.WHITE;
        if (mShape == DrawableUtils.SHAPE_NONE)
            mShape = DrawableUtils.SHAPE_SQUARE;
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        mLettersView = pageView.findViewById(R.id.letters);
        mLettersView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                generateTextIcons(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        {
            GridView shapeGridView = pageView.findViewById(R.id.shapeGrid);
            mShapesAdapter = new ShapedIconAdapter((adapter, view, position) -> {
                Activity activity = Utilities.getActivity(view);
                if (activity == null)
                    return;

                Object objItem = adapter.getItem(position);
                if (objItem instanceof NamedIconInfo) {
                    CharSequence name = ((NamedIconInfo) objItem).name;
                    for (int shape : DrawableUtils.SHAPE_LIST) {
                        if (name.equals(DrawableUtils.shapeName(activity, shape))) {
                            mShape = shape;
                            break;
                        }
                    }
                }
                reshapeIcons(activity);
            });
            shapeGridView.setAdapter(mShapesAdapter);
            TBApplication.ui(context).setResultListPref(shapeGridView);
        }

        {
            GridView gridView = pageView.findViewById(R.id.iconGrid);
            mShapedIconAdapter = new ShapedIconAdapter(iconClickListener);
            gridView.setAdapter(mShapedIconAdapter);
            TBApplication.ui(context).setResultListPref(gridView);
        }

        {
            SeekBar seekBar = pageView.findViewById(R.id.scaleBar);
            seekBar.setMax(200);
            seekBar.setProgress((int) (100.f * mScale));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mScale = 0.01f * seekBar.getProgress();
                    reshapeIcons(seekBar.getContext());
                }
            });
        }

        int colorPreviewSize = context.getResources().getDimensionPixelSize(R.dimen.color_preview_size);

        // shape background color chooser
        {
            TextView colorView = pageView.findViewById(R.id.backgroundColor);
            ColorDrawable colorDrawable = new ColorDrawable(mBackground);
            colorDrawable.setBounds(0, 0, colorPreviewSize, colorPreviewSize);
            colorView.setCompoundDrawables(null, null, colorDrawable, null);
            colorView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                launchCustomColorDialog(ctx, mBackground, color -> {
                    mBackground = color;
                    Activity activity = Utilities.getActivity(v);
                    if (activity == null)
                        return;
                    colorDrawable.setColor(mBackground);
                    colorView.setCompoundDrawables(null, null, colorDrawable, null);
                    generateShapes(activity);
                    reshapeIcons(activity);
                });
            });
        }

        // letter color chooser
        {
            TextView colorView = pageView.findViewById(R.id.lettersColor);
            ColorDrawable colorDrawable = new ColorDrawable(mLetters);
            colorDrawable.setBounds(0, 0, colorPreviewSize, colorPreviewSize);
            colorView.setCompoundDrawables(null, null, colorDrawable, null);
            colorView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                launchCustomColorDialog(ctx, mLetters, color -> {
                    mLetters = color;
                    Activity activity = Utilities.getActivity(v);
                    if (activity == null)
                        return;
                    colorDrawable.setColor(mLetters);
                    colorView.setCompoundDrawables(null, null, colorDrawable, null);
                    generateTextIcons(mLettersView.getText());
                });
            });
        }

        generateShapes(context);
        addSystemIcons(context, mShapedIconAdapter);

        // this will call generateTextIcons
        //mLettersView.setText(pageName);
    }

    interface OnColorChanged {
        void onColorChanged(int color);
    }

    private static void launchCustomColorDialog(@Nullable Context context, int selectedColor, @NonNull OnColorChanged listener) {
        Activity activity = Utilities.getActivity(context);
        if (!(activity instanceof AppCompatActivity))
            return;

        ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.SettingsDialogTheme);
        DialogView dialogView = new DialogView(themeWrapper);

        dialogView.init(selectedColor, (AppCompatActivity) activity);
        dialogView.setWithAlpha(true);

        DialogInterface.OnClickListener buttonListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                listener.onColorChanged(dialogView.getColor());
            }
            dialog.dismiss();
        };

        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.ok, buttonListener)
                .setNegativeButton(android.R.string.cancel, buttonListener);
        builder.setView(dialogView);
        builder.show();
    }

    private void reshapeIcons(Context context) {
        generateTextIcons(null);
        for (ListIterator<ShapedIconInfo> iterator = mShapedIconAdapter.getList().listIterator(); iterator.hasNext(); ) {
            ShapedIconInfo iconInfo = iterator.next();
            if (iconInfo.textId == R.string.icon_pack_loading)
                continue;
            if (iconInfo.textId == R.string.default_icon)
                continue;
            ShapedIconInfo newInfo;
            Drawable drawable = DrawableUtils.applyIconMaskShape(context, iconInfo.originalDrawable, mShape, mScale, mBackground);
            if (iconInfo instanceof NamedIconInfo) {
                newInfo = new NamedIconInfo(((NamedIconInfo) iconInfo).name, drawable, iconInfo.originalDrawable);
            } else {
                newInfo = new ShapedIconInfo(drawable, iconInfo.originalDrawable);
                newInfo.textId = iconInfo.textId;
            }
            iterator.set(newInfo);
        }
        mShapedIconAdapter.notifyDataSetChanged();
        generateTextIcons(mLettersView.getText());
    }

    private void generateShapes(Context context) {
        final ShapedIconAdapter adapter = mShapesAdapter;
        adapter.getList().clear();
        adapter.notifyDataSetChanged();
        Drawable drawable = new ColorDrawable(mBackground);
        for (int shape : DrawableUtils.SHAPE_LIST) {
            String name = DrawableUtils.shapeName(context, shape);
            Drawable shapedDrawable;
            if (shape == DrawableUtils.SHAPE_NONE) {
                shapedDrawable = new ColorDrawable(Color.TRANSPARENT);
            } else {
                shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, shape, mScale, Color.TRANSPARENT);
            }
            NamedIconInfo iconInfo = new NamedIconInfo(name, shapedDrawable, null);
            adapter.addItem(iconInfo);
        }
    }

    public void addIcon(@NonNull String name, @NonNull Drawable drawable) {
        Context context = pageView.getContext();
        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
        NamedIconInfo iconInfo = new NamedIconInfo(name, shapedDrawable, drawable);
        mShapedIconAdapter.addItem(iconInfo);
    }

    private void addSystemIcons(Context context, ShapedIconAdapter adapter) {
        ArraySet<Bitmap> dSet = new ArraySet<>(3);

        IconsHandler iconsHandler = TBApplication.getApplication(context).iconsHandler();
        // add default icon
        {
            Drawable drawable = iconsHandler.getDrawableIconForPackage(componentName, userHandle);

            //checkDuplicateDrawable(dSet, drawable);

            ShapedIconInfo iconInfo = new DefaultIconInfo(drawable);
            iconInfo.textId = R.string.default_icon;
            adapter.addItem(iconInfo);
        }

        // add getActivityIcon(componentName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getActivityIcon(componentName);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                if (checkDuplicateDrawable(dSet, drawable)) {
                    Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
                    addQuickOption(R.string.custom_icon_activity, shapedDrawable, drawable, adapter);
                }
            }
        }

        // add getApplicationIcon(packageName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getApplicationIcon(componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                if (checkDuplicateDrawable(dSet, drawable)) {
                    Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
                    addQuickOption(R.string.custom_icon_application, shapedDrawable, drawable, adapter);
                }
            }
        }

        // add Activity BadgedIcon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
            for (LauncherActivityInfo info : icons) {
                Drawable drawable = info.getBadgedIcon(0);

                if (drawable != null) {
                    if (checkDuplicateDrawable(dSet, drawable)) {
                        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
                        addQuickOption(R.string.custom_icon_badged, shapedDrawable, drawable, adapter);
                    }
                }
            }
        }
    }

    private void generateTextIcons(@Nullable CharSequence text) {
        final ShapedIconAdapter adapter = mShapedIconAdapter;

        // remove all TextDrawable icons
        for (Iterator<ShapedIconInfo> iterator = adapter.getList().iterator(); iterator.hasNext(); ) {
            ShapedIconInfo info = iterator.next();
            if (info instanceof LetterIconInfo)
                iterator.remove();
        }
        adapter.notifyDataSetChanged();

        final int length = Utilities.codePointsLength(text);
        if (length >= 1) {
            TextDrawable icon = new CodePointDrawable(text);
            addTextIcon(icon);
        }
        if (length >= 2) {
            TextDrawable icon = TwoCodePointDrawable.fromText(text, false);
            addTextIcon(icon);
        }
        if (length >= 2) {
            TextDrawable icon = TwoCodePointDrawable.fromText(text, true);
            addTextIcon(icon);
        }
        if (length >= 3) {
            TextDrawable icon = FourCodePointDrawable.fromText(text, true);
            addTextIcon(icon);
        }
        if (length >= 3) {
            TextDrawable icon = FourCodePointDrawable.fromText(text, false);
            addTextIcon(icon);
        }
    }

    private void addTextIcon(@NonNull TextDrawable icon) {
        final Context ctx = pageView.getContext();
        final ShapedIconAdapter adapter = mShapedIconAdapter;

        icon.setTextColor(mLetters);
        Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, mShape, mScale, mBackground);
        adapter.addItem(new LetterIconInfo(shapedIcon));
    }

    private boolean checkDuplicateDrawable(ArraySet<Bitmap> set, Drawable drawable) {
        Bitmap b = null;
        if (drawable instanceof BitmapDrawable)
            b = ((BitmapDrawable) drawable).getBitmap();

        if (set.contains(b))
            return false;

        set.add(b);
        return true;
    }

    private static void addQuickOption(@StringRes int textId, Drawable shapedDrawable, Drawable drawable, ShapedIconAdapter adapter) {
        if (!(drawable instanceof BitmapDrawable))
            return;

        ShapedIconInfo iconInfo = new ShapedIconInfo(shapedDrawable, drawable);
        iconInfo.textId = textId;
        adapter.addItem(iconInfo);
    }

    public void loadIconPackIcons(List<Pair<String, String>> iconPacks) {
        if (iconPacks.isEmpty())
            return;
        final Context ctx = pageView.getContext();
        final ShapedIconInfo placeholderItem = new ShapedIconInfo(DrawableUtils.getProgressBarIndeterminate(ctx), null);
        placeholderItem.textId = R.string.icon_pack_loading;
        {
            mShapedIconAdapter.addItem(placeholderItem);
        }
        final ArrayList<NamedIconInfo> options = new ArrayList<>();
        Utilities.runAsync((t) -> {
            for (Pair<String, String> packInfo : iconPacks) {
                String packPackageName = packInfo.first;
                String packName = packInfo.second;
                Activity activity = Utilities.getActivity(pageView);
                if (activity != null) {
                    IconPackXML pack = TBApplication.iconPackCache(activity).getIconPack(packPackageName);
                    pack.load(activity.getPackageManager());
                    Drawable drawable = pack.getComponentDrawable(activity, componentName, userHandle);
                    Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(activity, drawable, mShape, mScale, mBackground);
                    NamedIconInfo iconInfo = new NamedIconInfo(packName, shapedDrawable, drawable);
                    options.add(iconInfo);
                } else {
                    break;
                }
            }
        }, (t) -> {
            Activity activity = Utilities.getActivity(pageView);
            if (activity != null) {
                final ShapedIconAdapter adapter = mShapedIconAdapter;
                adapter.removeItem(placeholderItem);
                adapter.addAll(options);
            }
        });
    }

    static class LetterIconInfo extends NamedIconInfo {

        LetterIconInfo(Drawable icon) {
            super("", icon, icon);
        }
    }

    static class DefaultIconInfo extends ShapedIconInfo {

        DefaultIconInfo(Drawable icon) {
            super(icon, icon);
        }

        @Override
        Drawable getIcon() {
            return null;
        }
    }

    static class NamedIconInfo extends ShapedIconInfo {
        final CharSequence name;

        NamedIconInfo(CharSequence name, Drawable icon, Drawable origin) {
            super(icon, origin);
            this.name = name;
        }
    }

    static class ShapedIconInfo {
        final Drawable originalDrawable;
        final Drawable iconDrawable;
        @StringRes
        int textId;

        ShapedIconInfo(Drawable icon, Drawable origin) {
            iconDrawable = icon;
            originalDrawable = origin;
        }

        Drawable getIcon() {
            return iconDrawable;
        }

        public Drawable getPreview() {
            return iconDrawable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShapedIconInfo that = (ShapedIconInfo) o;
            return Objects.equals(iconDrawable, that.iconDrawable) &&
                    Objects.equals(textId, that.textId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(iconDrawable, textId);
        }
    }

    public static class SystemPageViewHolder extends ViewHolderAdapter.ViewHolder<ShapedIconInfo> {
        View root;
        ImageView icon;
        TextView text1;

        public SystemPageViewHolder(View view) {
            super(view);
            root = view;
            icon = view.findViewById(android.R.id.icon);
            text1 = view.findViewById(android.R.id.text1);
        }

        @Override
        protected void setContent(ShapedIconInfo content, int position, @NonNull ViewHolderAdapter<ShapedIconInfo, ? extends ViewHolderAdapter.ViewHolder<ShapedIconInfo>> adapter) {
            ShapedIconAdapter shapedIconAdapter = (ShapedIconAdapter) adapter;
            // set icon
            Drawable preview = content.getPreview();
            icon.setImageDrawable(preview);
            icon.setVisibility(preview == null ? View.GONE : View.VISIBLE);
            if (preview instanceof Animatable)
                ((Animatable) preview).start();

            //set text
            if (content instanceof NamedIconInfo)
                text1.setText(((NamedIconInfo) content).name);
            else
                text1.setText(content.textId);

            // setOnClickListener when we have an icon
            if (shapedIconAdapter.mIconClickListener != null && content.getPreview() != null)
                root.setOnClickListener(v -> shapedIconAdapter.mIconClickListener.onItemClick(adapter, v, position));
        }
    }

    static class ShapedIconAdapter extends ViewHolderListAdapter<ShapedIconInfo, SystemPageViewHolder> {
        @Nullable
        private final OnItemClickListener mIconClickListener;

        protected ShapedIconAdapter(@Nullable OnItemClickListener iconClickListener) {
            super(SystemPageViewHolder.class, R.layout.item_grid, new ArrayList<>());
            mIconClickListener = iconClickListener;
        }

        List<ShapedIconInfo> getList() {
            return mList;
        }

        void addItem(ShapedIconInfo item) {
            mList.add(item);
            notifyDataSetChanged();
        }

        void addAll(Collection<? extends ShapedIconInfo> collection) {
            mList.addAll(collection);
            notifyDataSetChanged();
        }

        void removeItem(ShapedIconInfo item) {
            mList.remove(item);
            notifyDataSetChanged();
        }

        @Override
        public ShapedIconInfo getItem(int position) {
            return mList.get(position);
        }
    }
}
