package lucns.robot2wd.activities;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import lucns.robot2wd.R;
import lucns.robot2wd.services.MainService;
import lucns.robot2wd.services.ServiceController;
import lucns.robot2wd.services.remote.Transceiver;
import lucns.robot2wd.utils.Notify;
import lucns.robot2wd.utils.Prefs;
import lucns.robot2wd.utils.Utils;
import lucns.robot2wd.views.TriangleView;
import lucns.robot2wd.views.VerticalSeekBar;

public class EasyControllerActivity extends BaseActivity {

    // private PopupMenu popupMenu;
    private MainService mainService;
    private TextView textRssi, textBattery, textStatus;
    private ImageButton buttonConnect;
    private TriangleView buttonUp, buttonDown, buttonLeft, buttonRight;
    private TextView textTx, textRx, textVelocity;

    private int pwmForwardLeft, pwmForwardRight, pwmBackwardLeft, pwmBackwardRight, percentage;
    private int MAXIMUM_PWM = 255;

    private RelativeLayout root;
    private ValueAnimator animator;
    private int currentColor;
    private int colorLow, colorMiddle, colorHigh, colorTarget;

    @Override
    public boolean onCreated() {
        setContentView(R.layout.activity_easy_controller);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        root = findViewById(R.id.root);
        colorLow = getColor(R.color.background_app_low);
        colorMiddle = getColor(R.color.background_app_middle);
        colorHigh = getColor(R.color.background_app_normal);

        pwmForwardLeft = Prefs.getInt("pwm_forward_left");
        pwmForwardRight = Prefs.getInt("pwm_forward_right");
        pwmBackwardLeft = Prefs.getInt("pwm_backward_left");
        pwmBackwardRight = Prefs.getInt("pwm_backward_right");
        if (pwmForwardLeft < 0) pwmForwardLeft = MAXIMUM_PWM;
        if (pwmForwardRight < 0) pwmForwardRight = MAXIMUM_PWM;
        if (pwmBackwardLeft < 0) pwmBackwardLeft = MAXIMUM_PWM;
        if (pwmBackwardRight < 0) pwmBackwardRight = MAXIMUM_PWM;

        textStatus = findViewById(R.id.textStatus);
        textVelocity = findViewById(R.id.textVelocityValue);
        textRssi = findViewById(R.id.textRssiValue);
        textBattery = findViewById(R.id.textBatteryValue);
        textTx = findViewById(R.id.textTx);
        textRx = findViewById(R.id.textRx);

        buttonUp = findViewById(R.id.buttonUp);
        buttonDown = findViewById(R.id.buttonDown);
        buttonLeft = findViewById(R.id.buttonLeft);
        buttonRight = findViewById(R.id.buttonRight);
        buttonUp.setEnabled(false);
        buttonDown.setEnabled(false);
        buttonLeft.setEnabled(false);
        buttonRight.setEnabled(false);
        buttonUp.setPosition(TriangleView.Positions.TOP);
        buttonDown.setPosition(TriangleView.Positions.BOTTOM);
        buttonLeft.setPosition(TriangleView.Positions.LEFT);
        buttonRight.setPosition(TriangleView.Positions.RIGHT);

        TriangleView.TouchCallback touchCallback = new TriangleView.TouchCallback() {
            int left, right;

            final VelocityProvider provider = new VelocityProvider(50, 25, 100, 5, new VelocityProvider.Callback() {
                @Override
                public void onValueChanged(int velocity) {
                    percentage = velocity;
                    textVelocity.setText(velocity + "%");

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
                    textVelocity.setText(String.valueOf(0));
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
            public void onTouch(TriangleView view, boolean touched) {
                if (touched) Utils.vibrate(75);
                else Utils.vibrate(40);
                updateValues();
            }
        };

        buttonUp.setTouchCallback(touchCallback);
        buttonDown.setTouchCallback(touchCallback);
        buttonLeft.setTouchCallback(touchCallback);
        buttonRight.setTouchCallback(touchCallback);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainService == null) return;
                if (v.getId() == R.id.buttonConnect) {
                    if (mainService.isConnected()) {
                        mainService.disconnect();
                    } else {
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter.isEnabled()) {
                            textStatus.setTextColor(R.color.orange);
                            textStatus.setText(R.string.connecting);
                            mainService.connect();
                        }
                        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1234);
                    }
                    buttonConnect.setImageResource(mainService.isConnected() ? R.drawable.icon_close : R.drawable.icon_reconnect);
                } else if (v.getId() == R.id.buttonSpeed) {
                    showDialogSpeed();
                } else if (v.getId() == R.id.buttonController) {
                    mainService.showSuspendedController();
                    finish();
                }
                updateText();
            }
        };
        findViewById(R.id.buttonConnect).setOnClickListener(onClickListener);
        findViewById(R.id.buttonSpeed).setOnClickListener(onClickListener);
        findViewById(R.id.buttonController).setOnClickListener(onClickListener);
        buttonConnect = findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(onClickListener);

        /*
        popupMenu = new PopupMenu(this, buttonMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                }
                return true;
            }
        });
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popupMenu.getMenu());
         */

        ServiceController.getInstance(this, new ServiceController.OnServiceAvailableListener() {
            @Override
            public void onAvailable(MainService mainService) {
                EasyControllerActivity.this.mainService = mainService;
                mainService.stopForeground();
                mainService.setCallback(callback);
                updateText();
                buttonUp.setEnabled(true);
                buttonDown.setEnabled(true);
                buttonLeft.setEnabled(true);
                buttonRight.setEnabled(true);
                buttonConnect.setImageResource(mainService.isConnected() ? R.drawable.icon_close : R.drawable.icon_reconnect);
                if (mainService.isConnected()) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
        //registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        return true;
    }

    private void updateColor(int color) {
        if (colorTarget == color) return;
        if (animator != null) animator.cancel();
        colorTarget = color;

        animator = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, colorTarget);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                currentColor = (int) animator.getAnimatedValue();
                root.setBackgroundColor(currentColor);
                getWindow().setStatusBarColor(currentColor);
                getWindow().setNavigationBarColor(currentColor);
            }
        });
        animator.setDuration(500);
        animator.start();
    }

    private void showDialogSpeed() {
        Dialog dialog = generateDialog(R.layout.dialog_speeds, true);
        VerticalSeekBar seekBarForwardLeft = dialog.findViewById(R.id.seekBarForwardLeft);
        VerticalSeekBar seekBarForwardRight = dialog.findViewById(R.id.seekBarForwardRight);
        VerticalSeekBar seekBarBackwardLeft = dialog.findViewById(R.id.seekBarBackwardLeft);
        VerticalSeekBar seekBarBackwardRight = dialog.findViewById(R.id.seekBarBackwardRight);
        seekBarBackwardLeft.isInvertVertically(true);
        seekBarBackwardRight.isInvertVertically(true);
        seekBarForwardLeft.setMax(MAXIMUM_PWM);
        seekBarForwardRight.setMax(MAXIMUM_PWM);
        seekBarBackwardLeft.setMax(MAXIMUM_PWM);
        seekBarBackwardRight.setMax(MAXIMUM_PWM);
        seekBarForwardLeft.setProgress(pwmForwardLeft);
        seekBarForwardRight.setProgress(pwmForwardRight);
        seekBarBackwardLeft.setProgress(pwmBackwardLeft);
        seekBarBackwardRight.setProgress(pwmBackwardRight);
        TextView textForwardLeft = dialog.findViewById(R.id.textForwardLeft);
        TextView textForwardRight = dialog.findViewById(R.id.textForwardRight);
        TextView textBackwardLeft = dialog.findViewById(R.id.textBackwardLeft);
        TextView textBackwardRight = dialog.findViewById(R.id.textBackwardRight);
        textForwardLeft.setText(String.valueOf(pwmForwardLeft));
        textForwardRight.setText(String.valueOf(pwmForwardRight));
        textBackwardLeft.setText(String.valueOf(pwmBackwardLeft));
        textBackwardRight.setText(String.valueOf(pwmBackwardRight));
        SeekBar.OnSeekBarChangeListener onSeek = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getId() == R.id.seekBarForwardLeft) {
                    textForwardLeft.setText(String.valueOf(progress));
                } else if (seekBar.getId() == R.id.seekBarForwardRight) {
                    textForwardRight.setText(String.valueOf(progress));
                } else if (seekBar.getId() == R.id.seekBarBackwardLeft) {
                    textBackwardLeft.setText(String.valueOf(progress));
                } else { // R.id.seekBarBackwardRight
                    textBackwardRight.setText(String.valueOf(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getId() == R.id.seekBarForwardLeft) {
                    pwmForwardLeft = seekBar.getProgress();
                    Prefs.setInt("pwm_forward_left", seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarForwardRight) {
                    pwmForwardRight = seekBar.getProgress();
                    Prefs.setInt("pwm_forward_right", seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarBackwardLeft) {
                    pwmBackwardLeft = seekBar.getProgress();
                    Prefs.setInt("pwm_backward_left", seekBar.getProgress());
                } else { // R.id.seekBarBackwardRight
                    pwmBackwardRight = seekBar.getProgress();
                    Prefs.setInt("pwm_backward_right", seekBar.getProgress());
                }
            }
        };
        seekBarForwardLeft.setOnSeekBarChangeListener(onSeek);
        seekBarForwardRight.setOnSeekBarChangeListener(onSeek);
        seekBarForwardLeft.setOnSeekBarChangeListener(onSeek);
        seekBarBackwardLeft.setOnSeekBarChangeListener(onSeek);
        seekBarBackwardRight.setOnSeekBarChangeListener(onSeek);
        dialog.show();
    }

    @Override
    public void onResumed() {
        super.onResumed();
        if (mainService != null) {
            mainService.hideSuspendedController();
            mainService.stopForeground();
            buttonConnect.setImageResource(mainService.isConnected() ? R.drawable.icon_close : R.drawable.icon_reconnect);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPaused() {
        super.onPaused();
        //popupMenu.dismiss();
        if (mainService != null) {
            if (mainService.isConnected()) {
                mainService.put(new Transceiver.Command("motors", new byte[]{'s'}, false));
                return;
            } else if (!mainService.isScanning() || mainService.isConnecting() || mainService.isPreparing()) {
                mainService.startForeground();
                return;
            }
            mainService.stopForeground(true);
            mainService.stopSelf();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();
        //unregisterReceiver(bluetoothReceiver);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            if (resultCode == RESULT_OK) {
                if (isFinishing()) return;
                textStatus.setTextColor(getColor(R.color.orange));
                textStatus.setText(R.string.connecting);
                mainService.connect();
                return;
            }
            Notify.showToast(R.string.canceled);
        }
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
            if (isFinishing()) return;
            String text = getString(R.string.connecting);
            textStatus.setText(text);
        }

        @Override
        public void onConnectionStateChanged(boolean connected) {
            buttonConnect.setImageResource(R.drawable.icon_reconnect);
            if (connected) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                Utils.vibrate();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            Notify.showToast(connected ? R.string.connected : R.string.disconnected);
            textStatus.setText(connected ? R.string.connected : R.string.disconnected);
            textStatus.setTextColor(getColor(connected ? R.color.white : R.color.red));
        }

        @Override
        public void onPreparing() {
            lastPercentage = 100;
            buttonConnect.setImageResource(R.drawable.icon_close);
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
            textRssi.setText(String.valueOf(rssi));
            if (rssi <= -90) {
                textRssi.setTextColor(getColor(R.color.red));
                updateColor(colorLow);
            } else if (rssi <= -80) {
                textRssi.setTextColor(getColor(R.color.orange));
                updateColor(colorMiddle);
            } else {
                textRssi.setTextColor(getColor(R.color.green));
                updateColor(colorHigh);
            }
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
            textBattery.setText(lastPercentage + "%");
            if (percentage >= 50) {
                textBattery.setTextColor(getColor(R.color.green));
            } else if (percentage > 24) {
                textBattery.setTextColor(getColor(R.color.orange));
            } else {
                textBattery.setTextColor(getColor(R.color.red));
            }
        }
    };

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null || intent.getAction() == null ? "" : intent.getAction();
            Log.d("lucas", "action " + action);

            int state;
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == BluetoothAdapter.STATE_OFF) {
                    mainService.close();
                    callback.onConnectionStateChanged(false);
                } else if (state == BluetoothAdapter.STATE_ON) {
                    textStatus.setText(R.string.connecting);
                    mainService.connect();
                    //if (address != null) connect(address);
                }
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