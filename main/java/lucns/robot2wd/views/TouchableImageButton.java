package lucns.robot2wd.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageButton;

public class TouchableImageButton extends ImageButton {

    public interface TouchCallback {
        void onTouch(TouchableImageButton view, boolean touched);
    }

    private boolean isTouched;
    private TouchCallback touchCallback;

    public TouchableImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isTouched() {
        return isTouched;
    }

    public void setTouchCallback(TouchCallback touchCallback) {
        this.touchCallback = touchCallback;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouched = true;
                setPressed(true);
                if (touchCallback != null) touchCallback.onTouch(this, true);
                break;
            case MotionEvent.ACTION_UP:
                isTouched = false;
                setPressed(false);
                if (touchCallback != null) touchCallback.onTouch(this, false);
                break;
        }
        invalidate();
        return true;
    }
}
