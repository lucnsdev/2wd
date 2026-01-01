package lucns.robot2wd.remote;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import lucns.robot2wd.R;
import lucns.robot2wd.activities.VelocityProvider;
import lucns.robot2wd.services.MainService;
import lucns.robot2wd.services.remote.Transceiver;
import lucns.robot2wd.utils.Notify;
import lucns.robot2wd.utils.Prefs;
import lucns.robot2wd.utils.Utils;
import lucns.robot2wd.views.TouchableImageButton;
import lucns.robot2wd.views.TriangleView;

public class FloatController extends SuspendedWindow {

    private final TextView textStatus, textRssi;
    private final MainService mainService;
    private boolean isHiding;

    private int pwmForwardLeft, pwmForwardRight, pwmBackwardLeft, pwmBackwardRight, percentage;
    private final int MAXIMUM_PWM = 255;

    public FloatController(Context context, MainService mainService) {
        super(context, false);
        this.mainService = mainService;
        mainService.setCallback(callback);
        setCallback(new Callback() {
            @Override
            public void onBackPressed() {
                mainService.put(new Transceiver.Command("motors", new byte[]{'s'}, false));
                startHide();
            }
        });

        setContentView(R.layout.float_controller);
        //setAlpha(0.9f);
        //int widthDisplay = Resources.getSystem().getDisplayMetrics().widthPixels;
        //int minWidth = widthDisplay - (widthDisplay / 10);
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
        setSize(px, WindowManager.LayoutParams.WRAP_CONTENT);
        lock(true, true);
        setGravity(Gravity.BOTTOM);
        View chamfer = findViewById(R.id.chamfer);
        RelativeLayout toolbar = (RelativeLayout) findViewById(R.id.toolbar);
        toolbar.setOnTouchListener(new View.OnTouchListener() {

            int x, y, lastX, lastY, lastY2;
            float initialTouchX;
            float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = getXPosition();
                        lastY = lastY2;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        chamfer.setActivated(true);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        x = lastX + (int) (event.getRawX() - initialTouchX);
                        y = lastY + (int) (event.getRawY() - initialTouchY);
                        lastY2 = y;
                        setXPosition(x);
                        setYPosition(y * (-1));
                        applyLayoutParameters();
                        break;
                    case MotionEvent.ACTION_UP:
                        chamfer.setActivated(false);
                        break;
                }
                return true;
            }
        });
        textStatus = (TextView) findViewById(R.id.textStatus);
        textRssi = (TextView) findViewById(R.id.textRssi);

        View.OnClickListener onClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Utils.vibrate();
                if (v.getId() == R.id.buttonClose) {
                    startHide();
                } else if (v.getId() == R.id.buttonConnect) {
                    if (!mainService.isConnecting()) mainService.connect();
                }
            }
        };
        findViewById(R.id.buttonClose).setOnClickListener(onClick);

        TouchableImageButton buttonUp = findViewById(R.id.buttonUp);
        TouchableImageButton buttonDown = findViewById(R.id.buttonDown);
        TouchableImageButton buttonLeft = findViewById(R.id.buttonLeft);
        TouchableImageButton buttonRight = findViewById(R.id.buttonRight);

        pwmForwardLeft = Prefs.getInt("pwm_forward_left");
        pwmForwardRight = Prefs.getInt("pwm_forward_right");
        pwmBackwardLeft = Prefs.getInt("pwm_backward_left");
        pwmBackwardRight = Prefs.getInt("pwm_backward_right");
        if (pwmForwardLeft < 0) pwmForwardLeft = MAXIMUM_PWM;
        if (pwmForwardRight < 0) pwmForwardRight = MAXIMUM_PWM;
        if (pwmBackwardLeft < 0) pwmBackwardLeft = MAXIMUM_PWM;
        if (pwmBackwardRight < 0) pwmBackwardRight = MAXIMUM_PWM;

        TouchableImageButton.TouchCallback touchCallback = new TouchableImageButton.TouchCallback() {
            int left, right;

            final VelocityProvider provider = new VelocityProvider(50, 25, 100, 5, new VelocityProvider.Callback() {
                @Override
                public void onValueChanged(int velocity) {
                    percentage = velocity;
                    updateValues();
                }
            });

            private void updateValues() {
                int pwm = (int) (MAXIMUM_PWM * ((float) percentage / 100));
                //Log.d("Lucas", "pwm" + pwm);
                int initialPwm = (int) (pwm * (20f / 100));
                boolean persistent = true;
                if (buttonUp.isTouched()) {
                    if (buttonLeft.isTouched()) {
                        left = -pwm;
                        right = -initialPwm;
                    } else if (buttonRight.isTouched()) {
                        left = -initialPwm;
                        right = -pwm;
                    } else {
                        left = (int) (-pwmForwardLeft * ((float) percentage / 100));
                        right = (int) (-pwmForwardRight * ((float) percentage / 100));
                        if ((pwmForwardLeft == 255 || pwmForwardRight == 255) && (pwmForwardLeft != pwmForwardRight) && (left != -255 && right != -255)) {
                            if (left > right) left = right;
                            else right = left;
                        }
                    }
                } else if (buttonDown.isTouched()) {
                    if (buttonLeft.isTouched()) {
                        left = initialPwm;
                        right = pwm;
                    } else if (buttonRight.isTouched()) {
                        left = pwm;
                        right = initialPwm;
                    } else {
                        left = (int) (pwmBackwardLeft * ((float) percentage / 100));
                        right = (int) (pwmForwardRight * ((float) percentage / 100));
                        if ((pwmBackwardLeft == 255 || pwmBackwardRight == 255) && (pwmBackwardLeft != pwmBackwardRight) && (left != 255 && right != 255)) {
                            if (left > right) right = left;
                            else left = right;
                        }
                    }
                } else if (!buttonLeft.isTouched() && !buttonRight.isTouched()) {
                    left = 0;
                    right = 0;
                    persistent = false;
                }

                if (buttonLeft.isTouched()) {
                    if (buttonUp.isTouched()) {
                        left = -initialPwm;
                        right = -pwm;
                    } else if (buttonDown.isTouched()) {
                        left = initialPwm;
                        right = pwm;
                    } else {
                        left = pwm;
                        right = -pwm;
                    }
                } else if (buttonRight.isTouched()) {
                    if (buttonUp.isTouched()) {
                        left = -pwm;
                        right = -initialPwm;
                    } else if (buttonDown.isTouched()) {
                        left = pwm;
                        right = initialPwm;
                    } else {
                        left = -pwm;
                        right = pwm;
                    }
                } else if (!buttonUp.isTouched() && !buttonDown.isTouched()) {
                    left = 0;
                    right = 0;
                    persistent = false;
                }

                if (persistent) {
                    if (!provider.isRunning()) provider.start();
                } else {
                    provider.stop();
                }
                //Log.d("Lucas", left + "x" + right);
                byte[] payload = new byte[5];
                payload[0] = 'm';
                payload[1] = (byte) ((left & 0x0000FF00) >> 8);
                payload[2] = (byte) ((left & 0x000000FF));
                payload[3] = (byte) ((right & 0x0000FF00) >> 8);
                payload[4] = (byte) ((right & 0x000000FF));
                mainService.put(new Transceiver.Command("motors", payload, persistent));
            }

            @Override
            public void onTouch(TouchableImageButton view, boolean touched) {
                if (touched) Utils.vibrate(75);
                else Utils.vibrate(40);
                updateValues();
            }
        };

        buttonUp.setTouchCallback(touchCallback);
        buttonDown.setTouchCallback(touchCallback);
        buttonLeft.setTouchCallback(touchCallback);
        buttonRight.setTouchCallback(touchCallback);
    }

    public void show() {
        setAlpha(0f);
        super.show();
        mainService.setCallback(callback);
        updateText();
        ObjectAnimator o = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f).setDuration(300);
        o.setInterpolator(new DecelerateInterpolator());
        o.start();

        // if (!mainService.isConnected() && !mainService.isConnecting()) mainService.connect();
        //setYPosition(getNavigationBarHeight() * (-1));
        //applyLayoutParameters();
    }

    public void startHide() {
        if (isHiding || !isShowing()) return;
        isHiding = true;
        ObjectAnimator o = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0f).setDuration(300);
        o.setInterpolator(new AccelerateInterpolator());
        o.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                hide();
                isHiding = false;
            }
        });
        o.start();
    }

    private final Transceiver.Callback callback = new Transceiver.Callback() {

        int lastPercentage = 100;

        @Override
        public void onScanning() {
            textStatus.setTextColor(getColor(R.color.orange));
            textStatus.setText(R.string.scanning);
        }

        @Override
        public void onScanCompleted(BluetoothDevice[] devices) {
            if (devices.length == 0) {
                Notify.showToast(R.string.no_devices);
                textStatus.setTextColor(getColor(R.color.red));
                textStatus.setText(R.string.no_devices);
                return;
            }
            textStatus.setTextColor(getColor(R.color.orange));
            textStatus.setText(R.string.choose_device);
            //showDialogList(devices);
        }

        @Override
        public void onConnecting() {
            if (isHiding) return;
            String text = getString(R.string.connecting);
            textStatus.setText(text);
        }

        @Override
        public void onConnectionStateChanged(boolean connected) {
            Notify.showToast(connected ? R.string.connected : R.string.disconnected);
            textStatus.setText(connected ? R.string.connected : R.string.disconnected);
            textStatus.setTextColor(getColor(connected ? R.color.white : R.color.red));
        }

        @Override
        public void onPreparing() {
            lastPercentage = 100;
            textStatus.setTextColor(getColor(R.color.white));
            textStatus.setText(R.string.preparing);
        }

        @Override
        public void onPrepared() {
            Utils.vibrate();
            textStatus.setText(R.string.ready);
        }

        @Override
        public void onFieldStrengthChanged(int rssi) {
            textRssi.setText(rssi + "dbm");
            if (rssi < -80) textRssi.setTextColor(getColor(R.color.red));
            else if (rssi < -75) textRssi.setTextColor(getColor(R.color.orange));
            else textRssi.setTextColor(getColor(R.color.main));
        }

        public double resizeNumber(double value) {
            int temp = (int) (value * 10.0d);
            return temp / 10.0d;
        }

        @Override
        public void onReceive(byte[] payload) {
            if (payload.length < 3) return;
            int raw = (payload[1] << 8) + (payload[2] & 0xFF);
            int percentage;
            if (raw <= 570) { // (3.63v = 570) (3.7volts = 581)
                percentage = 0;
            } else if (raw >= 670) { // (4.26v = 670) (4.2volts = 659)
                percentage = 100;
            } else {
                percentage = raw - 570;
            }
            if (percentage < lastPercentage) lastPercentage = percentage;
            textStatus.setText(lastPercentage + "%");
            if (percentage >= 50) {
                textStatus.setTextColor(getColor(R.color.green));
            } else if (percentage > 24) {
                textStatus.setTextColor(getColor(R.color.orange));
            } else {
                textStatus.setTextColor(getColor(R.color.red));
            }
        }
    };

    private void updateText() {
        if (mainService.isConnected()) {
            textStatus.setText(R.string.ready);
        } else if (mainService.isConnecting()) {
            textStatus.setTextColor(getColor(R.color.orange));
            textStatus.setText(R.string.connecting);
        } else if (mainService.isPreparing()) {
            textStatus.setTextColor(getColor(R.color.white));
            textStatus.setText(R.string.preparing);
        } else if (mainService.isScanning()) {
            textStatus.setTextColor(getColor(R.color.orange));
            textStatus.setText(R.string.scanning);
        } else {
            textStatus.setTextColor(getColor(R.color.red));
            textStatus.setText(R.string.disconnected);
        }
    }
}
