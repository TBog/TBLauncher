package rocks.tbog.tblauncher.WorkAsync;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public abstract class AsyncTask<In, Out> extends FutureTask<Out> {
    private static final String TAG = "AsyncT";
    In input = null;

    protected AsyncTask() {
        this(new BackgroundWorker<>());
    }

    private AsyncTask(BackgroundWorker<In, Out> worker) {
        super(worker);
        worker.task = this;
    }

    @MainThread
    protected void onPreExecute() {
    }

    @WorkerThread
    protected abstract Out doInBackground(In input);

    @Override
    protected void done() {
        TaskRunner.runOnUiThread(() -> {
            if (isCancelled())
                onCancelled();
            else {
                Out result = null;
                try {
                    result = get();
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "AsyncTask " + AsyncTask.this, e);
                }
                onPostExecute(result);
            }
        });
    }

    @MainThread
    protected void onPostExecute(Out output) {
    }

    @MainThread
    protected void onCancelled() {
    }

    private static class BackgroundWorker<In, Out> implements Callable<Out> {
        private AsyncTask<In, Out> task = null;

        @Override
        public Out call() {
            Out output = task.doInBackground(task.input);
            task.input = null;
            return output;
        }
    }
}
