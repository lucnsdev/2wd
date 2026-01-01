package lucns.robot2wd.services;

import android.bluetooth.BluetoothDevice;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import lucns.robot2wd.R;
import lucns.robot2wd.remote.FloatController;
import lucns.robot2wd.services.remote.Transceiver;
import lucns.robot2wd.utils.Notify;
import lucns.robot2wd.utils.Utils;

public class MainService extends BaseService {

    private final String E104_NAME = "E104-BT5032A";
    private final String E104_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    private final String E104_TX_CHARACTERISTIC = "0000fff2-0000-1000-8000-00805f9b34fb";
    private final String E104_RX_CHARACTERISTIC = "0000fff1-0000-1000-8000-00805f9b34fb";
    private final String E104_RX_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
    //private final String E104_MAC = "D0:8E:8A:5C:85:62";
    private final String E104_MAC = "F3:C1:0E:99:0C:76";

    private NotificationProvider notification;
    private Transceiver transceiver;
    private Transceiver.Callback callback;
    private String deviceAddress = E104_MAC;
    //private String deviceAddress = null;
    private boolean disconnectedByUser;

    private FloatController floatController;

    public void setCallback(Transceiver.Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notification = new NotificationProvider(this, "main_service", R.drawable.icon_car, new NotificationProvider.OnNotificationClick() {
            @Override
            public void onClick() {
                if (!floatController.isShowing()) floatController.show();
            }

            @Override
            public void onButtonClick() {
                if (floatController.isShowing()) floatController.startHide();
                transceiver.disable();
                stopForeground();
                notification.hide();
                stopSelf();
            }
        });

        transceiver = new Transceiver(this, new Transceiver.Callback() {

            @Override
            public void onScanning() {
                if (callback != null) callback.onScanning();
                if (isForeground()) notification.show(getString(R.string.scanning), getString(R.string.cancel));
            }

            @Override
            public void onScanCompleted(BluetoothDevice[] devices) {
                if (disconnectedByUser) return;
                if (devices.length == 0) {
                    Utils.vibrate();
                    Notify.showToast(R.string.no_devices);
                    if (isForeground()) notification.show(getString(R.string.scan_completed) + ". " + getString(R.string.no_devices), getString(R.string.cancel));
                    callback.onScanCompleted(devices);
                } else {
                    try {
                        for (BluetoothDevice device : devices) {
                            String name = device.getName();
                            if (name != null && name.equals(E104_NAME)) {
                                transceiver.connect(device.getAddress());
                                return;
                            }
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    if (callback != null) callback.onScanCompleted(devices);
                    if (isForeground()) notification.show(getString(R.string.scan_completed) + ". " + getString(R.string.device_not_found), getString(R.string.cancel));
                }
            }

            @Override
            public void onConnecting() {
                if (callback != null) callback.onConnecting();
                if (isForeground()) notification.show(getString(R.string.connecting), getString(R.string.cancel));
            }

            @Override
            public void onConnectionStateChanged(boolean connected) {
                if (disconnectedByUser) return;
                if (!connected) Utils.vibrate();
                if (callback != null) {
                    callback.onConnectionStateChanged(connected);
                    disconnectedByUser = false;
                }
                if (isForeground()) notification.show(getString(connected ? R.string.connected : R.string.disconnected), getString(R.string.cancel));
                Notify.showToast(connected ? R.string.connected : R.string.disconnected);
            }

            @Override
            public void onPreparing() {
                if (callback != null) callback.onPreparing();
                if (isForeground()) notification.show(getString(R.string.preparing), getString(R.string.cancel));
            }

            @Override
            public void onPrepared() {
                if (callback != null) callback.onPrepared();
                if (isForeground()) notification.show(getString(R.string.ready), getString(R.string.disconnect));
            }

            @Override
            public void onReceive(byte[] payload) {
                if (callback != null) callback.onReceive(payload);
                //Log.d("lucas", "received:" + s);
            }

            @Override
            public void onFieldStrengthChanged(int rssi) {
                if (callback != null) callback.onFieldStrengthChanged(rssi);
            }
        });
        transceiver.setUuids(E104_SERVICE, E104_TX_CHARACTERISTIC, E104_RX_CHARACTERISTIC, E104_RX_DESCRIPTOR);

        floatController = new FloatController(this, this);
    }

    public void startForeground() {
        if (!isForeground()) startForeground(MainService.class, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        transceiver.disable();
    }

    @Override
    public NotificationProvider onForegroundRequested() {
        Log.d("lucas", "onForegroundRequested");

        if (!notification.isShowing()) {
            String title;
            if (isConnected()) {
                title = getString(R.string.connected);
                notification.show(title, getString(R.string.disconnect));
                return notification;
            } else if (isConnecting()) {
                title = getString(R.string.connecting);
            } else if (isPreparing()) {
                title = getString(R.string.preparing);
            } else if (isScanning()) {
                title = getString(R.string.scanning);
            } else {
                title = getString(R.string.disconnected);
                notification.show(title, getString(R.string.close));
                return notification;
            }
            notification.show(title, getString(R.string.cancel));
        }
        return notification;
    }

    public void showSuspendedController() {
        startForeground();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                floatController.show();
            }
        }, 500);
    }

    public void hideSuspendedController() {
        if (floatController.isShowing()) floatController.startHide();
    }

    public BluetoothDevice[] getDevices() {
        return transceiver.getDevices();
    }

    public void close() {
        if (transceiver.isConnected()) transceiver.close();
    }

    public void put(Transceiver.Command command) {
        transceiver.put(command);
    }

    public boolean isScanning() {
        return transceiver.isScanning();
    }

    public boolean isPreparing() {
        return transceiver.isPreparing();
    }

    public boolean isConnecting() {
        return transceiver.isConnecting();
    }

    public boolean isConnected() {
        return transceiver.isConnected();
    }

    public void disconnect() {
        disconnectedByUser = true;
        transceiver.close();
    }

    public void connect() {
        disconnectedByUser = false;
        //deviceAddress = null;
        transceiver.connect(deviceAddress);
    }

    public void connect(String address) {
        disconnectedByUser = false;
        deviceAddress = address;
        transceiver.connect(address);
    }
}
