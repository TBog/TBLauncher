package rocks.tbog.tblauncher.WorkAsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public final class RunnableTask extends FutureTask<RunnableTask> {
    private TaskRunner.AsyncRunnable whenDone = null;
    private Lifecycle lifecycle = null;

    public void cancel() {
        cancel(false);
    }

    protected RunnableTask(@NonNull TaskRunner.AsyncRunnable worker, @Nullable TaskRunner.AsyncRunnable main, @Nullable Lifecycle lifecycle) {
        this(new BackgroundWorker(worker));
        whenDone = main;
        this.lifecycle = lifecycle;
    }

    protected RunnableTask(@NonNull TaskRunner.AsyncRunnable worker, @Nullable TaskRunner.AsyncRunnable main) {
        this(worker, main, null);
    }

    private RunnableTask(@NonNull BackgroundWorker background) {
        super(background);
        background.task = this;
    }

    @Override
    protected void done() {
        if (whenDone != null) {
            TaskRunner.runOnUiThread(() -> {
                if (lifecycle == null || lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                    whenDone.run(this);
            });
        }
    }

    private static class BackgroundWorker implements Callable<RunnableTask> {
        private RunnableTask task = null;
        private final TaskRunner.AsyncRunnable worker;

        private BackgroundWorker(TaskRunner.AsyncRunnable worker) {
            this.worker = worker;
        }

        @Override
        public RunnableTask call() {
            worker.run(task);
            return task;
        }
    }
}
