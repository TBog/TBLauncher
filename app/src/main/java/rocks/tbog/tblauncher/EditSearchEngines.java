package rocks.tbog.tblauncher;

import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.ArraySet;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import rocks.tbog.tblauncher.dataprovider.SearchProvider;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DialogHelper;
import rocks.tbog.tblauncher.utils.SimpleTextWatcher;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class EditSearchEngines extends AndroidViewModel {
    private final MutableLiveData<ArrayList<SearchEngineInfo>> searchEngineInfoList = new MutableLiveData<>();
    private final MutableLiveData<String> defaultProviderName = new MutableLiveData<>();
    private final MutableLiveData<String> addSearchEngineName = new MutableLiveData<>();
    private final MutableLiveData<String> addSearchEngineUrl = new MutableLiveData<>();

    public EditSearchEngines(@NonNull Application application) {
        super(application);
    }

    public LiveData<ArrayList<SearchEngineInfo>> getSearchEngineInfoList() {
        return searchEngineInfoList;
    }

    public LiveData<String> getDefaultProviderName() {
        return defaultProviderName;
    }

    public void setDefaultProviderName(String name) {
        defaultProviderName.setValue(name);
    }

    public void updateSearchEngineInfoList(SearchEngineInfo info) {
        ArrayList<SearchEngineInfo> arrayList = searchEngineInfoList.getValue();
        if (arrayList == null || arrayList.contains(info))
            searchEngineInfoList.setValue(arrayList);
    }

    public void loadDefaults(@NonNull Context context) {
        final ArrayList<SearchEngineInfo> list = new ArrayList<>(0);
        Utilities.runAsync(() -> {
            Set<String> defaultSearchProviders = SearchProvider.getDefaultSearchProviders(context);
            list.ensureCapacity(defaultSearchProviders.size());
            for (String searchProvider : defaultSearchProviders) {
                SearchEngineInfo searchEngineInfo = new SearchEngineInfo(searchProvider);
                searchEngineInfo.selected = true;
                list.add(searchEngineInfo);
            }
            Collections.sort(list, (lhs, rhs) -> lhs.provider.compareTo(rhs.provider));

            searchEngineInfoList.postValue(list);
            defaultProviderName.postValue("Google");
        });
    }

    public void loadData(@NonNull Context context, @NonNull SharedPreferences prefs) {
        final ArrayList<SearchEngineInfo> list = new ArrayList<>(0);
        Utilities.runAsync(() -> {
            // load search engines
            Set<String> availableSearchProviders = SearchProvider.getAvailableSearchProviders(context, prefs);
            Set<String> selectedProviderNames = SearchProvider.getSelectedProviderNames(context, prefs);

            list.ensureCapacity(availableSearchProviders.size());

            for (String searchProvider : availableSearchProviders) {
                SearchEngineInfo searchEngineInfo = new SearchEngineInfo(searchProvider);
                searchEngineInfo.selected = selectedProviderNames.contains(searchEngineInfo.name);
                list.add(searchEngineInfo);
            }
            Collections.sort(list, (lhs, rhs) -> lhs.provider.compareTo(rhs.provider));
            searchEngineInfoList.postValue(list);

            // get default search engine name
            String providerName = prefs.getString("default-search-provider", null);
            if (providerName == null || providerName.isEmpty())
                defaultProviderName.postValue(list.isEmpty() ? "" : list.get(0).name);
            else
                defaultProviderName.postValue(providerName);
        });
    }

    public void applyChanges(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> availableProviders = new ArraySet<>();
        Set<String> selectedProviderNames = new ArraySet<>();

        String addName = addSearchEngineName.getValue();
        String addUrl = addSearchEngineUrl.getValue();
        if (addName != null && addUrl != null) {
            String name = SearchProvider.sanitizeProviderName(addName).trim();
            String url = SearchProvider.sanitizeProviderUrl(addUrl).trim();
            if (!name.isEmpty() && !url.isEmpty()) {
                String searchProvider = SearchProvider.makeProvider(name, url);
                availableProviders.add(searchProvider);
                selectedProviderNames.add(name);
            }
        }

        ArrayList<SearchEngineInfo> searchEngineList = getSearchEngineInfoList().getValue();
        if (searchEngineList != null) {
            for (SearchEngineInfo searchEngineInfo : searchEngineList) {
                if (searchEngineInfo.action == SearchEngineInfo.Action.DELETE)
                    continue;
                availableProviders.add(SearchProvider.makeProvider(searchEngineInfo.name, searchEngineInfo.url));
                if (searchEngineInfo.selected)
                    selectedProviderNames.add(searchEngineInfo.name);
            }
        }

        prefs.edit()
            .putStringSet("available-search-providers", availableProviders)
            .putStringSet("selected-search-provider-names", selectedProviderNames)
            .putString("default-search-provider", getDefaultProviderName().getValue())
            .apply();

        TBApplication.dataHandler(context).reloadProviders();
    }

    public void bindEditView(@NonNull View view) {
        ListView listView = view.findViewById(android.R.id.list);

        listView.setOnItemClickListener((list, itemView, position, id) -> {
            Adapter adapter = list.getAdapter();
            if (adapter instanceof SearchEngineAdapter) {
                SearchEngineAdapter searchEngineAdapter = (SearchEngineAdapter) adapter;

                SearchEngineInfo info = searchEngineAdapter.getItem(position);
                info.selected = !info.selected;
                updateSearchEngineInfoList(info);
            }
        });

        listView.setOnItemLongClickListener((list, itemView, position, id) -> {
            Adapter adapter = list.getAdapter();
            if (!(adapter instanceof SearchEngineAdapter))
                return false;
            SearchEngineAdapter searchEngineAdapter = (SearchEngineAdapter) adapter;
            SearchEngineInfo info = searchEngineAdapter.getItem(position);
            if (info.action == SearchEngineInfo.Action.DELETE) {
                String provider = SearchProvider.makeProvider(info.name, info.url);
                info.action = info.provider.equals(provider) ? SearchEngineInfo.Action.NONE : SearchEngineInfo.Action.RENAME;
                updateSearchEngineInfoList(info);
            } else {
                Context ctx = list.getContext();
                ArrayAdapter<ListPopup.Item> arrayAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1);
                ListPopup popup = ListPopup.create(ctx, arrayAdapter);
                if (!info.name.equals(getDefaultProviderName().getValue()) && info.selected)
                    arrayAdapter.add(new ListPopup.Item(ctx, R.string.search_engine_set_default));
                arrayAdapter.add(new ListPopup.Item(ctx, R.string.menu_action_rename));
                arrayAdapter.add(new ListPopup.Item(ctx, R.string.search_engine_edit_url));
                arrayAdapter.add(new ListPopup.Item(ctx, R.string.menu_action_delete));
                popup.setOnItemClickListener((popupAdapter, popupItemView, popupPosition) -> {
                    Object object = popupAdapter.getItem(popupPosition);
                    if (!(object instanceof ListPopup.Item))
                        return;
                    ListPopup.Item item = (ListPopup.Item) object;
                    if (item.stringId == R.string.search_engine_set_default) {
                        setDefaultProviderName(info.name);
                    } else if (item.stringId == R.string.menu_action_rename) {
                        launchRenameDialog(ctx, info);
                    } else if (item.stringId == R.string.search_engine_edit_url) {
                        launchEditUrlDialog(ctx, info);
                    } else if (item.stringId == R.string.menu_action_delete) {
                        info.action = SearchEngineInfo.Action.DELETE;
                        updateSearchEngineInfoList(info);
                    }
                });
                popup.setModal(true);
                popup.setDimAmount(0.5f);
                popup.show(itemView);
            }
            return true;
        });
    }

    public void bindAddView(@NonNull View view) {
        EditText editText;
        {
            String name = addSearchEngineName.getValue();
            editText = view.findViewById(android.R.id.text1);
            if (!TextUtils.isEmpty(name))
                editText.setText(name);
            editText.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(String newValue) {
                    addSearchEngineName.setValue(name);
                }
            });
        }
        {
            String urlValue = addSearchEngineUrl.getValue();
            editText = view.findViewById(android.R.id.text2);
            if (!TextUtils.isEmpty(urlValue))
                editText.setText(urlValue);
            editText.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(String newValue) {
                    addSearchEngineUrl.setValue(urlValue);
                }
            });
        }
    }

    private void launchRenameDialog(Context ctx, SearchEngineInfo info) {
        DialogHelper.makeRenameDialog(ctx, info.name, (dialog, name) -> {
            String newName = SearchProvider.sanitizeProviderName(name).trim();
            boolean isValid = !newName.isEmpty();
            ArrayList<SearchEngineInfo> searchEngineList = getSearchEngineInfoList().getValue();
            if (searchEngineList != null) {
                for (SearchEngineInfo searchEngineInfo : searchEngineList) {
                    if (searchEngineInfo == info)
                        continue;
                    if (SearchProvider.getProviderName(searchEngineInfo.provider).equals(newName) || searchEngineInfo.name.equals(newName)) {
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
            if (TextUtils.equals(defaultProviderName.getValue(), info.name))
                setDefaultProviderName(newName);
            info.name = newName;
            info.action = SearchProvider.getProviderName(info.provider).equals(info.name) ? SearchEngineInfo.Action.NONE : SearchEngineInfo.Action.RENAME;
            updateSearchEngineInfoList(info);
        })
            .setTitle(R.string.title_rename_search_engine)
            .show();
    }

    private void launchEditUrlDialog(Context ctx, SearchEngineInfo info) {
        ContextThemeWrapper context = new ContextThemeWrapper(ctx, R.style.NoTitleDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.title_edit_url_search_engine));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.dialog_rename);
        } else {
            builder.setView(View.inflate(context, R.layout.dialog_rename, null));
        }

        builder.setPositiveButton(R.string.confirm_edit_url_search_engine, (dialog, which) -> {
            EditText input = ((AlertDialog) dialog).findViewById(R.id.rename);
            if (input == null)
                return;

            // Set new name
            info.url = SearchProvider.sanitizeProviderUrl(input.getText().toString()).trim();
            if (info.url.equals(SearchProvider.getProviderUrl(info.provider))) {
                info.action = SearchProvider.getProviderName(info.provider).equals(info.name) ? SearchEngineInfo.Action.NONE : SearchEngineInfo.Action.RENAME;
            } else {
                info.action = SearchEngineInfo.Action.RENAME;
            }

            updateSearchEngineInfoList(info);

            dialog.dismiss();
        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
        // call after dialog got inflated (show call)
        TextView nameView = dialog.findViewById(R.id.rename);
        assert nameView != null;
        nameView.setText(info.url);
        nameView.requestFocus();
    }

    public void onStartLifecycle(@NonNull Dialog dialog, @NonNull LifecycleOwner viewLifecycleOwner) {
        ListView listView = dialog.findViewById(android.R.id.list);
        if (listView != null) {
            ArrayList<SearchEngineInfo> list = getSearchEngineInfoList().getValue();
            if (list == null)
                list = new ArrayList<>();
            SearchEngineAdapter adapter = new SearchEngineAdapter(list);
            listView.setAdapter(adapter);
            getSearchEngineInfoList().observe(viewLifecycleOwner, adapter::replaceItems);
        }
    }

    static class SearchEngineAdapter extends ViewHolderListAdapter<SearchEngineInfo, TagViewHolder> {

        SearchEngineAdapter(@NonNull ArrayList<SearchEngineInfo> list) {
            super(TagViewHolder.class, android.R.layout.simple_list_item_checked, list);
        }

        @Override
        protected int getItemViewTypeLayout(int viewType) {
            if (viewType == 1)
                return android.R.layout.simple_list_item_1;
            return super.getItemViewTypeLayout(viewType);
        }

        public int getItemViewType(int position) {
            return getItem(position).action == SearchEngineInfo.Action.DELETE ? 1 : 0;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public void replaceItems(Collection<? extends SearchEngineInfo> list) {
            if (list != mList) {
                mList.clear();
                mList.addAll(list);
            }
            notifyDataSetChanged();
        }
    }

    public static class TagViewHolder extends ViewHolderAdapter.ViewHolder<SearchEngineInfo> {
        private final TextView text1View;

        public TagViewHolder(View itemView) {
            super(itemView);
            text1View = itemView.findViewById(android.R.id.text1);
            text1View.setLines(2);
        }

        @Override
        protected void setContent(SearchEngineInfo content, int position, @NonNull ViewHolderAdapter<SearchEngineInfo, ? extends ViewHolderAdapter.ViewHolder<SearchEngineInfo>> adapter) {
            SpannableStringBuilder enhancedText = new SpannableStringBuilder();
            enhancedText
                .append(content.name)
                .setSpan(new UnderlineSpan(), 0, content.name.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            enhancedText
                .append("\n")
                .append(content.url);
            text1View.setText(enhancedText);
            text1View.setTypeface(null, content.action == SearchEngineInfo.Action.RENAME ? Typeface.BOLD : Typeface.NORMAL);
            if (text1View instanceof CheckedTextView)
                ((CheckedTextView) text1View).setChecked(content.selected);
            if (content.action == SearchEngineInfo.Action.DELETE) {
                text1View.setPaintFlags(text1View.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }

    public static class SearchEngineInfo {
        @NonNull
        final String provider;
        String name;
        String url;
        public boolean selected;
        Action action = Action.NONE;

        enum Action {NONE, DELETE, RENAME}

        public SearchEngineInfo(@NonNull String searchProvider) {
            provider = searchProvider;
            name = SearchProvider.getProviderName(searchProvider);
            url = SearchProvider.getProviderUrl(searchProvider);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SearchEngineInfo that = (SearchEngineInfo) o;
            return selected == that.selected &&
                provider.equals(that.provider) &&
                Objects.equals(name, that.name) &&
                Objects.equals(url, that.url) &&
                action == that.action;
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, name, url, selected, action);
        }
    }
}
