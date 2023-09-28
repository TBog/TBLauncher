package rocks.tbog.tblauncher.preference;

import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DialogHelper;
import rocks.tbog.tblauncher.utils.SimpleTextWatcher;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class EditSearchHintPreferenceDialog extends EditAddResetPreferenceDialog {

    @Nullable
    public static EditSearchHintPreferenceDialog newInstance(String key) {
        return EditAddResetPreferenceDialog.newInstance(key, EditSearchHintPreferenceDialog.class);
    }

    @Nullable
    @Override
    protected EditAddResetEditor newEditor() {
        return new ViewModelProvider(this).get(EditSearchHint.class);
    }

    @NonNull
    protected String[] getEditAddResetKeys() {
        return new String[]{"edit-search-hint", "add-search-hint", "reset-search-hint"};
    }

    public static class EditSearchHint extends EditAddResetEditor {

        private final MutableLiveData<ArrayList<SearchHintInfo>> searchHintList = new MutableLiveData<>();
        private final MutableLiveData<String> addHintName = new MutableLiveData<>();

        public EditSearchHint(@NonNull Application application) {
            super(application);
        }

        public void updateSearchHintList(SearchHintInfo info) {
            ArrayList<SearchHintInfo> arrayList = searchHintList.getValue();
            if (arrayList == null || arrayList.contains(info))
                searchHintList.setValue(arrayList);
        }

        public void applyChanges(@NonNull Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            Set<String> availableHints = new ArraySet<>();
            Set<String> selectedHints = new ArraySet<>();

            String mAddHint = addHintName.getValue();
            if (mAddHint != null) {
                String name = mAddHint.trim();
                availableHints.add(name);
                selectedHints.add(name);
            }

            ArrayList<SearchHintInfo> searchHintInfoList = searchHintList.getValue();
            if (searchHintInfoList != null) {
                for (SearchHintInfo hintInfo : searchHintInfoList) {
                    if (hintInfo.action == SearchHintInfo.Action.DELETE)
                        continue;
                    availableHints.add(hintInfo.text);
                    if (hintInfo.selected)
                        selectedHints.add(hintInfo.text);
                }
            }

            prefs.edit()
                .putStringSet("available-search-hints", availableHints)
                .putStringSet("selected-search-hints", selectedHints)
                .apply();

            TBApplication.dataHandler(context).reloadProviders();
        }

        public void bindEditView(@NonNull View view) {
            ListView listView = view.findViewById(android.R.id.list);

            listView.setOnItemClickListener((list, itemView, position, id) -> {
                Adapter adapter = list.getAdapter();
                if (adapter instanceof SearchHintAdapter) {
                    SearchHintInfo info = ((SearchHintAdapter) adapter).getItem(position);
                    info.selected = !info.selected;
                    updateSearchHintList(info);
                }
            });

            listView.setOnItemLongClickListener((list, itemView, position, id) -> {
                Adapter adapter = list.getAdapter();
                if (!(adapter instanceof SearchHintAdapter))
                    return false;
                SearchHintInfo info = ((SearchHintAdapter) adapter).getItem(position);
                if (info.action == SearchHintInfo.Action.DELETE) {
                    info.action = info.text.equals(info.hint) ? SearchHintInfo.Action.NONE : SearchHintInfo.Action.RENAME;
                    updateSearchHintList(info);
                } else {
                    Context ctx = list.getContext();
                    ArrayAdapter<ListPopup.Item> arrayAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1);
                    arrayAdapter.add(new ListPopup.Item(ctx, R.string.menu_action_rename));
                    arrayAdapter.add(new ListPopup.Item(ctx, R.string.menu_action_delete));
                    ListPopup.create(ctx, arrayAdapter)
                        .setOnItemClickListener((popupAdapter, popupItemView, popupPosition) -> {
                            Object object = popupAdapter.getItem(popupPosition);
                            if (!(object instanceof ListPopup.Item))
                                return;
                            ListPopup.Item item = (ListPopup.Item) object;
                            if (item.stringId == R.string.menu_action_rename) {
                                launchRenameDialog(listView.getContext(), info);
                            } else if (item.stringId == R.string.menu_action_delete) {
                                info.action = SearchHintInfo.Action.DELETE;
                                updateSearchHintList(info);
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
            String name = addHintName.getValue();
            EditText editText = view.findViewById(android.R.id.text1);
            if (!TextUtils.isEmpty(name))
                editText.setText(name);
            editText.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(String newValue) {
                    addHintName.setValue(newValue);
                }
            });
        }

        private void launchRenameDialog(@NonNull Context ctx, @NonNull SearchHintInfo info) {
            DialogHelper.makeRenameDialog(ctx, info.text, (dialog, newName) -> {
                    boolean isValid = true;
                    ArrayList<SearchHintInfo> searchHintInfoList = searchHintList.getValue();
                    if (searchHintInfoList != null) {
                        for (SearchHintInfo hintInfo : searchHintInfoList) {
                            if (info.equals(hintInfo))
                                continue;
                            if (hintInfo.hint.equals(newName) || hintInfo.text.equals(newName)) {
                                isValid = false;
                                break;
                            }
                        }
                    }
                    if (!isValid) {
                        Toast.makeText(ctx, ctx.getString(R.string.invalid_rename_search_engine, newName), Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Set new name
                    info.text = newName;
                    info.action = info.hint.equals(info.text) ? SearchHintInfo.Action.NONE : SearchHintInfo.Action.RENAME;

                    updateSearchHintList(info);
                })
                .setTitle(R.string.title_rename_search_hint)
                .setNegativeButton(android.R.string.cancel, null)
                .getDialog()
                .show(getFragmentManager(), "rename");
        }

        public void onStartLifecycle(@NonNull Dialog dialog, @NonNull BasePreferenceDialog owner) {
            super.onStartLifecycle(dialog, owner);
            ListView listView = dialog.findViewById(android.R.id.list);
            if (listView != null) {
                ArrayList<SearchHintInfo> list = searchHintList.getValue();
                if (list == null)
                    list = new ArrayList<>();
                SearchHintAdapter adapter = new SearchHintAdapter(list);
                listView.setAdapter(adapter);
                searchHintList.observe(owner, adapter::replaceItems);
            }
        }

        public void loadDataInternal(@NonNull Context context, @NonNull SharedPreferences prefs) {
            ArrayList<SearchHintInfo> list = new ArrayList<>(0);
            Set<String> availableHints = prefs.getStringSet("available-search-hints", null);
            if (availableHints == null) {
                availableHints = new ArraySet<>();
                Collections.addAll(availableHints, context.getResources().getStringArray(R.array.defaultSearchHints));
            }
            Set<String> selectedHints = prefs.getStringSet("selected-search-hints", null);
            if (selectedHints == null) {
                selectedHints = new ArraySet<>(availableHints);
            }

            list.ensureCapacity(availableHints.size());

            for (String hint : availableHints) {
                SearchHintInfo hintInfo = new SearchHintInfo(hint);
                hintInfo.selected = selectedHints.contains(hintInfo.hint);
                list.add(hintInfo);
            }
            Collections.sort(list, Comparator.comparing(lhs -> lhs.hint));
            searchHintList.postValue(list);
        }

        public void loadDefaultsInternal(@NonNull Context context) {
            ArrayList<SearchHintInfo> list = new ArrayList<>(0);
            String[] defaultSearchHints = context.getResources().getStringArray(R.array.defaultSearchHints);
            list.ensureCapacity(defaultSearchHints.length);
            for (String searchHint : defaultSearchHints) {
                SearchHintInfo searchEngineInfo = new SearchHintInfo(searchHint);
                searchEngineInfo.selected = true;
                list.add(searchEngineInfo);
            }
            Collections.sort(list, Comparator.comparing(lhs -> lhs.hint));
            searchHintList.postValue(list);
        }

    }

    public static class SearchHintInfo {
        @NonNull
        public final String hint;
        public String text;
        public boolean selected;
        public Action action = Action.NONE;

        public enum Action {NONE, DELETE, RENAME}

        public SearchHintInfo(@NonNull String hintText) {
            hint = hintText;
            text = hintText;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
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

    public static class SearchHintAdapter extends ViewHolderListAdapter<SearchHintInfo, SearchHintVH> {

        public SearchHintAdapter(@NonNull List<SearchHintInfo> list) {
            super(SearchHintVH.class, android.R.layout.simple_list_item_checked, list);
        }

        @Override
        public int getItemViewTypeLayout(int viewType) {
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

        public void replaceItems(Collection<? extends SearchHintInfo> list) {
            if (list != mList) {
                mList.clear();
                mList.addAll(list);
            }
            notifyDataSetChanged();
        }
    }

    public static class SearchHintVH extends ViewHolderAdapter.ViewHolder<SearchHintInfo> {
        private final TextView text1View;

        public SearchHintVH(View view) {
            super(view);
            text1View = view.findViewById(android.R.id.text1);
        }

        @Override
        public void setContent(SearchHintInfo content, int position, @NonNull ViewHolderAdapter<SearchHintInfo, ? extends ViewHolderAdapter.ViewHolder<SearchHintInfo>> adapter) {
            text1View.setText(content.text);
            text1View.setTypeface(null, content.action == SearchHintInfo.Action.RENAME ? Typeface.BOLD : Typeface.NORMAL);
            if (text1View instanceof CheckedTextView)
                ((CheckedTextView) text1View).setChecked(content.selected);
            if (content.action == SearchHintInfo.Action.DELETE) {
                text1View.setPaintFlags(text1View.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }
}
