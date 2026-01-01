package lucns.robot2wd.remote;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class SuspendedWindow extends RelativeLayout {

    public interface Callback {
        void onBackPressed();
    }

    private Callback callback;
    private final WindowManager windowManager;
    private final WindowManager.LayoutParams layoutParams;
    private boolean showing;
    private final ViewGroup root;

    private boolean lockHorizontally, lockVertically;

    public SuspendedWindow(Context context, boolean interceptBackPressed) {
        super(context);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.gravity = Gravity.CENTER;

        if (interceptBackPressed) {
            root = new FrameLayout(context) {
                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == MotionEvent.ACTION_UP) callback.onBackPressed();
                    return true;
                }
            };
            root.addView(this);
            setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        } else {
            root = new RelativeLayout(getContext());
            root.addView(this);
            setLayoutParams(new LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        }

        //root.setBackgroundResource(R.drawable.suspended_background);
        setOnTouchListener(new OnTouchListener() {

            int initialX;
            int initialY;
            float initialTouchX;
            float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                    case MotionEvent.ACTION_MOVE:
                        if (!lockHorizontally) layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        if (!lockVertically) layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        if (!lockHorizontally || !lockVertically) applyLayoutParameters();
                }
                return true;
            }
        });
    }

    public int getNavigationBarHeight() {
        return windowManager.getCurrentWindowMetrics().getWindowInsets().getInsets(WindowInsets.Type.navigationBars()).bottom;
    }

    public int getStatusBarHeight() {
        return windowManager.getCurrentWindowMetrics().getWindowInsets().getInsets(WindowInsets.Type.statusBars()).bottom;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void lock(boolean horizontal, boolean vertical) {
        lockHorizontally = horizontal;
        lockVertically = vertical;
    }

    public void setContentView(int layoutId) {
        addView(LayoutInflater.from(getContext()).inflate(layoutId, null, false));
    }

    public void addViewWithDefaultParameters(View view) {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(view, layoutParams);
    }

    public void show() {
        if (showing) return;
        showing = true;
        windowManager.addView(root, layoutParams);
    }

    public boolean isShowing() {
        return showing;
    }

    public void setSize(int width, int height) {
        layoutParams.width = width;
        layoutParams.height = height;
    }

    public int getXPosition() {
        return layoutParams.x;
    }

    public int getYPosition() {
        return layoutParams.y;
    }

    public void setXPosition(int x) {
        layoutParams.x = x;
    }

    public void setYPosition(int y) {
        layoutParams.y = y;
    }

    public void applyLayoutParameters() {
        windowManager.updateViewLayout(root, layoutParams);
    }

    public void setGravity(int gravity) {
        layoutParams.gravity = gravity;
    }

    public void hide() {
        if (!showing) return;
        showing = false;
        windowManager.removeView(root);
    }

    public int getColor(int colorId) {
        return getContext().getColor(colorId);
    }

    public String getString(int id) {
        return getContext().getString(id);
    }
}
