package lucns.robot2wd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import lucns.robot2wd.R;

public class VerticalSeekBar extends SeekBar {

    private final float thumbSize, trackWidth, circleWidth;
    private final Paint paintTrack, paintCircle;

    private OnSeekBarChangeListener listener;
    private boolean invertVertically;
    private int progress, max;
    Paint p = new Paint();

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        thumbSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 24, getResources().getDisplayMetrics());
        trackWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 3, getResources().getDisplayMetrics());
        circleWidth = trackWidth * 2;
        paintTrack = new Paint();
        paintTrack.setStrokeWidth(trackWidth);
        paintTrack.setStyle(Paint.Style.STROKE);

        paintCircle = new Paint();
        paintCircle.setStrokeWidth(circleWidth);
        paintCircle.setStyle(Paint.Style.STROKE);
        paintCircle.setColor(context.getColor(R.color.main));

        max = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "max", 100);
        progress = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "progress", 0);
        p.setColor(Color.BLACK);
    }

    public void isInvertVertically(boolean invertVertically) {
        this.invertVertically = invertVertically;
        invalidate();
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void setProgress(int progress) {
        this.progress = progress;
        if (listener != null) listener.onProgressChanged(this, getProgress(), false);
        invalidate();
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getMax() {
        return max;
    }

    @Override
    public void setMax(int max) {
        this.max = max;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
        params.width = (int) ((thumbSize * 2) + (trackWidth * 2));
        setLayoutParams(params);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                compute(event.getY());
                if (listener != null) listener.onStartTrackingTouch(this);
                break;
            case MotionEvent.ACTION_MOVE:
                compute(event.getY());
                if (listener != null) listener.onProgressChanged(this, progress, true);
                break;
            case MotionEvent.ACTION_UP:
                compute(event.getY());
                if (listener != null) listener.onStopTrackingTouch(this);
                break;
        }
        return true;
    }

    private void compute(float y) {
        progress = invertVertically ? (int) (max * y / getHeight()) : max - (int) (max * y / getHeight());
        if (progress > max) progress = max;
        else if (progress < 0) progress = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.drawPaint(p);

        int p;
        if (invertVertically) p = max - progress;
        else p = progress;

        float thumbY = getHeight() - (((float) (p) / max) * getHeight());
        if (thumbY > getHeight() - thumbSize - circleWidth) thumbY = getHeight() - thumbSize - circleWidth;
        else if (thumbY < thumbSize + circleWidth) thumbY = thumbSize + circleWidth;
        paintTrack.setColor(getContext().getColor(R.color.gray));
        canvas.drawCircle(getWidth() / 2f, thumbY, thumbSize, paintCircle);

        float y = Math.max(thumbSize, thumbY - thumbSize);
        paintTrack.setColor(getContext().getColor(invertVertically ? R.color.main : R.color.gray));
        canvas.drawLine(getWidth() / 2f, thumbSize, getWidth() / 2f, y, paintTrack);

        y = Math.min(getHeight() - thumbSize, thumbY + thumbSize);
        paintTrack.setColor(getContext().getColor(invertVertically ? R.color.gray : R.color.main));
        canvas.drawLine(getWidth() / 2f, y, getWidth() / 2f, getHeight() - thumbSize, paintTrack);
    }
}