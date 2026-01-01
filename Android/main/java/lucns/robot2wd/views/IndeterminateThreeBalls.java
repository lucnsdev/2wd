package lucns.robot2wd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import lucns.robot2wd.R;
import lucns.robot2wd.views.animations.Animation;
import lucns.robot2wd.views.animations.base.BaseAnimation;
import lucns.robot2wd.views.animations.base.SimultaneousAnimation;

import java.util.ArrayList;
import java.util.List;

public class IndeterminateThreeBalls extends RelativeLayout {

    private boolean viewsInserted;
    private int circleSize;
    private final List<CircleView> list;
    private boolean backing;

    public IndeterminateThreeBalls(Context context) {
        super(context);
        //setBackgroundColor(Color.parseColor("#ff0000"));
        list = new ArrayList<>();
    }

    public IndeterminateThreeBalls(Context context, AttributeSet attrs) {
        super(context, attrs);
        //setBackgroundColor(Color.parseColor("#ff0000"));
        list = new ArrayList<>();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (r > 0 && b > 0) {
            insertViews();
        }
    }

    private void insertViews() {
        if (viewsInserted) return;
        viewsInserted = true;
        float dip = 12f;
        circleSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
        int spaceBetweenCircles = circleSize / 10;

        int circlesQuantity = 3;
        int viewHeight = (circleSize * circlesQuantity) + ((circlesQuantity - 1) * spaceBetweenCircles);
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params == null) {
            params = new LayoutParams(circleSize, viewHeight);
        } else {
            params.width = circleSize;
            params.height = viewHeight;
        }
        setLayoutParams(params);
        for (int i = 0; i < circlesQuantity; i++) {
            CircleView circleView = new CircleView(getContext());
            circleView.setColor(getContext().getColor(R.color.main));
            LayoutParams circleParams = new LayoutParams(circleSize, circleSize);
            addView(circleView, circleParams);
            circleView.setLayoutParams(circleParams);
            circleView.setY(i * (circleSize + spaceBetweenCircles));
            circleView.setIndex(i);
            circleView.setScaleX(0.5f);
            circleView.setScaleY(0.5f);
            list.add(circleView);
        }
        list.get(0).startAnimations();
    }

    private void updateNext(CircleView circleView) {
        int index = circleView.getIndex();
        if (index == list.size() - 1) backing = true;
        else if (index == 0) backing = false;
        if (backing) index--;
        else index++;

        circleView = list.get(index);
        circleView.startAnimations();
    }

    private class CircleView extends View {

        private Paint paintFill, paintStroke;
        private SimultaneousAnimation in, out;
        private int index;

        public CircleView(Context context) {
            super(context);
            init();
        }

        public CircleView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            paintFill = new Paint();
            paintFill.setColor(Color.WHITE);
            paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintStroke.setStrokeWidth(2);
            paintStroke.setColor(Color.BLACK);
            paintStroke.setStyle(Paint.Style.STROKE);
            in = new SimultaneousAnimation();
            long duration = 500;
            in.add(new Animation(this, View.SCALE_X, 0.5f, 1f, duration));
            in.add(new Animation(this, View.SCALE_Y, 0.5f, 1f, duration));
            in.setCallback(new BaseAnimation.Callback() {
                @Override
                public void onFinish() {
                    updateNext(CircleView.this);
                    out.start();
                }
            });
            out = new SimultaneousAnimation();
            out.add(new Animation(this, View.SCALE_X, 1f, 0.5f, duration));
            out.add(new Animation(this, View.SCALE_Y, 1f, 0.5f, duration));
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public void setColor(int color) {
            paintFill.setColor(color);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (r > 0 && b > 0) {
                //startAnimations();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (in != null) in.stop();
            if (out != null) out.stop();
        }

        public void startAnimations() {
            in.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(circleSize / 2f, circleSize / 2f, circleSize / 2f, paintFill);
            //canvas.drawCircle(circleSize / 2f, circleSize / 2f, circleSize / 2f, paintStroke);
        }
    }
}
