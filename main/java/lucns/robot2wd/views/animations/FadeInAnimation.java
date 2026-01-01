package lucns.robot2wd.views.animations;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

import lucns.robot2wd.views.animations.base.SimultaneousAnimation;

public class FadeInAnimation {

    private final AnimationController controller;

    public FadeInAnimation(View view, long duration, AnimationController.Callback callback) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        SimultaneousAnimation out = new SimultaneousAnimation();
        out.add(new Animation(view, View.ALPHA, 0f, 1f, duration, new DecelerateInterpolator()));
        out.add(new Animation(view, View.SCALE_X, 0.5f, 1f, duration, new DecelerateInterpolator()));
        out.add(new Animation(view, View.SCALE_Y, 0.5f, 1f, duration, new DecelerateInterpolator()));
        controller = new AnimationController();
        controller.setCallback(callback);
        controller.add(out);
    }

    public void start() {
        controller.start();
    }

    public void stop() {
        controller.stop();
    }
}
