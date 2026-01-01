package lucns.robot2wd.views.animations.base;

public abstract class BaseAnimation {

    public interface Callback {
        void onFinish();
    }

    protected Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public abstract void reset();

    public abstract void start();

    public abstract void stop();

    public abstract long getDuration();
}
