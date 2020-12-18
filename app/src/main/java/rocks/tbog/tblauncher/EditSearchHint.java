package rocks.tbog.tblauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.ArraySet;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DialogBuilder;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class EditSearchHint {

    private final SearchHintAdapter mAdapter = new SearchHintAdapter(new ArrayList<>());
    private ListView mListView = null;
    private EditText mAddHint = null;

    public void applyChanges(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> availableHints = new ArraySet<>();
        Set<String> selectedHints = new ArraySet<>();

        if (mAddHint != null) {
            String name = mAddHint.getText().toString().trim();
            availableHints.add(name);
            selectedHints.add(name);
        }

        for (SearchHintInfo hintInfo : mAdapter.getItems()) {
            if (hintInfo.action == SearchHintInfo.Action.DELETE)
                continue;
            availableHints.add(hintInfo.text);
            if (hintInfo.selected)
                selectedHints.add(hintInfo.text);
        }

        prefs.edit()
                .putStringSet("available-search-hints", availableHints)
                .putStringSet("selected-search-hints", selectedHints)
                .apply();

        TBApplication.dataHandler(context).reloadProviders();
    }

    public void bindEditView(@NonNull View view) {
        mListView = view.findViewById(android.R.id.list);

        mListView.setOnItemClickListener((listView, itemView, position, id) -> {
            SearchHintInfo info = mAdapter.getItem(position);
            info.selected = !info.selected;
            mAdapter.notifyDataSetChanged();
        });

        mListView.setOnItemLongClickListener((listView, itemView, position, id) -> {
            SearchHintInfo info = mAdapter.getItem(position);
            if (info.action == SearchHintInfo.Action.DELETE) {
                info.action = info.text.equals(info.hint) ? SearchHintInfo.Action.NONE : SearchHintInfo.Action.RENAME;
                mAdapter.notifyDataSetChanged();
            } else {
                Context ctx = listView.getContext();
                ArrayAdapter<ListPopup.Item> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1);
                adapter.add(new ListPopup.Item(ctx, R.string.menu_action_rename));
                adapter.add(new ListPopup.Item(ctx, R.string.menu_action_delete));
                ListPopup.create(ctx, adapter)
                        .setOnItemClickListener((popupAdapter, popupItemView, popupPosition) -> {
                            Object object = popupAdapter.getItem(popupPosition);
                            if (!(object instanceof ListPopup.Item))
                                return;
                            ListPopup.Item item = (ListPopup.Item) object;
                            if (item.stringId == R.string.menu_action_rename) {
                                launchRenameDialog(ctx, info);
                            } else if (item.stringId == R.string.menu_action_delete) {
                                info.action = SearchHintInfo.Action.DELETE;
                                mAdapter.notifyDataSetChanged();
                            }
                        })
                        .setModal(true)
                        .setDimAmount(0.5f)
                        .show(itemView);
            }
            return true;
        });
    }

    public void bindAddView(@NonNull View view) {
        mAddHint = view.findViewById(android.R.id.text1);
    }

    private void launchRenameDialog(@NonNull Context ctx, @NonNull SearchHintInfo info) {
        DialogBuilder.withContext(ctx, R.style.NoTitleDialogTheme)
                .setTitle(R.string.title_rename_search_hint)
                .setView(R.layout.dialog_rename)
                .setPositiveButton(R.string.menu_action_rename, (dialog, which) -> {
                    EditText input = ((AlertDialog) dialog).findViewById(R.id.rename);
                    if (input == null)
                        return;
                    String newName = input.getText().toString().trim();
                    boolean isValid = true;
                    for (SearchHintInfo hintInfo : mAdapter.getItems()) {
                        if (info.equals(hintInfo))
                            continue;
                        if (hintInfo.hint.equals(newName) || hintInfo.text.equals(newName)) {
                            isValid = false;
                            break;
                        }
                    }
                    if (!isValid) {
                        Toast.makeText(ctx, ctx.getString(R.string.invalid_rename_search_engine, newName), Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Set new name
                    info.text = newName;
                    info.action = info.hint.equals(info.text) ? SearchHintInfo.Action.NONE : SearchHintInfo.Action.RENAME;

                    mAdapter.notifyDataSetChanged();

                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .afterInflate(dialog -> {
                    @SuppressLint("CutPasteId")
                    TextView nameView = ((AlertDialog) dialog).findViewById(R.id.rename);
                    if (nameView != null) {
                        nameView.setText(info.text);
                        nameView.requestFocus();
                    }
                })
                .show();
    }

    public void onStart() {
        // Set list adapter after the view inflated
        // This is a workaround to fix listview items not having the correct width
        if (mListView != null)
            mListView.post(() -> mListView.setAdapter(mAdapter));
    }

    public void loadData(@NonNull Context context, @NonNull SharedPreferences prefs) {
        Set<String> availableHints = prefs.getStringSet("available-search-hints", null);
        if (availableHints == null) {
            availableHints = new ArraySet<>();
            Collections.addAll(availableHints, context.getResources().getStringArray(R.array.defaultSearchHints));
        }
        Set<String> selectedHints = prefs.getStringSet("selected-search-hints", null);
        if (selectedHints == null) {
            selectedHints = new ArraySet<>(availableHints);
        }

        for (String hint : availableHints) {
            SearchHintInfo hintInfo = new SearchHintInfo(hint);
            hintInfo.selected = selectedHints.contains(hintInfo.hint);
            mAdapter.addItem(hintInfo);
        }
        Collections.sort(mAdapter.getItems(), (lhs, rhs) -> lhs.hint.compareTo(rhs.hint));
        mAdapter.notifyDataSetChanged();
    }

    public void loadDefaults(@NonNull Context context) {
        String[] defaultSearchHints = context.getResources().getStringArray(R.array.defaultSearchHints);
        for (String searchHint : defaultSearchHints) {
            SearchHintInfo searchEngineInfo = new SearchHintInfo(searchHint);
            searchEngineInfo.selected = true;
            mAdapter.addItem(searchEngineInfo);
        }
        Collections.sort(mAdapter.getItems(), (lhs, rhs) -> lhs.hint.compareTo(rhs.hint));
        mAdapter.notifyDataSetChanged();
    }


    static class SearchHintAdapter extends ViewHolderListAdapter<SearchHintInfo, SearchHintVH> {

        protected SearchHintAdapter(@NonNull List<SearchHintInfo> list) {
            super(SearchHintVH.class, android.R.layout.simple_list_item_checked, list);
        }

        @Override
        protected int getItemViewTypeLayout(int viewType) {
            if (viewType == 1)
                return android.R.layout.simple_list_item_1;
            return super.getItemViewTypeLayout(viewType);
        }

        public int getItemViewType(int position) {
            return getItem(position).action == SearchHintInfo.Action.DELETE ? 1 : 0;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public List<SearchHintInfo> getItems() {
            return mList;
        }
    }

    public static class SearchHintVH extends ViewHolderAdapter.ViewHolder<SearchHintInfo> {
        private final TextView text1View;

        public SearchHintVH(View view) {
            super(view);
            text1View = view.findViewById(android.R.id.text1);
        }

        @Override
        protected void setContent(SearchHintInfo content, int position, @NonNull ViewHolderAdapter<SearchHintInfo, ? extends ViewHolderAdapter.ViewHolder<SearchHintInfo>> adapter) {
            text1View.setText(content.text);
            text1View.setTypeface(null, content.action == SearchHintInfo.Action.RENAME ? Typeface.BOLD : Typeface.NORMAL);
            if (text1View instanceof CheckedTextView)
                ((CheckedTextView) text1View).setChecked(content.selected);
            if (content.action == SearchHintInfo.Action.DELETE) {
                text1View.setPaintFlags(text1View.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }

    private static class SearchHintInfo {
        @NonNull
        private final String hint;
        private String text;
        private boolean selected;
        private SearchHintInfo.Action action = SearchHintInfo.Action.NONE;

        enum Action {NONE, DELETE, RENAME}

        public SearchHintInfo(@NonNull String hintText) {
            hint = hintText;
            text = hintText;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchHintInfo that = (SearchHintInfo) o;
            return selected == that.selected &&
                    action == that.action &&
                    hint.equals(that.hint) &&
                    text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hint, text, selected, action);
        }
    }

}
