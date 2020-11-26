package rocks.tbog.tblauncher.CustomIcon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Objects;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.ui.CodePointDrawable;
import rocks.tbog.tblauncher.ui.FourCodePointDrawable;
import rocks.tbog.tblauncher.ui.TwoCodePointDrawable;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class StaticEntryPage extends PageAdapter.Page {
    final ArrayList<DrawableData> iconDataList = new ArrayList<>();
    private StaticEntry mStaticEntry;
    private GridView mGridView;

    SystemPage.ShapedIconAdapter mDefaultItemAdapter = new SystemPage.ShapedIconAdapter(null) {
        @Override
        public SystemPage.ShapedIconInfo getItem(int position) {
            Context ctx = pageView.getContext();
            return new SystemPage.DefaultIconInfo(mStaticEntry.getDefaultDrawable(ctx));
        }
    };

    StaticEntryPage(CharSequence name, View view, StaticEntry staticEntry) {
        super(name, view);
        mStaticEntry = staticEntry;
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener) {
        int size = UISizes.getResultIconSize(context);

        // default icon
        {
            TextView textView = pageView.findViewById(android.R.id.text1);
            textView.setText(context.getString(R.string.default_static_icon, mStaticEntry.getName()));

            Drawable drawable = mStaticEntry.getDefaultDrawable(context);
            drawable.setBounds(0, 0, size, size);
            textView.setCompoundDrawables(drawable, null, null, null);

            if (iconClickListener != null)
                textView.setOnClickListener(v -> iconClickListener.onItemClick(mDefaultItemAdapter, textView, 0));
        }

        mGridView = pageView.findViewById(R.id.iconGrid);
        DrawableAdapter iconAdapter = new DrawableAdapter(iconDataList, iconClickListener);
        mGridView.setAdapter(iconAdapter);

        {
            EditText editText = pageView.findViewById(android.R.id.text2);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    generateIcons(s);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            editText.setText(mStaticEntry.getName());
        }
    }

    private void generateIcons(CharSequence text) {
        Context ctx = pageView.getContext();
        iconDataList.clear();

        int length = Utilities.codePointsLength(text);
        for (int shape : DrawableUtils.SHAPE_LIST) {
            if (length >= 1) {
                Drawable icon = new CodePointDrawable(text);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                iconDataList.add(new DrawableData(shapedIcon));
            }
            if (length >= 2) {
                Drawable icon = TwoCodePointDrawable.fromText(text, false);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                iconDataList.add(new DrawableData(shapedIcon));
            }
            if (length >= 2) {
                Drawable icon = TwoCodePointDrawable.fromText(text, true);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                iconDataList.add(new DrawableData(shapedIcon));
            }
            if (length >= 3) {
                Drawable icon = FourCodePointDrawable.fromText(text, true);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                iconDataList.add(new DrawableData(shapedIcon));
            }
            if (length >= 3) {
                Drawable icon = FourCodePointDrawable.fromText(text, false);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                iconDataList.add(new DrawableData(shapedIcon));
            }
        }
        ((DrawableAdapter) mGridView.getAdapter()).notifyDataSetChanged();
    }

    static class DrawableData {
        final Drawable icon;

        DrawableData(Drawable icon) {
            this.icon = icon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DrawableData that = (DrawableData) o;
            return Objects.equals(icon, that.icon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(icon);
        }
    }

    public static class DrawableDataVH extends ViewHolderAdapter.ViewHolder<DrawableData> {
        View root;
        ImageView icon;
        TextView text;

        public DrawableDataVH(View view) {
            super(view);
            root = view;
            icon = view.findViewById(android.R.id.icon);
            text = view.findViewById(android.R.id.text1);
        }

        @Override
        protected void setContent(DrawableData content, int position, @NonNull ViewHolderAdapter<DrawableData, ? extends ViewHolderAdapter.ViewHolder<DrawableData>> adapter) {
            icon.setImageDrawable(content.icon);
            text.setVisibility(View.GONE);
            final DrawableAdapter drawableAdapter = (DrawableAdapter) adapter;
            if (drawableAdapter.mIconClickListener != null)
                root.setOnClickListener(v -> drawableAdapter.mIconClickListener.onItemClick(drawableAdapter, v, position));
        }
    }

    static class DrawableAdapter extends ViewHolderListAdapter<DrawableData, DrawableDataVH> {
        private final OnItemClickListener mIconClickListener;

        protected DrawableAdapter(ArrayList<DrawableData> list, @Nullable OnItemClickListener iconClickListener) {
            super(DrawableDataVH.class, R.layout.item_grid, list);
            mIconClickListener = iconClickListener;
        }
    }
}
