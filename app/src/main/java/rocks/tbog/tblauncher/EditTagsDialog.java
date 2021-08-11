package rocks.tbog.tblauncher;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.DialogWrapper;

public class EditTagsDialog extends DialogFragment<Set<String>> {

    private static final String TAG = EditTagsDialog.class.getSimpleName();
    private final ArraySet<String> mTagList = new ArraySet<>();
    private TagsAdapter mAdapter;
    private AutoCompleteTextView mNewTag;

    @Override
    protected int layoutRes() {
        return R.layout.dialog_edit_tags;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = requireDialog().getContext();

        setupDefaultButtons(context);
        Bundle args = getArguments() != null ? getArguments() : new Bundle();

        // make sure we use the dialog context
        LayoutInflater dialogInflater = inflater.cloneInContext(context);
        ViewGroup root = (ViewGroup) super.onCreateView(dialogInflater, container, savedInstanceState);
        assert root != null;

        // make a layout for the entry we are changing
        String entryId = args.getString("entryId", "");
        TBApplication app = TBApplication.getApplication(context);
        EntryItem entry = app.getDataHandler().getPojo(entryId);
        ViewGroup wrapper = root.findViewById(R.id.previewWrapper);
        if (wrapper == null)
            wrapper = root;
        if (entry != null) {
            int drawFlags = EntryItem.FLAG_DRAW_LIST | EntryItem.FLAG_DRAW_NAME | EntryItem.FLAG_DRAW_ICON;
            View entryView = dialogInflater.inflate(entry.getResultLayout(drawFlags), wrapper, false);
            entryView.setId(R.id.preview);
            wrapper.addView(entryView, 0);
            app.ui().setResultListPref(entryView);
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = view.getContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        String entryId = args.getString("entryId", "");
        String entryName = args.getString("entryName", "");

        // show the app we are changing
        EntryItem entry = TBApplication.getApplication(context).getDataHandler().getPojo(entryId);
        if (entry == null) {
            dismiss();
            return;
        }
        int drawFlags = EntryItem.FLAG_DRAW_LIST | EntryItem.FLAG_DRAW_NAME | EntryItem.FLAG_DRAW_ICON;
        entry.displayResult(view.findViewById(R.id.preview), drawFlags);

        // prepare the grid with all the tags
        mAdapter = new TagsAdapter(mTagList);
        GridView gridView = view.findViewById(R.id.grid);
        gridView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener((adapter, v, position) -> removeTag(adapter.getItem(position)));

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
        mNewTag.setOnEditorActionListener((v, actionId, event) -> {
            if (event == null) {
                if (actionId != EditorInfo.IME_ACTION_NONE) {
                    String tag = mNewTag.getText().toString();
                    if (tag.isEmpty()) {
                        onConfirm(mTagList);
                        dismiss();
                        return true;
                    }
                    addTag(tag);
                    return true;
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    String tag = mNewTag.getText().toString();
                    addTag(tag);
                }
                return true;
            }
            return false;
        });
        // set the auto complete list
        {
            List<String> allTags = new ArrayList<>(TBApplication.tagsHandler(context).getValidTags());
            Collections.sort(allTags);
            mNewTag.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, allTags));
        }

        // initialize add tag button
        ImageView addTag = view.findViewById(R.id.addTag);
        addTag.setOnClickListener(v ->
        {
            String tag = mNewTag.getText().toString();
            addTag(tag);
        });
    }

    @Override
    public void onButtonClick(@NonNull Button button) {
        if (button == Button.POSITIVE) {
            String tag = mNewTag.getText().toString();
            addTag(tag);
            onConfirm(mTagList);
        }
        super.onButtonClick(button);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof DialogWrapper) {
            ((DialogWrapper) dialog).setOnWindowFocusChanged((dlg, hasFocus) -> {
                if (hasFocus) {
                    dlg.setOnWindowFocusChanged(null);
                    showKeyboard(dlg, mNewTag);
                }
            });
        }
    }

    private static void showKeyboard(@NonNull Dialog dialog, @NonNull TextView textView) {
        Log.i(TAG, "Keyboard - SHOW");
        textView.requestFocus();

        InputMethodManager mgr = (InputMethodManager) dialog.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;
        mgr.showSoftInput(textView, InputMethodManager.SHOW_IMPLICIT);
    }

    private void addTag(String tag) {
        tag = tag.trim();
        if (tag.length() == 0)
            return;
        mTagList.add(tag);
        mAdapter.notifyDataSetChanged();
        mNewTag.setText("");
    }

    private void removeTag(String tag) {
        mTagList.remove(tag);
        mAdapter.notifyDataSetChanged();
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
