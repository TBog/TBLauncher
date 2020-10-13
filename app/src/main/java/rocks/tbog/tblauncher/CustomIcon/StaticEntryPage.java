package rocks.tbog.tblauncher.CustomIcon;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.ui.CodePointDrawable;
import rocks.tbog.tblauncher.ui.FourCodePointDrawable;
import rocks.tbog.tblauncher.ui.TwoCodePointDrawable;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;

public class StaticEntryPage extends PageAdapter.Page {
    final ArrayList<DrawableData> iconDataList = new ArrayList<>();
    private StaticEntry mStaticEntry;
    private GridView mGridView;

    SystemPage.SystemPageAdapter mDefaultItemAdapter = new SystemPage.SystemPageAdapter(SystemPage.SystemPageViewHolder.class, null) {
        @Override
        public SystemPage.SystemIconInfo getItem(int position) {
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
        iconDataList.clear();
        int length = Utilities.codePointsLength(text);
        if (length >= 1) {
            DrawableData dd = new DrawableData();
            dd.icon = new CodePointDrawable(text);
            iconDataList.add(dd);
        }
        if (length >= 2) {
            DrawableData dd = new DrawableData();
            dd.icon = TwoCodePointDrawable.fromText(text, false);
            iconDataList.add(dd);
        }
        if (length >= 2) {
            DrawableData dd = new DrawableData();
            dd.icon = TwoCodePointDrawable.fromText(text, true);
            iconDataList.add(dd);
        }
        if (length >= 3) {
            DrawableData dd = new DrawableData();
            dd.icon = FourCodePointDrawable.fromText(text, true);
            iconDataList.add(dd);
        }
        if (length >= 3) {
            DrawableData dd = new DrawableData();
            dd.icon = FourCodePointDrawable.fromText(text, false);
            iconDataList.add(dd);
        }
        ((DrawableAdapter) mGridView.getAdapter()).notifyDataSetChanged();
    }

    static class DrawableData {
        Drawable icon;
    }

    static class DrawableAdapter extends BaseAdapter {
        private final ArrayList<DrawableData> mList;
        private OnItemClickListener mIconClickListener;

        DrawableAdapter(ArrayList<DrawableData> list, @Nullable OnItemClickListener iconClickListener) {
            mList = list;
            mIconClickListener = iconClickListener;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public DrawableData getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            @SuppressLint("ViewHolder")
            View view = inflater.inflate(R.layout.item_grid, parent, false);

            ImageView icon = view.findViewById(android.R.id.icon);
            icon.setImageDrawable(getItem(position).icon);

            TextView text = view.findViewById(android.R.id.text1);
            text.setVisibility(View.GONE);

            if (mIconClickListener != null)
                view.setOnClickListener(v -> mIconClickListener.onItemClick(this, v, position));

            return view;
        }
    }
}
