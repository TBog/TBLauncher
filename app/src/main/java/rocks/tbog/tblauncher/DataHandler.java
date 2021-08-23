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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import rocks.tbog.tblauncher.dataprovider.ActionProvider;
import rocks.tbog.tblauncher.dataprovider.AppCacheProvider;
import rocks.tbog.tblauncher.dataprovider.AppProvider;
import rocks.tbog.tblauncher.dataprovider.CalculatorProvider;
import rocks.tbog.tblauncher.dataprovider.ContactsProvider;
import rocks.tbog.tblauncher.dataprovider.FavProvider;
import rocks.tbog.tblauncher.dataprovider.FilterProvider;
import rocks.tbog.tblauncher.dataprovider.IProvider;
import rocks.tbog.tblauncher.dataprovider.Provider;
import rocks.tbog.tblauncher.dataprovider.QuickListProvider;
import rocks.tbog.tblauncher.dataprovider.SearchProvider;
import rocks.tbog.tblauncher.dataprovider.ShortcutsProvider;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
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
import rocks.tbog.tblauncher.utils.Timer;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class DataHandler extends BroadcastReceiver
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    final static private String TAG = "DataHandler";

    public static final ExecutorService EXECUTOR_PROVIDERS;

    static {
        /*
         corePoolSize: the number of threads to keep in the pool.
         maximumPoolSize: the maximum number of threads to allow in the pool.
         keepAliveTime: if the pool currently has more than corePoolSize threads, excess threads will be terminated if they have been idle for more than keepAliveTime.
         unit: the time unit for the keepAliveTime argument. Can be NANOSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS and DAYS.
         workQueue: the queue used for holding tasks before they are executed. Default choices are SynchronousQueue for multi-threaded pools and LinkedBlockingQueue for single-threaded pools.
        */
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                1, 1, 1, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        threadPoolExecutor.allowCoreThreadTimeOut(true);

        EXECUTOR_PROVIDERS = threadPoolExecutor;
    }

    /**
     * Package the providers reside in
     */
    final static private String PROVIDER_PREFIX = IProvider.class.getPackage().getName() + ".";
    /**
     * List all known providers
     */
    final static private List<String> PROVIDER_NAMES = Arrays.asList(
            "app"
            , "contacts"
            , "shortcuts"
    );

    final private Context context;
    private String currentQuery;
    private final Map<String, ProviderEntry> providers = new LinkedHashMap<>(); // preserve insert order
    private boolean mFullLoadOverSent = false;
    private final ArrayDeque<Runnable> mAfterLoadOverTasks = new ArrayDeque<>(2);
    private final Timer mTimer = new Timer();

    /**
     * Initialize all providers
     */
    public DataHandler(Context context) {
        // Make sure we are in the context of the main application
        // (otherwise we might receive an exception about broadcast listeners not being able
        //  to bind to services)
        this.context = context.getApplicationContext();

        mTimer.start();

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

        // Filters
        {
            ProviderEntry providerEntry = new ProviderEntry();
            providerEntry.provider = new FilterProvider(context);
            providers.put("filters", providerEntry);
        }

        // Actions
        {
            ProviderEntry providerEntry = new ProviderEntry();
            providerEntry.provider = new ActionProvider(context);
            providers.put("actions", providerEntry);
        }

        // Tag provider
        {
            ProviderEntry providerEntry = new ProviderEntry();
            providerEntry.provider = new TagsProvider(context);
            providers.put("tags", providerEntry);
        }

        // Favorites
        {
            ProviderEntry providerEntry = new ProviderEntry();
            providerEntry.provider = new FavProvider(context);
            providers.put("favorites", providerEntry);
        }

        // QuickList
        {
            ProviderEntry providerEntry = new ProviderEntry();
            providerEntry.provider = new QuickListProvider(context);
            providers.put("quickList", providerEntry);
        }

        // add providers that may be toggled by preferences
        toggleableProviders(prefs);
    }

    private void toggleableProviders(SharedPreferences prefs) {
        // Search engine provider,
        {
            String providerName = "search";
            if (prefs.getBoolean("enable-" + providerName, true)) {
                ProviderEntry providerEntry = new ProviderEntry();
                providerEntry.provider = new SearchProvider(context);
                providers.put(providerName, providerEntry);
            } else {
                providers.remove(providerName);
            }
        }

        // Calculator provider, may be toggled by preference
        {
            String providerName = "calculator";
            if (prefs.getBoolean("enable-" + providerName, true)) {
                ProviderEntry providerEntry = new ProviderEntry();
                providerEntry.provider = new CalculatorProvider();
                providers.put(providerName, providerEntry);
            } else {
                providers.remove(providerName);
            }
        }
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
                IProvider<?> provider = binder.getService();

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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (String providerName : PROVIDER_NAMES) {
            if (prefs.getBoolean("enable-" + providerName, true)) {
                if (!providers.containsKey(providerName))
                    return false;
            }
        }
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

        mTimer.stop();
        Log.v(TAG, "Time to load all providers: " + mTimer);

        mFullLoadOverSent = true;
        // run and remove tasks
        Runnable task;
        while (null != (task = mAfterLoadOverTasks.poll()))
            task.run();

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
            IProvider<?> provider = setEntry.getValue().provider;
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
            if (entry.provider == null)
                continue;

            List<? extends EntryItem> pojos = entry.provider.getPojos();
            if (pojos == null)
                continue;
            boolean accept = searcher.addResult(pojos.toArray(new EntryItem[0]));
            // if searcher will not accept any more results, exit
            if (!accept)
                break;
        }
    }

    @NonNull
    public static DBHelper.HistoryMode getHistoryMode(String historyMode) {
        switch (historyMode) {
            case "frecency":
                return DBHelper.HistoryMode.FRECENCY;
            case "frequency":
                return DBHelper.HistoryMode.FREQUENCY;
            case "adaptive":
                return DBHelper.HistoryMode.ADAPTIVE;
            default:
                return DBHelper.HistoryMode.RECENCY;
        }
    }

    /**
     * Return previously selected items.<br />
     * May return null if no items were ever selected (app first use)<br />
     * May return an empty set if the providers are not done building records,
     * in this case it is probably a good idea to call this function 500ms after
     *
     * @param itemCount          max number of items to retrieve, total number may be less (search or calls are not returned for instance)
     * @param historyMode        Recency vs Frecency vs Frequency vs Adaptive
     * @param sortHistory        Sort history entries alphabetically
     * @param itemsToExcludeById Items to exclude from history by their id
     * @return pojos in recent history
     */
    public List<EntryItem> getHistory(int itemCount, DBHelper.HistoryMode historyMode,
                                      boolean sortHistory, Set<String> itemsToExcludeById) {
        // Max sure that we get enough items, regardless of how many may be excluded
        int extendedItemCount = itemCount + itemsToExcludeById.size();

        // Read history
        List<ValuedHistoryRecord> ids = DBHelper.getHistory(context, extendedItemCount, historyMode);

        // Pre-allocate array slots that are likely to be used
        ArrayList<EntryItem> history = new ArrayList<>(ids.size());

        // Find associated items
        for (int i = 0; i < ids.size(); i++) {
            // Ask all providers if they know this id
            EntryItem pojo = getPojo(ids.get(i).record);

            if (pojo == null)
                continue;

            if (itemsToExcludeById.contains(pojo.id))
                continue;

            history.add(pojo);
        }

        // sort the list if needed
        if (sortHistory)
            Collections.sort(history, (a, b) -> a.getName().compareTo(b.getName()));

        // enforce item count after the sort operation
        if (history.size() > itemCount)
            history.subList(itemCount, history.size()).clear();

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
        List<ShortcutRecord> shortcutsList = DBHelper.getShortcutsNoIcons(context, packageName);
        for (ShortcutRecord shortcut : shortcutsList) {
            String id = ShortcutEntry.generateShortcutId(shortcut);
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

    public boolean addToHidden(AppEntry entry) {
        // if it's hidden it shouldn't be a favorite, right?
        removeFromFavorites(entry);
        return DBHelper.setAppHidden(context, entry.getUserComponentName());
    }

    public boolean removeFromHidden(AppEntry entry) {
        return DBHelper.removeAppHidden(context, entry.getUserComponentName());
    }

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
    public List<AppEntry> getApplicationsWithoutHidden() {
        AppProvider appProvider = getAppProvider();
        return appProvider != null ? appProvider.getAllAppsWithoutHidden() : null;
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

    @Nullable
    public TagsProvider getTagsProvider() {
        ProviderEntry entry = this.providers.get("tags");
        return (entry != null) ? ((TagsProvider) entry.provider) : null;
    }

    @Nullable
    public QuickListProvider getQuickListProvider() {
        ProviderEntry entry = this.providers.get("quickList");
        return (entry != null) ? ((QuickListProvider) entry.provider) : null;
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

    @NonNull
    public List<String> getTagList() {
        return DBHelper.loadTagList(context);
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
        if (DBHelper.removeFavorite(context, entry.id)) {
            FavProvider favProvider = getFavProvider();
            if (favProvider != null)
                favProvider.reload(true);
        }
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

    @NonNull
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

    public void setCachedAppIcon(String componentName, Bitmap bitmap) {
        byte[] array = Utilities.bitmapToByteArray(bitmap);
        if (array == null) {
            Log.e(TAG, "bitmapToByteArray failed for `" + componentName + "` with bitmap " + bitmap);
            return;
        }
        if (!DBHelper.setCachedAppIcon(context, componentName, array)) {
            Log.w(TAG, "setCachedAppIcon failed for `" + componentName + "` with bitmap " + bitmap);
        }
    }

    public AppRecord setCustomAppIcon(String componentName, Bitmap bitmap) {
        byte[] array = Utilities.bitmapToByteArray(bitmap);
        if (array == null) {
            Log.e(TAG, "bitmapToByteArray failed for `" + componentName + "` with bitmap " + bitmap);
            return null;
        }
        return DBHelper.setCustomAppIcon(context, componentName, array);
    }

    public void setCustomStaticEntryIcon(String entryId, Bitmap bitmap) {
        byte[] array = Utilities.bitmapToByteArray(bitmap);
        if (array != null) {
            DBHelper.setCustomStaticEntryIcon(context, entryId, array);
            // reload provider to make sure we're up to date
            FavProvider favProvider = getFavProvider();
            if (favProvider != null)
                favProvider.reload(true);
        }
    }

    public Bitmap getCachedAppIcon(String componentName) {
        byte[] bytes = DBHelper.getCachedAppIcon(context, componentName);
        if (bytes == null)
            return null;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
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

    public Bitmap getCustomShortcutIcon(ShortcutEntry shortcutEntry) {
        byte[] bytes = DBHelper.getCustomFavIcon(context, shortcutEntry.id);
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

        mTimer.start();

        IntentFilter intentFilter = new IntentFilter(TBLauncherActivity.LOAD_OVER);
        context.registerReceiver(this, intentFilter);

        Intent i = new Intent(TBLauncherActivity.START_LOAD);
        context.sendBroadcast(i);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        toggleableProviders(prefs);

        for (String providerName : PROVIDER_NAMES) {
            if (prefs.getBoolean("enable-" + providerName, true)) {
                connectToProvider(providerName, 0);
            }
        }

        for (int step : IProvider.LOAD_STEPS) {
            for (ProviderEntry entry : providers.values()) {
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

//    @NonNull
//    public List<? extends EntryItem> getQuickList() {
//        FavProvider favProvider = getFavProvider();
//        if (favProvider == null)
//            return Collections.emptyList();
//        return favProvider.getQuickList();
//    }

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
            if (!DBHelper.updateQuickListPosition(context, record, position)) {
                FavRecord favRecord = new FavRecord();
                favRecord.record = record;
                favRecord.addFlags(FavRecord.FLAG_SHOW_IN_QUICK_LIST);
                favRecord.position = position;
                DBHelper.setFavorite(context, favRecord);
            }
            pos += 11;
        }

        TagsProvider tagsProvider = getTagsProvider();

        for (FavRecord favRecord : oldFav) {
            if (favRecord.isInQuickList()) {
                favRecord.clearFlags(FavRecord.FLAG_SHOW_IN_QUICK_LIST);
                DBHelper.setFavorite(context, favRecord);
            }
            // keep custom icons
            if (!favRecord.hasCustomIcon()) {
                // don't keep tags as favorites
                if (tagsProvider != null && tagsProvider.mayFindById(favRecord.record)) {
                    DBHelper.removeFavorite(context, favRecord.record);
                }
            }
        }

        // refresh Favorites provider
        {
            IProvider<?> provider = getFavProvider();
            if (provider != null)
                provider.reload(true);
        }
        {
            IProvider<?> provider = getTagsProvider();
            if (provider != null)
                provider.reload(true);
        }
        {
            IProvider<?> provider = getQuickListProvider();
            if (provider != null)
                provider.reload(true);
        }
    }

    public boolean fullLoadOverSent() {
        return mFullLoadOverSent;
    }

    public void runAfterLoadOver(@NonNull Runnable task) {
        synchronized (this) {
            if (mFullLoadOverSent)
                task.run();
            else
                mAfterLoadOverTasks.add(task);
        }
    }

    static final class ProviderEntry {
        public IProvider<?> provider = null;
        ServiceConnection connection = null;
    }
}
