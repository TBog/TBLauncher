package rocks.tbog.tblauncher.result;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import java.lang.ref.WeakReference;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.WorkAsync.AsyncTask;
import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.Utilities;

public abstract class AsyncSetEntryDrawable<Entry extends EntryItem> extends AsyncTask<Void, Drawable> {
    private static final String TAG = "AyncSED";
    protected final String cacheId;
    private final WeakReference<ImageView> weakImage;
    protected int drawFlags;
    protected Entry entryItem;

    public AsyncSetEntryDrawable(@NonNull ImageView image, int drawFlags, @NonNull Entry entryItem) {
        super();
        cacheId = entryItem.getIconCacheId();

        Object tag_cacheId = image.getTag(R.id.tag_cacheId);
        Object tag_iconTask = image.getTag(R.id.tag_iconTask);

        image.setTag(R.id.tag_cacheId, cacheId);
        image.setTag(R.id.tag_iconTask, this);

        boolean keepIcon = false;
        if (tag_iconTask instanceof AsyncSetEntryDrawable) {
            AsyncSetEntryDrawable<?> task = (AsyncSetEntryDrawable<?>) tag_iconTask;
            task.cancel(false);
            // if the old task was loading the same entry we can keep the icon while we refresh it
            keepIcon = entryItem.equals(task.entryItem);
        } else if (tag_cacheId instanceof String) {
            // if the tag equals cacheId then we can keep the icon while we refresh it
            keepIcon = tag_cacheId.equals(cacheId);
        }
        Log.i(TAG, "start task=" + Integer.toHexString(hashCode()) +
            " view=" + Integer.toHexString(image.hashCode()) +
            " tag_iconTask=" + (tag_iconTask != null ? Integer.toHexString(tag_iconTask.hashCode()) : "null") +
            " entry=`" + entryItem.getName() + "`" +
            " keepIcon=" + keepIcon +
            " tag_cacheId=" + tag_cacheId +
            " cacheId=" + cacheId);
        if (!keepIcon) {
            ResultViewHelper.setLoadingIcon(image);
        }
        this.weakImage = new WeakReference<>(image);
        this.drawFlags = drawFlags;
        this.entryItem = entryItem;
    }

    @Nullable
    public ImageView getImageView() {
        ImageView imageView = weakImage.get();
        // make sure we have a valid activity
        Activity act = Utilities.getActivity(imageView);
        if (act == null)
            return null;
        return imageView;
    }

    @Override
    protected Drawable doInBackground(Void param) {
        ImageView image = getImageView();
        if (isCancelled() || image == null) {
            weakImage.clear();
            return null;
        }
        Context ctx = image.getContext();
        return getDrawable(ctx);
    }

    @WorkerThread
    protected abstract Drawable getDrawable(Context context);

    @UiThread
    protected void setDrawable(ImageView image, Drawable drawable) {
        image.setImageDrawable(drawable);
        // async task finished, set icon task to null
        image.setTag(R.id.tag_iconTask, null);
        // start animation if it's possible
        Utilities.startAnimatable(image);
    }

    @Override
    protected void onPostExecute(Drawable drawable) {
        ImageView image = getImageView();
        if (image == null || drawable == null) {
            Log.i(TAG, "end task=" + Integer.toHexString(hashCode()) +
                " view=" + (image == null ? "null" : Integer.toHexString(image.hashCode())) +
                " drawable=" + drawable +
                " cacheId=`" + cacheId + "`");
            weakImage.clear();
            return;
        }
        Object tag_cacheId = image.getTag(R.id.tag_cacheId);
        Object tag_iconTask = image.getTag(R.id.tag_iconTask);

        if (cacheId != null && !Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_NO_CACHE))
            TBApplication.drawableCache(image.getContext()).cacheDrawable(cacheId, drawable);

        Log.i(TAG, "end task=" + Integer.toHexString(hashCode()) +
            " view=" + Integer.toHexString(image.hashCode()) +
            " tag_iconTask=" + (tag_iconTask != null ? Integer.toHexString(tag_iconTask.hashCode()) : "null") +
            " cacheId=`" + cacheId + "`");
        if (tag_iconTask instanceof AsyncSetEntryDrawable) {
            AsyncSetEntryDrawable<?> task = (AsyncSetEntryDrawable<?>) tag_iconTask;
            if (!entryItem.equals(task.entryItem)) {
                Log.d(TAG, "[task] skip reason: `" + entryItem.getName() + "` \u2260 `" + task.entryItem.getName() + "`");
                weakImage.clear();
                return;
            }
        } else {
            Log.d(TAG, "[task] skip reason: tag_iconTask=null entry=`" + entryItem.getName() + "`");
            weakImage.clear();
            return;
        }
        // if the cacheId changed, skip
        if (!tag_cacheId.equals(cacheId)) {
            Log.d(TAG, "[cacheId] skip reason: `" + tag_cacheId + "` \u2260 `" + cacheId + "`");
            weakImage.clear();
            return;
        }
        setDrawable(image, drawable);
    }

    @Override
    protected void onCancelled() {
        ImageView image = getImageView();
        Log.i(TAG, "cancelled task=" + Integer.toHexString(hashCode()) + " view=" + (image != null ? Integer.toHexString(image.hashCode()) : "null"));
    }

    public void execute() {
        TaskRunner.executeOnExecutor(ResultViewHelper.EXECUTOR_LOAD_ICON, this);
    }
}
