package lucns.robot2wd.views.animations;

import lucns.robot2wd.views.animations.base.BaseAnimation;
import lucns.robot2wd.views.animations.base.SequentialAnimation;
import lucns.robot2wd.views.animations.base.SimultaneousAnimation;

import java.util.LinkedList;
import java.util.List;

public class AnimationController {

    public interface Callback {
        void onFinish();
    }

    public Callback callback;
    public List<BaseAnimation> list;
    private BaseAnimation biggerDuration;

    public AnimationController() {
        list = new LinkedList<>();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void add(SequentialAnimation sequentialAnimation) {
        list.add(sequentialAnimation);
        resetCallback();
    }

    public void add(SimultaneousAnimation simultaneousAnimation) {
        list.add(simultaneousAnimation);
        resetCallback();
    }

    private void resetCallback() {
        biggerDuration = null;
        for (BaseAnimation b : list) {
            b.setCallback(null);
            if (biggerDuration == null) biggerDuration = b;
            else if (biggerDuration.getDuration() < b.getDuration()) biggerDuration = b;
        }
        biggerDuration.setCallback(new BaseAnimation.Callback() {
            @Override
            public void onFinish() {
                if (callback != null) callback.onFinish();
            }
        });
    }

    public void reset() {
        for (BaseAnimation b : list) {
            if (b instanceof SequentialAnimation) ((SequentialAnimation) b).reset();
        }
    }

    public void start() {
        for (BaseAnimation b : list) {
            b.start();
        }
    }

    public void stop() {
        for (BaseAnimation b : list) {
            b.stop();
        }
    }

    public long getDuration() {
        return biggerDuration.getDuration();
    }
}
