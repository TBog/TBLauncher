package rocks.tbog.tblauncher;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Set;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class EditTagsDialog extends DialogFragment<Set<String>> {

    private final ArraySet<String> mTagList = new ArraySet<>();
    private TagsAdapter mAdapter;
    private TextView mNewTag;

    @Override
    protected int layoutRes() {
        return R.layout.edit_tags_dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getContext();
        assert context != null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        String entryId = args.getString("entryId", "");
        String entryName = args.getString("entryName", "");

        // show the app we are changing
        EntryItem entry = TBApplication.getApplication(context).getDataHandler().getPojo(entryId);
        if (entry != null)
            entry.displayResult(view.findViewById(R.id.entry));

        // prepare the grid with all the tags
        mAdapter = new TagsAdapter(mTagList);
        GridView gridView = view.findViewById(R.id.grid);
        gridView.setAdapter(mAdapter);

        // initialize new tag EditView
        mNewTag = view.findViewById(R.id.newTag);
        mNewTag.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // initialize add tag button
        ImageView addTag = view.findViewById(R.id.addTag);
        addTag.setOnClickListener(v -> {
            String tag = mNewTag.getText().toString();
            mTagList.add(tag);
            mAdapter.notifyDataSetChanged();
        });

        // OK button
        {
            View button = view.findViewById(android.R.id.button1);
            button.setOnClickListener(v -> {
                onConfirm(mTagList);
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Context context = getActivity();
        assert context != null;

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        String entryId = args.getString("entryId", "");

        TagsHandler tagsHandler = TBApplication.tagsHandler(context);
        mTagList.clear();
        mTagList.addAll(tagsHandler.getTags(entryId));

        mAdapter.notifyDataSetChanged();
    }

    static class TagsAdapter extends BaseAdapter {
        private final ArraySet<String> mTags;
        private OnItemClickListener mOnItemClickListener = null;

        public interface OnItemClickListener {
            void onItemClick(TagsAdapter adapter, View view, int position);
        }

        TagsAdapter(@NonNull ArraySet<String> tags) {
            mTags = tags;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }

        @Override
        public int getCount() {
            return mTags.size();
        }

        @Override
        public String getItem(int position) {
            return mTags.valueAt(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_tag_item, parent, false);
            } else {
                view = convertView;
            }
            ViewHolder holder = view.getTag() instanceof ViewHolder ? (ViewHolder) view.getTag() : new ViewHolder(view);

            String content = getItem(position);
            holder.setContent(content);
            holder.buttonView.setOnClickListener(v -> {
                if (mOnItemClickListener != null)
                    mOnItemClickListener.onItemClick(TagsAdapter.this, v, position);
            });

            return view;
        }

        static class ViewHolder {
            TextView textView;
            View buttonView;

            ViewHolder(View itemView) {
                itemView.setTag(this);
                textView = itemView.findViewById(android.R.id.text1);
                buttonView = itemView.findViewById(android.R.id.button1);
            }

            public void setContent(CharSequence content) {
                textView.setText(content);
            }
        }
    }
}
