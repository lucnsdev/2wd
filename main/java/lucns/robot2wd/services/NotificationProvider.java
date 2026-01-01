package lucns.robot2wd.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import lucns.robot2wd.R;
import lucns.robot2wd.utils.Annotator;

public class NotificationProvider {

    public abstract static class OnNotificationClick {

        public void onClick() {
        }

        public void onButtonClick() {
        }
    }

    private int generateFixedCode(String name) {
        Annotator annotator = new Annotator("notification_main_code.json");
        try {
            int values = 0;
            if (annotator.exists()) {
                JSONObject jsonObject = new JSONObject(annotator.getContent());
                JSONArray jsonArray = jsonObject.getJSONArray("ids");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    if (json.has(name)) {
                        return json.getInt(name);
                    } else {
                        values = json.getInt(json.keys().next());
                    }
                }
                if (values == 0) values = jsonObject.getInt("notification_main_code");
                values++;
                jsonArray.put(new JSONObject().put(name, values));
                annotator.setContent(jsonObject.toString());
            } else {
                char[] chars = app.toCharArray();
                if (chars.length > 0) {
                    for (int i = 0; i < chars.length; i++) {
                        values += Character.codePointAt(chars, i);
                    }
                }
                if (values < 1000 && values > 0) values += 1234;
                else if (values > 65536) values = 65536 - new Random().nextInt(64302);
                else if (values == 0) values = new Random().nextInt(64302) + 1234;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("notification_main_code", values);
                values++;
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(new JSONObject().put(name, values));
                jsonObject.put("ids", jsonArray);
                annotator.setContent(jsonObject.toString());
                return values;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private final String app;
    private final int NOTIFICATION_CODE;
    private final String NOTIFICATION_CLICK;
    private final String BUTTON_CLICK;
    private final String ID_ONE;

    private final Context context;
    private final OnNotificationClick callback;
    private final NotificationManager notificationManager;
    private Notification notification;
    private boolean isShowing;

    private Bitmap defaultIcon, tintedIcon;

    public NotificationProvider(Context context, String tag, int icon, OnNotificationClick callback) {
        this.context = context;
        this.callback = callback;
        app = context.getPackageName();
        ID_ONE = "one_" + app;
        NOTIFICATION_CODE = generateFixedCode(tag);
        NOTIFICATION_CLICK = NOTIFICATION_CODE + "_notification_click." + app;
        BUTTON_CLICK = NOTIFICATION_CODE + "_button_click." + app;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        defaultIcon = tintBitmap(icon, Color.valueOf(context.getColor(R.color.main)));
        createChannels();
    }

    private void createChannels() {
        NotificationChannel builderChannel = new NotificationChannel(ID_ONE, context.getString(R.string.notification_small), NotificationManager.IMPORTANCE_DEFAULT);
        builderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        builderChannel.enableLights(false);
        builderChannel.enableVibration(false);
        builderChannel.setSound(null, null);
        notificationManager.createNotificationChannel(builderChannel);
    }

    public int getNotificationCode() {
        return NOTIFICATION_CODE;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setTintedIcon(int icon, int colorId) {
        tintedIcon = tintBitmap(icon, Color.valueOf(context.getColor(colorId)));
    }

    public void setIcon(int icon) {
        defaultIcon = BitmapFactory.decodeResource(context.getResources(), icon);
    }

    public void setIcon(int icon, int colorId) {
        defaultIcon = tintBitmap(icon, Color.valueOf(context.getColor(colorId)));
    }

    public void show(String title) {
        show(title, null);
    }

    public void show(String title, String buttonText) {
        isShowing = true;
        Notification.Builder builder;
        builder = new Notification.Builder(context, ID_ONE);
        builder.setAutoCancel(false);
        builder.setOngoing(false);
        builder.setShowWhen(false);
        builder.setColorized(false);
        builder.setTicker(title);
        builder.setContentText(title);
        builder.setSmallIcon(Icon.createWithBitmap(tintedIcon == null ? defaultIcon : tintedIcon));
        builder.setCategory(Notification.CATEGORY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            builder.setContentIntent(PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(NOTIFICATION_CLICK), PendingIntent.FLAG_IMMUTABLE));
        } else {
            builder.setContentIntent(PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(NOTIFICATION_CLICK), PendingIntent.FLAG_UPDATE_CURRENT));
        }
        RemoteViews root;
        if (buttonText != null) {
            root = new RemoteViews(context.getPackageName(), R.layout.notification_small_with_button);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                root.setOnClickPendingIntent(R.id.button, PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(BUTTON_CLICK), PendingIntent.FLAG_IMMUTABLE));
            } else {
                root.setOnClickPendingIntent(R.id.button, PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(BUTTON_CLICK), PendingIntent.FLAG_UPDATE_CURRENT));
            }
            root.setTextViewText(R.id.button, buttonText);
        } else {
            root = new RemoteViews(context.getPackageName(), R.layout.notification_small);
        }

        root.setViewVisibility(R.id.textTitle, View.INVISIBLE);
        root.setImageViewBitmap(R.id.appIcon, tintedIcon == null ? defaultIcon : tintedIcon);
        root.setTextViewText(R.id.textDescription, title);
        builder.setCustomContentView(root);
        notification = builder.build();

        notificationManager.notify(NOTIFICATION_CODE, notification);
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(NOTIFICATION_CLICK);
            filter.addAction(BUTTON_CLICK);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
        } catch (Exception ignore) {
        }
    }

    public void hide() {
        isShowing = false;
        notificationManager.cancelAll();
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception ignore) {
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    public Bitmap tintBitmap(int id, Color target) {
        Drawable drawable = context.getDrawable(id);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        Bitmap bitmap2 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int color = Color.argb(bitmap2.getColor(x, y).alpha(), target.red(), target.green(), target.blue());
                bitmap2.setPixel(x, y, color);
            }
        }
        bitmap.recycle();
        return bitmap2;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null || intent.getAction() == null ? "" : intent.getAction();
            if (action.equals(NOTIFICATION_CLICK)) {
                if (callback != null) callback.onClick();
            } else if (action.equals(BUTTON_CLICK)) {
                if (callback != null) callback.onButtonClick();
            }
        }
    };
}
