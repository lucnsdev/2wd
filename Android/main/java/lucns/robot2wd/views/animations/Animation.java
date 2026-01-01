package lucns.robot2wd.views.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.os.Handler;
import android.os.Looper;
import android.util.Property;
import android.view.View;
import android.view.animation.LinearInterpolator;

import lucns.robot2wd.views.animations.base.BaseAnimation;

public class Animation extends BaseAnimation {

    private ObjectAnimator animator;

    private View view;
    private float initialValue;
    private Property<View, Float> property;

    private long delay;
    private Handler handler;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (callback != null) callback.onFinish();
        }
    };

    public Animation(long delay) {
        this.delay = delay;
        handler = new Handler(Looper.getMainLooper());
    }

    public Animation(View view, Property<View, Float> property, float from, float to, long duration, TimeInterpolator interpolator) {
        this.view = view;
        this.property = property;
        this.initialValue = from;
        animator = ObjectAnimator.ofFloat(view, property, from, to).setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (callback != null) callback.onFinish();
            }
        });
    }

    public Animation(View view, Property<View, Float> property, float from, float to, long duration) {
        this.view = view;
        this.property = property;
        this.initialValue = from;
        if (property == View.ROTATION) {
            animator = ObjectAnimator.ofFloat(view, "rotation", from, to).setDuration(duration);
        } else {
            animator = ObjectAnimator.ofFloat(view, property, from, to).setDuration(duration);
        }
        animator.setInterpolator(new LinearInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (callback != null) callback.onFinish();
            }
        });
    }

    @Override
    public void reset() {
        if (view == null) return;
        ObjectAnimator.ofFloat(view, property, initialValue, initialValue).setDuration(0).start();
    }

    @Override
    public void start() {
        if (delay > 0) handler.postDelayed(runnable, delay);
        else animator.start();
    }

    @Override
    public void stop() {
        if (delay > 0) handler.removeCallbacks(runnable);
        else animator.cancel();
    }

    @Override
    public long getDuration() {
        if (delay > 0) return delay;
        return animator.getDuration();
    }

    public void setInterpolator(TimeInterpolator interpolator) {
        animator.setInterpolator(interpolator);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }
}
