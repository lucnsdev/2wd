package lucns.robot2wd.services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;

public abstract class BaseService extends Service {

    public class LocalBinder extends Binder {
        public BaseService getServiceInstance() {
            return BaseService.this;
        }
    }

    public static final String START_FOREGROUND = "start_foreground";
    public static final String STOP_FOREGROUND = "stop_foreground";
    public static final String STOP_SERVICE = "stop_service";
    public static final String ACTION = "action";
    public static final String TYPE = "type";

    private LocalBinder iBinder;
    private boolean isForeground;

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        iBinder = new LocalBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        iBinder = null;
    }

    public abstract NotificationProvider onForegroundRequested();

    public void onForegroundStarted() {
    }

    public void onForegroundStopped() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // String action = intent == null || intent.getAction() == null ? "" : intent.getAction();
        if (intent != null && intent.hasExtra(ACTION)) {
            switch (intent.getStringExtra(ACTION)) {
                case START_FOREGROUND:
                    isForeground = true;
                    NotificationProvider notification = onForegroundRequested();
                    startForeground(notification.getNotificationCode(), notification.getNotification(), intent.getIntExtra(BaseService.TYPE, 0));
                    onForegroundStarted();
                    break;
                case STOP_FOREGROUND:
                    stopForeground();
                    break;
                case STOP_SERVICE:
                    finish();
                    break;
            }
        }
        return START_STICKY;
    }

    public boolean isForeground() {
        return isForeground;
    }

    public void stopForeground() {
        if (!isForeground) return;
        isForeground = false;
        stopForeground(true);
        onForegroundStopped();
    }

    public void startForeground(Class<?> c, int serviceType) {
        Intent intent = new Intent(this, c);
        intent.putExtra(BaseService.ACTION, BaseService.START_FOREGROUND);
        intent.putExtra(BaseService.TYPE, serviceType);
        startForegroundService(intent);
    }

    public void startForeground(Class<?> c) {
        Intent intent = new Intent(this, c);
        intent.putExtra(BaseService.ACTION, BaseService.START_FOREGROUND);
        intent.putExtra(BaseService.TYPE, 0);
        startForegroundService(intent);
    }

    public void finish() {
        if (isForeground) stopForeground();
        stopSelf();
    }
}
