package lucns.robot2wd.activities;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class VelocityProvider {

    public interface Callback {
        void onValueChanged(int velocity);
    }

    private final Callback callback;

    private Thread thread;
    private int velocity;
    private boolean isRunning;
    private final long timeInterval;
    private final int minimumVelocity, maximumVelocity, increment;

    public VelocityProvider(long timeInterval, int minimumVelocity, int maximumVelocity, int increment, Callback callback) {
        this.timeInterval = timeInterval;
        this.maximumVelocity = maximumVelocity;
        this.minimumVelocity = minimumVelocity;
        this.increment = increment;
        this.callback = callback;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        velocity = minimumVelocity;
        callback.onValueChanged(velocity);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                long time2;
                while (!thread.isInterrupted()) {
                    time2 = System.currentTimeMillis();
                    if (time2 - time > timeInterval) {
                        time = time2;
                        velocity += increment;
                        if (velocity > maximumVelocity) {
                            velocity = maximumVelocity;
                        }
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onValueChanged(velocity);
                            }
                        });
                        if (velocity == maximumVelocity) break;
                    }
                }
            }
        });
        thread.start();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;
        if (thread != null && !thread.isInterrupted()) thread.interrupt();
        callback.onValueChanged(0);
    }
}
