package rocks.tbog.tblauncher.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.Lifecycle;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TaskRunner {
    private final static Handler handler = new Handler(Looper.getMainLooper());
    private final static String TAG = "TaskRun";
//    private final Executor executor;

    public interface Callback<R> {
        @UiThread
        void onComplete(R result);
    }

    public interface AsyncRunnable {
        void run(@NonNull CancellableTask task);
    }

//    public TaskRunner() {
//        this(Executors.newSingleThreadExecutor());
//    }
//
//    public TaskRunner(@NonNull Executor executor) {
//        super();
//        this.executor = executor;
//    }

    @NonNull
    public static <R> CancellableTask newTask(@NonNull Lifecycle lifecycle, @NonNull AsyncRunnable worker, @NonNull AsyncRunnable main) {
        return new CancellableTask() {
            @Override
            public void run() {
                if (cancelled)
                    return;
                try {
                    worker.run(this);
                } catch (Exception e) {
                    Log.e(TAG, "worker " + worker.toString(), e);
                } finally {
                    handler.post(() -> {
                        if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                            main.run(this);
                    });
                }
            }
        };
    }

    @NonNull
    public static <R> CancellableTask newTask(@NonNull AsyncRunnable worker, @NonNull AsyncRunnable main) {
        return new CancellableTask() {
            @Override
            public void run() {
                if (cancelled)
                    return;
                try {
                    worker.run(this);
                } catch (Exception e) {
                    Log.e(TAG, "worker " + worker.toString(), e);
                } finally {
                    handler.post(() -> main.run(this));
                }
            }
        };
    }

    @NonNull
    public static <R> CancellableTask newTask(@NonNull Callable<R> callable, @NonNull Callback<R> callback) {
        return new CancellableTask() {
            @Override
            public void run() {
                if (cancelled)
                    return;
                R result = null;
                try {
                    result = callable.call();
                } catch (Exception e) {
                    Log.e(TAG, "callable " + callable.toString(), e);
                } finally {
                    final R finalResult = result;
                    handler.post(() -> {
                        if (cancelled)
                            return;
                        callback.onComplete(finalResult);
                    });
                }
            }
        };
    }

    public static <R> CancellableTask executeAsync(@NonNull Executor executor, @NonNull Callable<R> callable, @NonNull Callback<R> callback) {
        final CancellableTask task = newTask(callable, callback);
        executor.execute(task);
        return task;
    }

    public static <In, Out, T extends AsyncTask<In, Out>> void executeOnExecutor(@NonNull ExecutorService executor, @NonNull T task) {
        executeOnExecutor(executor, task, null);
    }

    public static <In, Out, T extends AsyncTask<In, Out>> void executeOnExecutor(@NonNull ExecutorService executor, @NonNull T task, @Nullable In input) {
        task.onPreExecute(
                executor.submit(() -> {
                    Out output;
                    try {
                        output = task.doInBackground(input);
                    } catch (Throwable th) {
                        Log.w(TAG, task.getClass().toString(), th);
                        task.taskFuture.cancel(false);
                        throw th;
                    } finally {
                        handler.post(task::onPostExecute);
                    }
                    return output;
                }));
    }

    public static class CancellableTask implements Runnable {
        protected volatile boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        @WorkerThread
        @Override
        public void run() {

        }
    }

    public static abstract class AsyncTask<In, Out> {
        protected Future<Out> taskFuture = null;

        public boolean cancel(boolean mayInterruptIfRunning) {
            return taskFuture.cancel(mayInterruptIfRunning);
        }

        public boolean isCancelled() {
            return taskFuture.isCancelled();
        }

        @CallSuper
        protected void onPreExecute(Future<Out> taskFuture) {
            this.taskFuture = taskFuture;
            onPreExecute();
        }

        protected void onPreExecute() {
        }

        protected abstract Out doInBackground(In input);

        @CallSuper
        protected void onPostExecute() {
            Out output = null;
            try {
                output = taskFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "get future", e);
            } catch (CancellationException e) {
                output = null;
            } finally {
                onPostExecute(output);
            }
        }

        protected void onPostExecute(Out output) {
        }
    }
}
