package lucns.robot2wd.views.animations.base;

import java.util.LinkedList;
import java.util.List;

public class SequentialAnimation extends BaseAnimation {

    private final List<BaseAnimation> list;
    private int index;

    public SequentialAnimation() {
        list = new LinkedList<>();
    }

    public void add(BaseAnimation animation) {
        list.add(animation);
    }

    public void reset() {
        for (BaseAnimation s : list) s.reset();
    }

    @Override
    public void start() {
        index = 0;
        play();
    }

    @Override
    public void stop() {
        for (BaseAnimation s : list) s.stop();
    }

    @Override
    public long getDuration() {
        long l = 0;
        for (BaseAnimation s : list) l += s.getDuration();
        return l;
    }

    private void play() {
        BaseAnimation sa = list.get(index);
        sa.setCallback(new Callback() {
            @Override
            public void onFinish() {
                index++;
                if (index == list.size()) {
                    if (callback != null) callback.onFinish();
                    return;
                }
                play();
            }
        });
        sa.start();
    }
}
