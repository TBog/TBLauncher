package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.UnderlineSpan;
import android.view.ContextThemeWrapper;
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
import java.util.Set;

import rocks.tbog.tblauncher.dataprovider.SearchProvider;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;

public class EditSearchEngines {

    private final ArrayList<SearchEngineInfo> mSearchEngineList = new ArrayList<>();
    private ListView mListView = null;
    private SearchEngineAdapter mAdapter = null;
    private EditText mAddName = null;
    private EditText mAddUrl = null;
    @NonNull
    String defaultProviderName = "";

    public void applyChanges(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> availableProviders = new ArraySet<>();
        Set<String> selectedProviderNames = new ArraySet<>();

        if (mAddName != null && mAddUrl != null) {
            String name = SearchProvider.sanitizeProviderName(mAddName.getText().toString()).trim();
            String url = SearchProvider.sanitizeProviderUrl(mAddUrl.getText().toString()).trim();
            if (!name.isEmpty() && !url.isEmpty()) {
                String searchProvider = SearchProvider.makeProvider(name, url);
                availableProviders.add(searchProvider);
                selectedProviderNames.add(name);
            }
        }

        for (SearchEngineInfo searchEngineInfo : mSearchEngineList) {
            if (searchEngineInfo.action == SearchEngineInfo.Action.DELETE)
                continue;
            availableProviders.add(SearchProvider.makeProvider(searchEngineInfo.name, searchEngineInfo.url));
            if (searchEngineInfo.selected)
                selectedProviderNames.add(searchEngineInfo.name);
        }

        prefs.edit()
                .putStringSet("available-search-providers", availableProviders)
                .putStringSet("selected-search-provider-names", selectedProviderNames)
                .putString("default-search-provider", defaultProviderName)
                .apply();

        TBApplication.dataHandler(context).reloadProviders();
    }

    public void bindEditView(@NonNull View view) {
        final Context context = view.getContext();
        mListView = view.findViewById(android.R.id.list);

        // prepare the grid with all the tags
        mAdapter = new SearchEngineAdapter(mSearchEngineList);

        mListView.setOnItemClickListener((listView, itemView, position, id) -> {
            SearchEngineInfo info = mAdapter.getItem(position);
            info.selected = !info.selected;
            mAdapter.notifyDataSetChanged();
        });

        mListView.setOnItemLongClickListener((listView, itemView, position, id) -> {
            SearchEngineInfo info = mAdapter.getItem(position);
            if (info.action == SearchEngineInfo.Action.DELETE) {
                String provider = SearchProvider.makeProvider(info.name, info.url);
                info.action = info.provider.equals(provider) ? SearchEngineInfo.Action.NONE : SearchEngineInfo.Action.RENAME;
                mAdapter.notifyDataSetChanged();
            } else {
                Context ctx = listView.getContext();
                ArrayAdapter<ListPopup.Item> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1);
                ListPopup popup = ListPopup.create(ctx, adapter);
                if (!info.name.equals(defaultProviderName) && info.selected)
                    adapter.add(new ListPopup.Item(ctx, R.string.search_engine_set_default));
                adapter.add(new ListPopup.Item(ctx, R.string.search_engine_rename));
                adapter.add(new ListPopup.Item(ctx, R.string.search_engine_edit_url));
                adapter.add(new ListPopup.Item(ctx, R.string.search_engine_delete));
                popup.setOnItemClickListener((popupAdapter, popupItemView, popupPosition) -> {
                    Object object = popupAdapter.getItem(popupPosition);
                    if (!(object instanceof ListPopup.Item))
                        return;
                    ListPopup.Item item = (ListPopup.Item) object;
                    switch (item.stringId) {
                        case R.string.search_engine_set_default:
                            defaultProviderName = info.name;
                            break;
                        case R.string.search_engine_rename:
                            launchRenameDialog(ctx, info);
                            break;
                        case R.string.search_engine_edit_url:
                            launchEditUrlDialog(ctx, info);
                            break;
                        case R.string.search_engine_delete: {
                            info.action = SearchEngineInfo.Action.DELETE;
                            mAdapter.notifyDataSetChanged();
                        }
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
        mAddName = view.findViewById(android.R.id.text1);
        mAddUrl = view.findViewById(android.R.id.text2);
    }

    private void launchRenameDialog(Context ctx, SearchEngineInfo info) {
        ContextThemeWrapper context = new ContextThemeWrapper(ctx, R.style.NoTitleDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.title_rename_search_engine));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.dialog_rename);
        } else {
            builder.setView(View.inflate(context, R.layout.dialog_rename, null));
        }

        builder.setPositiveButton(R.string.custom_name_rename, (dialog, which) -> {
            EditText input = ((AlertDialog) dialog).findViewById(R.id.rename);
            if (input == null)
                return;
            String newName = SearchProvider.sanitizeProviderName(input.getText().toString()).trim();
            boolean isValid = !newName.isEmpty();
            for (SearchEngineInfo searchEngineInfo : mSearchEngineList) {
                if (searchEngineInfo == info)
                    continue;
                if (SearchProvider.getProviderName(searchEngineInfo.provider).equals(newName) || searchEngineInfo.name.equals(newName)) {
                    isValid = false;
                    break;
                }
            }
            if (!isValid) {
                Toast.makeText(ctx, ctx.getString(R.string.invalid_rename_search_engine, newName), Toast.LENGTH_LONG).show();
                return;
            }

            // Set new name
            if (defaultProviderName.equals(info.name))
                defaultProviderName = newName;
            info.name = newName;
            info.action = SearchProvider.getProviderName(info.provider).equals(info.name) ? SearchEngineInfo.Action.NONE : SearchEngineInfo.Action.RENAME;

            mAdapter.notifyDataSetChanged();

            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        // call after dialog got inflated (show call)
        TextView nameView = dialog.findViewById(R.id.rename);
        assert nameView != null;
        nameView.setText(info.name);
        nameView.requestFocus();
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

            mAdapter.notifyDataSetChanged();

            dialog.dismiss();
        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        // call after dialog got inflated (show call)
        TextView nameView = dialog.findViewById(R.id.rename);
        assert nameView != null;
        nameView.setText(info.url);
        nameView.requestFocus();
    }

    public void onStart() {
        // Set list adapter after the view inflated
        // This is a workaround to fix listview items not having the correct width
        if (mListView != null)
            mListView.post(() -> mListView.setAdapter(mAdapter));
    }

    public void loadData(@NonNull Context context, @NonNull SharedPreferences prefs) {
        // load search engines
        {
            Set<String> availableSearchProviders = SearchProvider.getAvailableSearchProviders(context, prefs);
            Set<String> selectedProviderNames = SearchProvider.getSelectedProviderNames(context, prefs);

            for (String searchProvider : availableSearchProviders) {
                SearchEngineInfo searchEngineInfo = new SearchEngineInfo(searchProvider);
                searchEngineInfo.selected = selectedProviderNames.contains(searchEngineInfo.name);
                mSearchEngineList.add(searchEngineInfo);
            }
            Collections.sort(mSearchEngineList, (lhs, rhs) -> lhs.provider.compareTo(rhs.provider));
        }

        // get default search engine name
        {
            String providerName = prefs.getString("default-search-provider", null);
            if (providerName == null || providerName.isEmpty())
                defaultProviderName = mSearchEngineList.isEmpty() ? "" : mSearchEngineList.get(0).name;
            else
                defaultProviderName = providerName;
        }
    }

    public void loadDefaults(@NonNull Context context) {
        Set<String> defaultSearchProviders = SearchProvider.getDefaultSearchProviders(context);
        for (String searchProvider : defaultSearchProviders) {
            SearchEngineInfo searchEngineInfo = new SearchEngineInfo(searchProvider);
            searchEngineInfo.selected = true;
            mSearchEngineList.add(searchEngineInfo);
        }
        Collections.sort(mSearchEngineList, (lhs, rhs) -> lhs.provider.compareTo(rhs.provider));
        defaultProviderName = "Google";
    }

    static class SearchEngineAdapter extends ViewHolderAdapter<SearchEngineInfo, TagViewHolder> {
        private final ArrayList<SearchEngineInfo> mTags;

        SearchEngineAdapter(@NonNull ArrayList<SearchEngineInfo> tags) {
            super(TagViewHolder.class, android.R.layout.simple_list_item_checked);
            mTags = tags;
        }

        @Override
        protected int getListItemLayout(int position) {
            if (getItemViewType(position) == 1)
                return android.R.layout.simple_list_item_1;
            return super.getListItemLayout(position);
        }

        @Override
        public int getCount() {
            return mTags.size();
        }

        @Override
        public SearchEngineInfo getItem(int position) {
            return mTags.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        public int getItemViewType(int position) {
            return getItem(position).action == SearchEngineInfo.Action.DELETE ? 1 : 0;
        }

        public int getViewTypeCount() {
            return 2;
        }
    }

    public static class TagViewHolder extends ViewHolderAdapter.ViewHolder<SearchEngineInfo> {
        TextView text1View;

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

    static class SearchEngineInfo {
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
    }
}
