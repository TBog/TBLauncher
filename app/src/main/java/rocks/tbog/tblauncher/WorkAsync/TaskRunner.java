package rocks.tbog.tblauncher.WorkAsync;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.concurrent.ExecutorService;

public class TaskRunner {
    private final static Handler handler = new Handler(Looper.getMainLooper());

    public interface AsyncRunnable {
        void run(@NonNull RunnableTask task);
    }

    public static boolean runOnUiThread(Runnable runnable) {
        return handler.post(runnable);
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

}
