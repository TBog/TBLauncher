package rocks.tbog.tblauncher;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import rocks.tbog.tblauncher.dataprovider.ActionProvider;
import rocks.tbog.tblauncher.dataprovider.AppCacheProvider;
import rocks.tbog.tblauncher.dataprovider.AppProvider;
import rocks.tbog.tblauncher.dataprovider.ContactsProvider;
import rocks.tbog.tblauncher.dataprovider.FavProvider;
import rocks.tbog.tblauncher.dataprovider.FilterProvider;
import rocks.tbog.tblauncher.dataprovider.IProvider;
import rocks.tbog.tblauncher.dataprovider.Provider;
import rocks.tbog.tblauncher.dataprovider.ShortcutsProvider;
import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.FavRecord;
import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.db.ValuedHistoryRecord;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class DataHandler extends BroadcastReceiver
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    final static private String TAG = "DataHandler";

    /**
     * Package the providers reside in
     */
    final static private String PROVIDER_PREFIX = IProvider.class.getPackage().getName() + ".";
    /**
     * List all known providers
     */
    final static private List<String> PROVIDER_NAMES = Arrays.asList(
            //TODO: enable providers when ready
//            "app", "contacts", "shortcuts"
            "app"
            , "contacts"
            , "shortcuts"
    );

    final private Context context;
    private String currentQuery;
    private final Map<String, ProviderEntry> providers = new HashMap<>();
    private boolean mFullLoadOverSent = false;
    private long start;

    /**
     * Initialize all providers
     */
    public DataHandler(Context context) {
        // Make sure we are in the context of the main application
        // (otherwise we might receive an exception about broadcast listeners not being able
        //  to bind to services)
        this.context = context.getApplicationContext();

        start = System.currentTimeMillis();

        IntentFilter intentFilter = new IntentFilter(TBLauncherActivity.LOAD_OVER);
        this.context.registerReceiver(this, intentFilter);

        Intent i = new Intent(TBLauncherActivity.START_LOAD);
        this.context.sendBroadcast(i);

        // Monitor changes for service preferences (to automatically start and stop services)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Connect to initial providers
        // Those are the complex providers, that are defined as Android services
        // to survive even if the app's UI is killed
        // (this way, we don't need to reload the app list as often)
        for (String providerName : PROVIDER_NAMES) {
            if (prefs.getBoolean("enable-" + providerName, true)) {
                this.connectToProvider(providerName, 0);
            }
        }

        /*
         * Some basic providers are defined directly, as we don't need the overhead of a service
         * for them. These providers don't expose a service connection, and you can't bind / unbind
         * to them dynamically.
         */

        // QuickListProvider
        {
            ProviderEntry providerEntry = new ProviderEntry();
            providerEntry.provider = new FilterProvider(context);
            providers.put("filters", providerEntry);
        }
        {
            ProviderEntry providerEntry = new ProviderEntry();
            providerEntry.provider = new FavProvider(context);
            providers.put("favorites", providerEntry);
        }
        {
            ProviderEntry providerEntry = new ProviderEntry();
            providerEntry.provider = new ActionProvider(context);
            providers.put("actions", providerEntry);
        }

//        ProviderEntry calculatorEntry = new ProviderEntry();
//        calculatorEntry.provider = new CalculatorProvider();
//        this.providers.put("calculator", calculatorEntry);
//        ProviderEntry phoneEntry = new ProviderEntry();
//        phoneEntry.provider = new PhoneProvider(context);
//        this.providers.put("phone", phoneEntry);
//        ProviderEntry searchEntry = new ProviderEntry();
//        searchEntry.provider = new SearchProvider(context);
//        this.providers.put("search", searchEntry);
//        ProviderEntry settingsEntry = new ProviderEntry();
//        settingsEntry.provider = new SettingsProvider(context);
//        this.providers.put("settings", settingsEntry);
//        ProviderEntry tagsEntry = new ProviderEntry();
//        tagsEntry.provider = new TagsProvider();
//        this.providers.put("tags", tagsEntry);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.startsWith("enable-")) {
            String providerName = key.substring(7);
            if (PROVIDER_NAMES.contains(providerName)) {
                if (sharedPreferences.getBoolean(key, true)) {
                    this.connectToProvider(providerName, 0);
                } else {
                    this.disconnectFromProvider(providerName);
                }
            }
        }
    }

    /**
     * Generate an intent that can be used to start or stop the given provider
     *
     * @param name The name of the provider
     * @return Android intent for this provider
     */
    private Intent providerName2Intent(String name) {
        // Build expected fully-qualified provider class name
        StringBuilder className = new StringBuilder(50);
        className.append(PROVIDER_PREFIX);
        className.append(Character.toUpperCase(name.charAt(0)));
        className.append(name.substring(1).toLowerCase(Locale.ROOT));
        className.append("Provider");

        // Try to create reflection class instance for class name
        try {
            return new Intent(this.context, Class.forName(className.toString()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Require the data handler to be connected to the data provider with the given name
     *
     * @param name Data provider name (i.e.: `ContactsProvider` → `"contacts"`)
     */
    private void connectToProvider(final String name, final int counter) {
        // Do not continue if this provider has already been connected to
        if (this.providers.containsKey(name)) {
            return;
        }

        Log.v(TAG, "Connecting to " + name);


        // Find provider class for the given service name
        final Intent intent = this.providerName2Intent(name);
        if (intent == null) {
            return;
        }

        try {
            // Send "start service" command first so that the service can run independently
            // of the activity
            this.context.startService(intent);
        } catch (IllegalStateException e) {
            // When KISS is the default launcher,
            // the system will try to start KISS in the background after a reboot
            // however at this point we're not allowed to start services, and an IllegalStateException will be thrown
            // We'll then add a broadcast receiver for the next time the user turns his screen on
            // (or passes the lockscreen) to retry at this point
            // https://github.com/Neamar/KISS/issues/1130
            // https://github.com/Neamar/KISS/issues/1154
            Log.w(TAG, "Unable to start service for " + name + ". KISS is probably not in the foreground. Service will automatically be started when KISS gets to the foreground.");

            if (counter > 20) {
                Log.e(TAG, "Already tried and failed twenty times to start service. Giving up.");
                return;
            }

            // Add a receiver to get notified next time the screen is on
            // or next time the users successfully dismisses his lock screen
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, Intent intent) {
                    // Is there a lockscreen still visible to the user?
                    // If yes, we can't start background services yet, so we'll need to wait until we get ACTION_USER_PRESENT
                    KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                    assert myKM != null;
                    boolean isPhoneLocked = myKM.inKeyguardRestrictedInputMode();
                    if (!isPhoneLocked) {
                        context.unregisterReceiver(this);
                        final Handler handler = new Handler();
                        // Even when all the stars are aligned,
                        // starting the service needs to be slightly delayed because the Intent is fired *before* the app is considered in the foreground.
                        // Each new release of Android manages to make the developer life harder.
                        // Can't wait for the next one.
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "Screen turned on or unlocked, retrying to start background services");
                                connectToProvider(name, counter + 1);
                            }
                        }, 10);
                    }
                }
            }, intentFilter);

            // Stop here for now, the Receiver will re-trigger the whole flow when services can be started.
            return;
        }

        final ProviderEntry entry = new ProviderEntry();
        // Add empty provider object to list of providers
        this.providers.put(name, entry);

        // Connect and bind to provider service
        this.context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.i(TAG, "onServiceConnected " + className);

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                Provider<?>.LocalBinder binder = (Provider<?>.LocalBinder) service;
                IProvider provider = binder.getService();

                // Update provider info so that it contains something useful
                entry.provider = provider;
                entry.connection = this;

                if (provider.isLoaded()) {
                    handleProviderLoaded();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                Log.i(TAG, "onServiceDisconnected " + arg0);
            }
        }, Context.BIND_AUTO_CREATE);
    }

    /**
     * Terminate any connection between the data handler and the data provider with the given name
     *
     * @param name Data provider name (i.e.: `AppProvider` → `"app"`)
     */
    private void disconnectFromProvider(String name) {
        // Skip already disconnected services
        ProviderEntry entry = this.providers.get(name);
        if (entry == null) {
            return;
        }

        // Disconnect from provider service
        this.context.unbindService(entry.connection);

        // Stop provider service
        this.context.stopService(new Intent(this.context, entry.provider.getClass()));

        // Remove provider from list
        this.providers.remove(name);
    }

    private boolean allProvidersHaveLoaded() {
        for (ProviderEntry entry : this.providers.values())
            if (entry.provider == null || !entry.provider.isLoaded())
                return false;
        return true;
    }

    private boolean providersHaveLoaded(int step) {
        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider == null) {
                return false;
            }
            if (step == entry.provider.getLoadStep() && !entry.provider.isLoaded()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called when some event occurred that makes us believe that all data providers
     * might be ready now
     */
    private void handleProviderLoaded() {
        if (mFullLoadOverSent) {
            return;
        }

        for (int step : IProvider.LOAD_STEPS) {
            boolean stepLoaded = true;
            for (ProviderEntry entry : this.providers.values()) {
                if (entry.provider == null)
                    return;
                if (step == entry.provider.getLoadStep() && !entry.provider.isLoaded()) {
                    stepLoaded = false;
                    entry.provider.reload(false);
                }
            }
            if (!stepLoaded)
                return;
        }

        if (!allProvidersHaveLoaded())
            return;

        long time = System.currentTimeMillis() - start;
        Log.v(TAG, "Time to load all providers: " + time + "ms");

        mFullLoadOverSent = true;

        // Broadcast the fact that the new providers list is ready
        try {
            this.context.unregisterReceiver(this);
            Intent i = new Intent(TBLauncherActivity.FULL_LOAD_OVER);
            this.context.sendBroadcast(i);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "send FULL_LOAD_OVER", e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // A provider finished loading and contacted us
        this.handleProviderLoaded();
    }

    /**
     * Get records for this query.
     *
     * @param query    query to run
     * @param searcher the searcher currently running
     */
    @WorkerThread
    public void requestResults(String query, Searcher searcher) {
        currentQuery = query;
        for (Map.Entry<String, ProviderEntry> setEntry : this.providers.entrySet()) {
            if (searcher.isCancelled())
                break;
            IProvider provider = setEntry.getValue().provider;
            if (provider == null || !provider.isLoaded()) {
                // if the apps provider has not finished yet, return the cached ones
                if ("app".equals(setEntry.getKey()))
                    provider = new AppCacheProvider(context, getCachedApps());
                else
                    continue;
            }
            // Retrieve results for query:
            provider.requestResults(query, searcher);
        }
    }

    /**
     * Get records for this query.
     *
     * @param searcher the searcher currently running
     */
    public void requestAllRecords(Searcher searcher) {
        for (ProviderEntry entry : this.providers.values()) {
            if (searcher.isCancelled())
                break;
            if (entry.provider == null)
                continue;

            List<? extends EntryItem> pojos = entry.provider.getPojos();
            if (pojos != null)
                searcher.addResult(pojos.toArray(new EntryItem[0]));
        }
    }

    /**
     * Return previously selected items.<br />
     * May return null if no items were ever selected (app first use)<br />
     * May return an empty set if the providers are not done building records,
     * in this case it is probably a good idea to call this function 500ms after
     *
     * @param context            android context
     * @param itemCount          max number of items to retrieve, total number may be less (search or calls are not returned for instance)
     * @param historyMode        Recency vs Frecency vs Frequency vs Adaptive
     * @param sortHistory        Sort history entries alphabetically
     * @param itemsToExcludeById Items to exclude from history by their id
     * @return pojos in recent history
     */
    public ArrayList<EntryItem> getHistory(Context context, int itemCount, String historyMode,
                                           boolean sortHistory, Set<String> itemsToExcludeById) {
        // Pre-allocate array slots that are likely to be used based on the current maximum item
        // count
        ArrayList<EntryItem> history = new ArrayList<>(Math.min(itemCount, 256));

        // Max sure that we get enough items, regardless of how many may be excluded
        int extendedItemCount = itemCount + itemsToExcludeById.size();

        // Read history
        List<ValuedHistoryRecord> ids = DBHelper.getHistory(context, extendedItemCount, historyMode, sortHistory);

        // Find associated items
        for (int i = 0; i < ids.size(); i++) {
            // Ask all providers if they know this id
            EntryItem pojo = getPojo(ids.get(i).record);

            if (pojo == null) {
                continue;
            }

            if (itemsToExcludeById.contains(pojo.id)) {
                continue;
            }

            history.add(pojo);

            // Break if maximum number of items have been retrieved
            if (history.size() >= itemCount) {
                break;
            }
        }

        return history;
    }

    public int getHistoryLength() {
        return DBHelper.getHistoryLength(this.context);
    }

    /**
     * Query database for item and return its name
     *
     * @param id globally unique ID, usually starts with provider scheme, e.g. "app://" or "contact://"
     * @return name of item (i.e. app name)
     */
    public String getItemName(String id) {
        // Ask all providers if they know this id
        EntryItem pojo = getPojo(id);

        return (pojo != null) ? pojo.getName() : "???";
    }

    public boolean addShortcut(ShortcutRecord record) {
        Log.d(TAG, "Adding shortcut " + record.displayName + " for " + record.packageName);
        if (DBHelper.insertShortcut(this.context, record)) {
            ShortcutsProvider provider = getShortcutsProvider();
            if (provider != null)
                provider.reload(true);
            return true;
        }
        return false;
    }

    public void addShortcut(String packageName) {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        List<ShortcutInfo> shortcuts;
        try {
            shortcuts = ShortcutUtil.getShortcut(context, packageName);
        } catch (SecurityException | IllegalStateException e) {
            e.printStackTrace();
            return;
        }

        for (ShortcutInfo shortcutInfo : shortcuts) {
            // Create Pojo
            ShortcutRecord record = ShortcutUtil.createShortcutRecord(context, shortcutInfo, true);
            if (record == null) {
                continue;
            }
            // Add shortcut to the DataHandler
            addShortcut(record);
        }

        if (!shortcuts.isEmpty() && this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload(true);
        }
    }

    public void clearHistory() {
        DBHelper.clearHistory(this.context);
    }

    public void removeShortcut(ShortcutEntry shortcut) {
        // Also remove shortcut from favorites
        removeFromFavorites(shortcut);
        DBHelper.removeShortcut(this.context, shortcut);

        if (shortcut.mShortcutInfo != null) {
            ShortcutUtil.removeShortcut(context, shortcut.mShortcutInfo);
        }

        if (this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload(true);
        }
    }

    public void removeShortcuts(String packageName) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Remove all shortcuts from favorites for given package name
        List<ShortcutRecord> shortcutsList = DBHelper.getShortcuts(context, packageName);
        for (ShortcutRecord shortcut : shortcutsList) {
            String id = ShortcutEntry.generateShortcutId(shortcut.dbId, shortcut.displayName);
            EntryItem entry = getPojo(id);
            if (entry != null)
                removeFromFavorites(entry);
        }

        DBHelper.removeShortcuts(this.context, packageName);

        if (this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload(true);
        }
    }

    @NonNull
    public Set<String> getExcludedFromHistory() {
        Set<String> excluded = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("excluded-apps-from-history", null);
        if (excluded == null) {
            excluded = new HashSet<>();
            excluded.add(context.getPackageName());
        }
        return excluded;
    }

    @NonNull
    public Set<String> getExcluded() {
        Set<String> excluded = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("excluded-apps", null);
        if (excluded == null) {
            excluded = new HashSet<>();
            excluded.add(context.getPackageName());
        }
        return excluded;
    }

//    public void addToExcludedFromHistory(AppEntry app) {
//        // The set needs to be cloned and then edited,
//        // modifying in place is not supported by putStringSet()
//        Set<String> excluded = new HashSet<>(getExcludedFromHistory());
//        excluded.add(app.id);
//
//        if (ShortcutUtil.areShortcutsEnabled(context)) {
//            // Add all shortcuts for given package name to being excluded from history
//            List<ShortcutRecord> shortcutsList = DBHelper.getShortcuts(context, app.getPackageName());
//            for (ShortcutRecord shortcut : shortcutsList) {
//                String id = ShortcutEntry.generateShortcutId(shortcut.displayName);
//                excluded.add(id);
//            }
//            // Refresh shortcuts
//            if (!shortcutsList.isEmpty() && this.getShortcutsProvider() != null) {
//                this.getShortcutsProvider().reload();
//            }
//        }
//
//        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps-from-history", excluded).apply();
//        app.setExcludedFromHistory(true);
//    }

//    public void removeFromExcludedFromHistory(AppEntry app) {
//        // The set needs to be cloned and then edited,
//        // modifying in place is not supported by putStringSet()
//        Set<String> excluded = new HashSet<>(getExcludedFromHistory());
//        excluded.remove(app.id);
//
//        if (ShortcutUtil.areShortcutsEnabled(context)) {
//            // Add all shortcuts for given package name to being included in history
//            List<ShortcutRecord> shortcutsList = DBHelper.getShortcuts(context, app.getPackageName());
//            for (ShortcutRecord shortcut : shortcutsList) {
//                String id = ShortcutEntry.generateShortcutId(shortcut.displayName);
//                excluded.remove(id);
//            }
//        }
//
//        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps-from-history", excluded).apply();
//        app.setExcludedFromHistory(false);
//    }

//    public void addToExcluded(AppEntry app) {
//        // The set needs to be cloned and then edited,
//        // modifying in place is not supported by putStringSet()
//        Set<String> excluded = new HashSet<>(getExcluded());
//        excluded.add(app.getUserComponentName());
//        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", excluded).apply();
//        app.setExcluded(true);
//
//        // Ensure it's removed from favorites too
//        DataHandler dataHandler = TBApplication.getApplication(context).getDataHandler();
//        dataHandler.removeFromFavorites(app.id);
//
//        // Exclude shortcuts for this app
//        removeShortcuts(app.getPackageName());
//    }

//    public void removeFromExcluded(AppEntry app) {
//        // The set needs to be cloned and then edited,
//        // modifying in place is not supported by putStringSet()
//        Set<String> excluded = new HashSet<>(getExcluded());
//        excluded.remove(app.getUserComponentName());
//        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", excluded).apply();
//        app.setExcluded(false);
//
//        // Add shortcuts for this app
//        addShortcut(app.getPackageName());
//    }

    public void removeFromExcluded(String packageName) {
        Set<String> excluded = getExcluded();
        Set<String> newExcluded = new HashSet<>();
        for (String excludedItem : excluded) {
            if (!excludedItem.contains(packageName + "/")) {
                newExcluded.add(excludedItem);
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", newExcluded).apply();
    }

    public void removeFromExcluded(UserHandleCompat user) {
        // This is only intended for apps from foreign-profiles
        if (user.isCurrentUser()) {
            return;
        }

        Set<String> excluded = getExcluded();
        Set<String> newExcluded = new HashSet<>();
        for (String excludedItem : excluded) {
            if (!user.hasStringUserSuffix(excludedItem, '#')) {
                newExcluded.add(excludedItem);
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", newExcluded).apply();
    }

    /**
     * Return all applications (including excluded)
     *
     * @return pojos for all applications
     */
    @Nullable
    public List<AppEntry> getApplications() {
        AppProvider appProvider = getAppProvider();
        return appProvider != null ? appProvider.getAllApps() : null;
    }

    /**
     * Return all applications
     *
     * @return pojos for all applications
     */
    @Nullable
    public List<AppEntry> getApplicationsWithoutExcluded() {
        AppProvider appProvider = getAppProvider();
        return appProvider != null ? appProvider.getAllAppsWithoutExcluded() : null;
    }

    @Nullable
    public ContactsProvider getContactsProvider() {
        ProviderEntry entry = this.providers.get("contacts");
        return (entry != null) ? ((ContactsProvider) entry.provider) : null;
    }

    @Nullable
    public ShortcutsProvider getShortcutsProvider() {
        ProviderEntry entry = this.providers.get("shortcuts");
        return (entry != null) ? ((ShortcutsProvider) entry.provider) : null;
    }

    @Nullable
    public AppProvider getAppProvider() {
        ProviderEntry entry = this.providers.get("app");
        return (entry != null) ? ((AppProvider) entry.provider) : null;
    }

    @Nullable
    public FavProvider getFavProvider() {
        ProviderEntry entry = this.providers.get("favorites");
        return (entry != null) ? ((FavProvider) entry.provider) : null;
    }

    @Nullable
    public FilterProvider getFilterProvider() {
        ProviderEntry entry = this.providers.get("filters");
        return (entry != null) ? ((FilterProvider) entry.provider) : null;
    }

    @Nullable
    public ActionProvider getActionProvider() {
        ProviderEntry entry = this.providers.get("actions");
        return (entry != null) ? ((ActionProvider) entry.provider) : null;
    }

//    @Nullable
//    public SearchProvider getSearchProvider() {
//        ProviderEntry entry = this.providers.get("search");
//        return (entry != null) ? ((SearchProvider) entry.provider) : null;
//    }

    /**
     * Return a list of records that the user marked as favorite
     *
     * @return list of {@link FavRecord}
     */
    @NonNull
    public ArrayList<FavRecord> getFavorites() {
        return DBHelper.getFavorites(context);
    }

//    /**
//     * This method is used to set the specific position of an app in the fav array.
//     *
//     * @param context  The mainActivity context
//     * @param id       the app you want to set the position of
//     * @param position the new position of the fav
//     */
//    public void setFavoritePosition(TBLauncherActivity context, String id, int position) {
//        List<EntryItem> currentFavorites = getFavorites();
//        List<String> favAppsList = new ArrayList<>();
//
//        for (EntryItem pojo : currentFavorites) {
//            favAppsList.add(pojo.getHistoryId());
//        }
//
//        int currentPos = favAppsList.indexOf(id);
//        if (currentPos == -1) {
//            Log.e(TAG, "Couldn't find id in favAppsList");
//            return;
//        }
//        // Clamp the position so we don't just extend past the end of the array.
//        position = Math.min(position, favAppsList.size() - 1);
//
//        favAppsList.remove(currentPos);
//        favAppsList.add(position, id);
//
//        String newFavList = TextUtils.join(";", favAppsList);
//
//        PreferenceManager.getDefaultSharedPreferences(context).edit()
//                .putString("favorite-apps-list", newFavList + ";").apply();
//
//        //TODO: make this work
//        //context.onFavoriteChange();
//    }

    public void addToFavorites(EntryItem entry) {
        FavRecord record = new FavRecord();
        record.record = entry.id;
        record.position = "0";
        DBHelper.setFavorite(context, record);
        FavProvider favProvider = getFavProvider();
        if (favProvider != null)
            favProvider.reload(true);
    }

    public void removeFromFavorites(EntryItem entry) {
        FavRecord record = new FavRecord();
        record.record = entry.id;
        DBHelper.removeFavorite(context, record);
        FavProvider favProvider = getFavProvider();
        if (favProvider != null)
            favProvider.reload(true);
    }

    @SuppressWarnings("StringSplitter")
    public void removeFromFavorites(UserHandleCompat user) {
        // This is only intended for apps from foreign-profiles
        if (user.isCurrentUser()) {
            return;
        }

        String[] favAppList = PreferenceManager.getDefaultSharedPreferences(this.context)
                .getString("favorite-apps-list", "").split(";");

        StringBuilder favApps = new StringBuilder();
        for (String favAppID : favAppList) {
            if (!favAppID.startsWith("app://") || !user.hasStringUserSuffix(favAppID, '/')) {
                favApps.append(favAppID);
                favApps.append(";");
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this.context).edit()
                .putString("favorite-apps-list", favApps.toString()).apply();
    }

    /**
     * Insert specified ID (probably a pojo.id) into history
     *
     * @param id pojo.id of item to record
     */
    public void addToHistory(String id) {
        if (id.isEmpty()) {
            return;
        }

        boolean frozen = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("freeze-history", false);

        Set<String> excludedFromHistory = getExcludedFromHistory();

        if (!frozen && !excludedFromHistory.contains(id)) {
            DBHelper.insertHistory(this.context, currentQuery, id);
        }
    }

    @Nullable
    public EntryItem getPojo(@NonNull String id) {
        // Ask all providers if they know this id
        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider != null && entry.provider.mayFindById(id)) {
                return entry.provider.findById(id);
            }
        }

        return null;
    }

    public HashMap<String, AppRecord> getCachedApps() {
        return DBHelper.getAppsData(context);
    }

    public void updateAppCache(ArrayList<AppRecord> appRecords) {
        if (appRecords.size() > 0)
            DBHelper.insertOrUpdateApps(context, appRecords);
    }

    public void removeAppCache(ArrayList<AppRecord> appRecords) {
        if (appRecords.size() > 0)
            DBHelper.deleteApps(context, appRecords);
    }

    public void renameApp(String componentName, String newName) {
        DBHelper.setCustomAppName(context, componentName, newName);
    }

    public void renameStaticEntry(@NonNull String entryId, @Nullable String newName) {
        if (newName == null)
            DBHelper.removeCustomStaticEntryName(context, entryId);
        else
            DBHelper.setCustomStaticEntryName(context, entryId, newName);
    }

    public void removeRenameApp(String componentName, String defaultName) {
        DBHelper.removeCustomAppName(context, componentName, defaultName);
    }

    public AppRecord setCustomAppIcon(String componentName, Bitmap bitmap) {
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream(1024);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                stream = null;
            else {
                stream.flush();
                stream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to convert bitmap", e);
        }
        if (stream == null)
            return null;
        return DBHelper.setCustomAppIcon(context, componentName, stream.toByteArray());
    }

    public void setCustomStaticEntryIcon(String entryId, Bitmap bitmap) {
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream(1024);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                stream = null;
            else {
                stream.flush();
                stream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to convert bitmap", e);
        }
        if (stream != null)
            DBHelper.setCustomStaticEntryIcon(context, entryId, stream.toByteArray());
    }

    public Bitmap getCustomAppIcon(String componentName) {
        byte[] bytes = DBHelper.getCustomAppIcon(context, componentName);
        if (bytes == null)
            return null;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public AppRecord removeCustomAppIcon(String componentName) {
        return DBHelper.removeCustomAppIcon(context, componentName);
    }

    public void removeCustomStaticEntryIcon(String entryId) {
        DBHelper.removeCustomStaticEntryIcon(context, entryId);
    }

    public Bitmap getCustomStaticEntryIcon(StaticEntry staticEntry) {
        byte[] bytes = DBHelper.getCustomFavIcon(context, staticEntry.id);
        if (bytes == null)
            return null;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public void renameShortcut(ShortcutEntry shortcutEntry, String newName) {
        DBHelper.renameShortcut(context, shortcutEntry, newName);
    }

    public void onProviderRecreated(Provider<? extends EntryItem> provider) {
        mFullLoadOverSent = false;

        IntentFilter intentFilter = new IntentFilter(TBLauncherActivity.LOAD_OVER);
        this.context.registerReceiver(this, intentFilter);

        Intent i = new Intent(TBLauncherActivity.START_LOAD);
        this.context.sendBroadcast(i);

        // reload providers for the next steps
        for (int step : IProvider.LOAD_STEPS) {
            if (step <= provider.getLoadStep())
                continue;
            for (ProviderEntry entry : this.providers.values()) {
                if (entry.provider != null && step == entry.provider.getLoadStep())
                    entry.provider.setDirty();
            }
        }
    }

    public void reloadProviders() {
        mFullLoadOverSent = false;

        start = System.currentTimeMillis();

        IntentFilter intentFilter = new IntentFilter(TBLauncherActivity.LOAD_OVER);
        this.context.registerReceiver(this, intentFilter);

        Intent i = new Intent(TBLauncherActivity.START_LOAD);
        this.context.sendBroadcast(i);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (String providerName : PROVIDER_NAMES) {
            if (prefs.getBoolean("enable-" + providerName, true)) {
                connectToProvider(providerName, 0);
            }
        }

        for (int step : IProvider.LOAD_STEPS) {
            for (ProviderEntry entry : this.providers.values()) {
                if (entry.provider != null && step == entry.provider.getLoadStep())
                    entry.provider.reload(true);
            }
        }
    }

    public void checkServices() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (String providerName : PROVIDER_NAMES) {
            if (!providers.containsKey(providerName) && prefs.getBoolean("enable-" + providerName, true)) {
                reloadProviders();
                break;
            }
        }
    }

    public void setQuickList(Iterable<String> records) {
        ArrayList<FavRecord> oldFav = getFavorites();
        int pos = 1;
        for (String record : records) {
            // remove from oldFav the current record
            for (Iterator<FavRecord> iterator = oldFav.iterator(); iterator.hasNext(); ) {
                FavRecord favRecord = iterator.next();
                if (favRecord.record.equals(record))
                    iterator.remove();
            }
            String position = String.format("%08x", pos);
            if (!DBHelper.setQuickListPosition(context, record, position)) {
                FavRecord favRecord = new FavRecord();
                favRecord.record = record;
                favRecord.flags |= FavRecord.FLAG_SHOW_IN_QUICK_LIST;
                favRecord.position = position;
                DBHelper.setFavorite(context, favRecord);
            }
            pos += 11;
        }

        for (FavRecord favRecord : oldFav) {
            if (favRecord.isInQuickList()) {
                favRecord.flags &= ~FavRecord.FLAG_SHOW_IN_QUICK_LIST;
                DBHelper.setFavorite(context, favRecord);
            }
        }

        FavProvider provider = getFavProvider();
        if (provider != null)
            provider.reload(true);
    }

    public boolean fullLoadOverSent() {
        return mFullLoadOverSent;
    }

    static final class ProviderEntry {
        public IProvider provider = null;
        ServiceConnection connection = null;
    }
}
