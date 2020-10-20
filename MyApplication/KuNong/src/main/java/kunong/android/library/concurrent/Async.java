package kunong.android.library.concurrent;

import android.os.Handler;
import android.os.Looper;

import java.util.LinkedList;

/**
 * Created by Macmini on 12/2/14 AD.
 */
public final class Async {

    private Async() {
    }

    public static Task newInstance() {
        return new Task();
    }

    public static Task background(Runnable runnable) {
        return newInstance().background(runnable);
    }

    public static Task main(Runnable runnable) {
        return main(runnable, 0);
    }

    public static Task main(Runnable runnable, long delay) {
        return newInstance().main(runnable, delay);
    }

    public static Task sync(Runnable runnable) {
        return sync(runnable, 0);
    }

    public static Task sync(Runnable runnable, long delay) {
        return newInstance().sync(runnable, delay);
    }

    protected enum QueueType {
        MAIN, BACKGROUND
    }

    public final static class Task {
        LinkedList<TaskQueue> queues = new LinkedList<>();

        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private boolean mHasRunningTask;
        private boolean mIsLocked;

        protected Task() {
        }

        public Task background(Runnable runnable) {
            this.queues.add(new TaskQueue(QueueType.BACKGROUND, runnable));

            onTaskComplete();

            return this;
        }

        public Task main(Runnable runnable) {
            return main(runnable, 0);
        }

        public Task main(Runnable runnable, long delay) {
            return sync(runnable, delay, false);
        }

        public Task sync(Runnable runnable) {
            return sync(runnable, 0);
        }

        public Task sync(Runnable runnable, long delay) {
            return sync(runnable, delay, true);
        }

        private Task sync(Runnable runnable, long delay, boolean isLocked) {
            this.queues.add(new TaskQueue(QueueType.MAIN, runnable, delay, isLocked));

            onTaskComplete();

            return this;
        }


        public void release() {
            if (mIsLocked) {
                mIsLocked = false;

                onTaskComplete();
            }
        }

        private void onTaskComplete() {
            if (!mIsLocked && !mHasRunningTask && queues.size() > 0) {
                TaskQueue taskQueue = queues.poll();

                runTask(taskQueue);
            }
        }

        private void runTask(TaskQueue taskQueue) {
            mHasRunningTask = true;

            if (taskQueue.isLocked) {
                mIsLocked = true;
            }

            if (taskQueue.type == QueueType.BACKGROUND) {
                runBackground(taskQueue.runnable);
            } else {
                runMain(taskQueue.runnable, taskQueue.delay);
            }
        }

        private void runBackground(Runnable runnable) {
            ThreadQueue threadQueue = new ThreadQueue();

            threadQueue.add(new QThread<Object, Object>() {
                @Override
                public Object run(Object... objects) {
                    runnable.run();

                    return null;
                }

                @Override
                public void onCompleted(Object o) {
                    mHasRunningTask = false;

                    onTaskComplete();
                }
            });

            threadQueue.start();
        }

        private void runMain(Runnable runnable, long delay) {
            mHandler.postDelayed(() -> {
                runnable.run();

                mHasRunningTask = false;

                onTaskComplete();
            }, delay);
        }

        public int size() {
            return this.queues.size();
        }
    }

    public final static class TaskQueue {
        QueueType type;
        Runnable runnable;
        long delay;
        boolean isLocked;

        protected TaskQueue(QueueType type, Runnable runnable) {
            this(type, runnable, 0);
        }

        protected TaskQueue(QueueType type, Runnable runnable, long delay) {
            this(type, runnable, delay, false);
        }

        protected TaskQueue(QueueType type, Runnable runnable, long delay, boolean isLocked) {
            this.type = type;
            this.runnable = runnable;
            this.delay = delay;
            this.isLocked = isLocked;
        }
    }
}
