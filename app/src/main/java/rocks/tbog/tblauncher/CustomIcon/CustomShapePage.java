package rocks.tbog.tblauncher.CustomIcon;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
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

import net.mm2d.color.chooser.DialogView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.ui.CodePointDrawable;
import rocks.tbog.tblauncher.ui.FourCodePointDrawable;
import rocks.tbog.tblauncher.ui.TextDrawable;
import rocks.tbog.tblauncher.ui.TwoCodePointDrawable;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

class CustomShapePage extends PageAdapter.Page {
    protected ShapedIconAdapter mShapesAdapter;
    protected ShapedIconAdapter mShapedIconAdapter;
    protected TextView mLettersView;
    protected int mShape;
    protected float mScale = 1.f;
    protected int mBackground;
    private int mLetters;

    CustomShapePage(CharSequence name, View view) {
        super(name, view);
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

        // shapes list
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

        // icons we are customizing
        {
            GridView gridView = pageView.findViewById(R.id.iconGrid);
            mShapedIconAdapter = new ShapedIconAdapter(iconClickListener);
            gridView.setAdapter(mShapedIconAdapter);
            TBApplication.ui(context).setResultListPref(gridView);
        }

        // scale bar
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
    }

    public void addIcon(@NonNull String name, @NonNull Drawable drawable) {
        Context context = pageView.getContext();
        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
        NamedIconInfo iconInfo = new NamedIconInfo(name, shapedDrawable, drawable);
        mShapedIconAdapter.addItem(iconInfo);
    }

    private void addTextIcon(@NonNull TextDrawable icon) {
        final Context ctx = pageView.getContext();
        final ShapedIconAdapter adapter = mShapedIconAdapter;

        icon.setTextColor(mLetters);
        Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, mShape, mScale, mBackground);
        adapter.addItem(new LetterIconInfo(shapedIcon));
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
                shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, shape);
            }
            NamedIconInfo iconInfo = new NamedIconInfo(name, shapedDrawable, null);
            adapter.addItem(iconInfo);
        }
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

        @Nullable
        @Override
        CharSequence getText() {
            return name;
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

        @Nullable
        CharSequence getText() {
            return null;
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

    public static class ShapedIconVH extends ViewHolderAdapter.ViewHolder<ShapedIconInfo> {
        View root;
        ImageView icon;
        TextView text1;

        public ShapedIconVH(View view) {
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
            CharSequence text = content.getText();
            if (text != null)
                text1.setText(text);
            else
                text1.setText(content.textId);

            // setOnClickListener when we have an icon
            if (shapedIconAdapter.mIconClickListener != null && content.getPreview() != null)
                root.setOnClickListener(v -> shapedIconAdapter.mIconClickListener.onItemClick(adapter, v, position));
        }
    }

    static class ShapedIconAdapter extends ViewHolderListAdapter<ShapedIconInfo, ShapedIconVH> {
        @Nullable
        private final OnItemClickListener mIconClickListener;

        protected ShapedIconAdapter(@Nullable OnItemClickListener iconClickListener) {
            super(ShapedIconVH.class, R.layout.item_grid, new ArrayList<>());
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
