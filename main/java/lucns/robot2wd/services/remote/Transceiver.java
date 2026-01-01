package lucns.robot2wd.services.remote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Transceiver {

    public abstract static class Callback {

        public void onConnectionStateChanged(boolean connected) {
        }

        public void onScanning() {
        }

        public void onScanCompleted(BluetoothDevice[] results) {
        }

        public void onConnecting() {
        }

        public void onPreparing() {
        }

        public void onPrepared() {
        }

        public void onFieldStrengthChanged(int rssi) {
        }

        public void onReceive(byte[] payload) {
        }
    }

    public static class Command {
        public String key;
        public byte[] payload;
        public boolean persistent;

        public Command(String key, byte[] payload, boolean persistent) {
            this.key = key;
            this.payload = payload;
            this.persistent = persistent;
        }
    }

    private final Context context;
    private final Callback callback;
    private final Handler mainLooper;

    private String serviceUuid, characteristicTxUuid, characteristicRxUuid, descriptorNotify;

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGattCharacteristic characteristicTx;
    private String address;
    private final Map<String, Command> map;
    private final Map<String, BluetoothDevice> scanResults;
    private boolean canceled, isScanning, isConnected, isPreparing, isConnecting;

    public Transceiver(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
        map = new LinkedHashMap<>();
        scanResults = new HashMap<>();
        mainLooper = new Handler(Looper.getMainLooper());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled())
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public void setUuids(String service, String tx, String rx, String descriptor) {
        serviceUuid = service;
        characteristicTxUuid = tx;
        characteristicRxUuid = rx;
        descriptorNotify = descriptor;
    }

    public void startScan() {
        if (!bluetoothAdapter.isEnabled() || canceled || isScanning) return;
        isScanning = true;
        scanResults.clear();
        callback.onScanning();

        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        builder.setReportDelay(0L);
        try {
            bluetoothLeScanner.startScan(null, builder.build(), scanCallback);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        mainLooper.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, 10000);
    }

    public void stopScan() {
        if (isScanning) {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        if (callback != null && isScanning && !canceled) {
            callback.onScanCompleted(getDevices());
        }
        isScanning = false;
    }

    public BluetoothDevice[] getDevices() {
        BluetoothDevice[] devices = new BluetoothDevice[scanResults.size()];
        int i = 0;
        for (BluetoothDevice b : scanResults.values()) devices[i++] = b;
        return devices;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            scanResults.put(result.getDevice().getAddress(), result.getDevice());
            /*
            try {
                Log.d("lucas", result.getDevice().getAddress() + " " + result.getDevice().getName());
            } catch (SecurityException e) {
                e.printStackTrace();
            }
             */

            if (address == null) return;
            for (BluetoothDevice b : scanResults.values()) {
                if (b.getAddress().equals(address)) {
                    isScanning = false;
                    try {
                        bluetoothLeScanner.stopScan(scanCallback);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    connect(address);
                }
            }
        }
    };

    public boolean isConnecting() {
        return isConnecting;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isPreparing() {
        return isPreparing;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void cancelConnect() {
        if (isScanning) {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        canceled = true;
        isScanning = false;
    }

    public void connect(final String address) {
        if (isConnecting || isConnected) return;
        this.address = address;
        canceled = false;

        if (address == null) {
            if (!isScanning) startScan();
        } else {
            isConnecting = true;
            callback.onConnecting();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            try {
                bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback);
                return;
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            isConnecting = false;
        }
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable updateRssi = new Runnable() {
            @Override
            public void run() {
                if (canceled || !isConnected || callback == null || bluetoothGatt == null) return;
                try {
                    bluetoothGatt.readRemoteRssi();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        };

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        long start;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (!gatt.getDevice().getAddress().equals(address)) return;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    isConnecting = false;
                    isConnected = true;
                    isPreparing = true;
                    mainLooper.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onConnectionStateChanged(true);
                            callback.onPreparing();
                        }
                    });
                    try {
                        bluetoothGatt.discoverServices();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    isConnecting = false;
                    isConnected = false;
                    mainLooper.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onConnectionStateChanged(false);
                        }
                    });
                    if (canceled) break;
                    // connect(address);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            isPreparing = false;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService bluetoothGattService = gatt.getService(UUID.fromString(serviceUuid));
                characteristicTx = bluetoothGattService.getCharacteristic(UUID.fromString(characteristicTxUuid));
                BluetoothGattCharacteristic characteristicRx = bluetoothGattService.getCharacteristic(UUID.fromString(characteristicRxUuid));
                try {
                    gatt.setCharacteristicNotification(characteristicRx, true);
                    BluetoothGattDescriptor descriptor = characteristicRx.getDescriptor(UUID.fromString(descriptorNotify));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);

                    gatt.readRemoteRssi();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }

                mainLooper.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onPrepared();
                    }
                });
            } else {
                if (!canceled) return;
                close();
                cancelConnect();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            out.writeBytes(characteristic.getValue());
            long now = System.currentTimeMillis();
            if (start == 0) start = now;
            if (now - start > 500) {
                start = now;
                mainLooper.post(new Runnable() {

                    @Override
                    public void run() {
                        callback.onReceive(out.toByteArray());
                        out.reset();
                    }
                });
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (canceled || !isConnected() || callback == null) return;
                    callback.onFieldStrengthChanged(rssi);
                    handler.postDelayed(updateRssi, 1000);
                }
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (map.size() > 0) {
                Command command = map.get(map.keySet().toArray(new String[0])[0]);
                if (command == null) return;
                if (!command.persistent) map.remove(command.key);
                send(command);
            }
        }
    };

    public void put(Command command) {
        if (!isConnected) return;
        if (map.size() == 0) {
            send(command);
            if (command.persistent) map.put(command.key, command);
            else map.remove(command.key);
        } else {
            map.put(command.key, command);
        }
    }

    private void send(Command command) {
        if (!isConnected || characteristicTx == null || bluetoothGatt == null) return;
        characteristicTx.setValue(command.payload);
        try {
            bluetoothGatt.writeCharacteristic(characteristicTx);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        canceled = true;
        address = null;
        isPreparing = false;
        isConnected = false;
        isScanning = false;
        map.clear();
        if (isScanning) stopScan();
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    public void disable() {
        close();
        try {
            bluetoothAdapter.disable();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
