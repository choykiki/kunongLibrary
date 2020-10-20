package kunong.android.library.concurrent;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class ThreadQueue {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<QThread<?, ?>> qThreadList = new ArrayList<>();
    private ThreadQueueListener mListener;

    public void add(QThread<?, ?> qThread) {
        qThreadList.add(qThread);
    }

    public void start() {
        new Thread() {

            @Override
            public void run() {
                while (qThreadList.size() > 0) {
                    // Pop QThread;
                    QThread<?, ?> qThread = qThreadList.remove(0);

                    // Execute process in background.
                    qThread.execute();

                    // Execute completed process in ui thread.
                    mHandler.post(qThread);
                }

                // Callback after completed all threads.
                if (mListener != null) {
                    mHandler.post(mListener::onCompleted);
                }
            }

        }.start();
    }

    public void setListener(ThreadQueueListener listener) {
        mListener = listener;
    }

    public interface ThreadQueueListener {
        void onCompleted();
    }
}
