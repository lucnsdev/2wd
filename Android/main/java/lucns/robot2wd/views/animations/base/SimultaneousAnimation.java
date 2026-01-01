package lucns.robot2wd.views.animations.base;

import lucns.robot2wd.views.animations.Animation;

import java.util.ArrayList;
import java.util.List;

public class SimultaneousAnimation extends BaseAnimation {

    private final List<BaseAnimation> list;
    private BaseAnimation biggerDuration;

    public SimultaneousAnimation() {
        list = new ArrayList<>();
    }

    public void add(BaseAnimation animation) {
        list.add(animation);
        for (BaseAnimation s : list) {
            s.setCallback(null);
            if (biggerDuration == null) biggerDuration = s;
            else if (biggerDuration.getDuration() < s.getDuration()) biggerDuration = s;
        }
        biggerDuration.setCallback(new Animation.Callback() {
            @Override
            public void onFinish() {
                if (callback != null) callback.onFinish();
            }
        });
    }

    @Override
    public void reset() {
    }

    @Override
    public void start() {
        for (BaseAnimation s : list) s.start();
    }

    @Override
    public void stop() {
        for (BaseAnimation s : list) s.stop();
    }

    @Override
    public long getDuration() {
        return biggerDuration.getDuration();
    }
}
