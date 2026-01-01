package lucns.robot2wd.utils;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class Utils {

    private static Vibrator vibrator;

    static {
        init();
    }

    private static void init() {
        Context context = App.getContext();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static void vibrate(int duration) {
        if (duration > 0) {
            vibrator.cancel();
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    public static void vibrate() {
        vibrator.cancel();
        vibrate(50);
    }
}
