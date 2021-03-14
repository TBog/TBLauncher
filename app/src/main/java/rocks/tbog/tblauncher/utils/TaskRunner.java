package rocks.tbog.tblauncher.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.Lifecycle;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

public class TaskRunner {
    private final static Handler handler = new Handler(Looper.getMainLooper());
    private final static String TAG = "TaskRun";

    public interface AsyncRunnable {
        void run(@NonNull RunnableTask task);
    }

    @NonNull
    public static RunnableTask newTask(@NonNull Lifecycle lifecycle, @NonNull AsyncRunnable worker, @NonNull AsyncRunnable main) {
        return new RunnableTask(worker, main, lifecycle);
    }

    @NonNull
    public static RunnableTask newTask(@NonNull AsyncRunnable worker, @NonNull AsyncRunnable main) {
        return new RunnableTask(worker, main);
    }

    @MainThread
    public static <In, Out, T extends AsyncTask<In, Out>> void executeOnExecutor(@NonNull ExecutorService executor, @NonNull T task) {
        executeOnExecutor(executor, task, null);
    }

    @MainThread
    public static <In, Out> void executeOnExecutor(@NonNull ExecutorService executor, @NonNull AsyncTask<In, Out> task, @Nullable In input) {
        task.onPreExecute();
        task.input = input;
        executor.submit(task);
    }

    public static abstract class AsyncTask<In, Out> extends FutureTask<Out> {
        private In input = null;

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
            handler.post(() -> {
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

    public static class RunnableTask extends FutureTask<RunnableTask> {
        private AsyncRunnable whenDone = null;
        private Lifecycle lifecycle = null;

        public void cancel() {
            cancel(false);
        }

        public RunnableTask(@NonNull AsyncRunnable worker, @Nullable AsyncRunnable main, @Nullable Lifecycle lifecycle) {
            this(new BackgroundWorker(worker));
            whenDone = main;
            this.lifecycle = lifecycle;
        }

        public RunnableTask(@NonNull AsyncRunnable worker, @Nullable AsyncRunnable main) {
            this(worker, main, null);
        }

        private RunnableTask(@NonNull BackgroundWorker background) {
            super(background);
            background.task = this;
        }

        @Override
        protected void done() {
            if (whenDone != null) {
                handler.post(() -> {
                    if (lifecycle == null || lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                        whenDone.run(this);
                });
            }
        }

        private static class BackgroundWorker implements Callable<RunnableTask> {
            private RunnableTask task = null;
            private final AsyncRunnable worker;

            private BackgroundWorker(AsyncRunnable worker) {
                this.worker = worker;
            }

            @Override
            public RunnableTask call() {
                worker.run(task);
                return task;
            }
        }
    }
}
