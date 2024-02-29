package rocks.tbog.tblauncher;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.ISearchActivity;

public class SearchActivity implements ISearchActivity {

    private static final String TAG = SearchActivity.class.getSimpleName();
    final WeakReference<TBLauncherActivity> mTBLauncherActivityWeakReference;

    public SearchActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivityWeakReference = new WeakReference<>(tbLauncherActivity);
    }

    @Override
    public void displayLoader(boolean running) {
        var activity = mTBLauncherActivityWeakReference.get();
        ImageView launcherButton = activity != null ? activity.getLauncherButton() : null;
        if (launcherButton == null)
            return;

        Drawable loadingDrawable = launcherButton.getDrawable();
        if (loadingDrawable instanceof Animatable) {
            if (running)
                ((Animatable) loadingDrawable).start();
            else
                ((Animatable) loadingDrawable).stop();
        }
    }

    @NonNull
    @Override
    public Context getContext() {
        var activity = mTBLauncherActivityWeakReference.get();
        if (activity == null)
            throw new IllegalStateException("SearchActivity activity==null");
        return activity;
    }

    @Override
    public void resetTask() {
        TBApplication.resetTask(getContext());
    }

    @Override
    public void clearAdapter() {
        var activity = mTBLauncherActivityWeakReference.get();
        if (activity != null) {
            activity.clearAdapter();
        }
    }

    @Override
    public void updateAdapter(@NonNull List<? extends EntryItem> results, boolean isRefresh) {
        Log.d(TAG, "updateAdapter " + results.size() + " result(s); isRefresh=" + isRefresh);
        var activity = mTBLauncherActivityWeakReference.get();
        if (activity == null)
            return;

        if (!activity.behaviour.isFragmentDialogVisible()) {
            LauncherState state = TBApplication.state();
            if (!state.isResultListVisible() && state.getDesktop() == LauncherState.Desktop.SEARCH)
                activity.showResultList(false);
        }
        activity.getResultAdapter().updateResults(results);

        if (!isRefresh) {
            // Make sure the first item is visible when we search
            activity.getResultList().scrollToFirstItem();
        }

        activity.quickList.adapterUpdated();
        //mClearButton.setVisibility(View.VISIBLE);
        //mMenuButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void removeResult(@NonNull EntryItem result) {
        var activity = mTBLauncherActivityWeakReference.get();
        if (activity != null) {
            activity.getResultAdapter().removeResult(result);
        }
    }

    @Override
    public void filterResults(String text) {
        var activity = mTBLauncherActivityWeakReference.get();
        if (activity != null) {
            activity.getResultAdapter().getFilter().filter(text);
        }
    }
}
