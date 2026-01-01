package lucns.robot2wd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import lucns.robot2wd.R;

public class TriangleView extends View {

    public interface TouchCallback {
        void onTouch(TriangleView view, boolean touched);
    }

    public enum Positions {TOP, BOTTOM, LEFT, RIGHT}

    private Paint paint;
    private Positions position;
    private TouchCallback touchCallback;
    private boolean isTouched;

    public TriangleView(Context context) {
        super(context);
        if (!isInEditMode()) init();
    }

    public TriangleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) init();
    }

    public TriangleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) init();
    }

    public TriangleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (!isInEditMode()) init();
    }

    private void init() {
        position = Positions.TOP;
        paint = new Paint();
        setAlpha(0.5f);
    }

    public boolean isTouched() {
        return isTouched;
    }

    public void setTouchCallback(TouchCallback touchCallback) {
        this.touchCallback = touchCallback;
    }

    public void setPosition(Positions position) {
        this.position = position;
        invalidate();
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        super.setEnabled(isEnabled);
        setAlpha(isEnabled ? 0.5f : 0.3f);
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(isPressed() ? getContext().getColor(R.color.main) : Color.WHITE);
        drawTriangle(canvas);
    }

    private void drawTriangle(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        Point p1, p2, p3;
        switch (position) {
            case BOTTOM:
                p1 = new Point(width / 2, height);
                p2 = new Point(0, 0);
                p3 = new Point(width, 0);
                break;
            case LEFT:
                p1 = new Point(0, height / 2);
                p2 = new Point(width, 0);
                p3 = new Point(width, height);
                break;
            case RIGHT:
                p1 = new Point(width, height / 2);
                p2 = new Point(0, 0);
                p3 = new Point(0, height);
                break;
            default:
                p1 = new Point(width / 2, 0);
                p2 = new Point(width, height);
                p3 = new Point(0, height);
                break;
        }

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.close();

        canvas.drawPath(path, paint);
    }
}
