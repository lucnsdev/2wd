package lucns.robot2wd.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.widget.Toast;

import java.util.Locale;

public class Notify {

    private static Context context;
    private static Toast toast;
    private static Handler main;

    static {
        init();
    }

    private static void init() {
        context = App.getContext();
        main = new Handler(Looper.getMainLooper());
    }

    public static void showToastFormat(int id, String s) {
        showToast(String.format(Locale.getDefault(), context.getString(id), s));
    }

    public static void showToast(final String mess) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    showToast(mess);
                }
            });
            return;
        }
        if (toast != null) toast.cancel();
        toast = Toast.makeText(context, mess, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showToast(final String tag, final String mess) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    showToast(tag, mess);
                }
            });
            return;
        }
        if (toast != null) toast.cancel();
        toast = Toast.makeText(context, tag + System.lineSeparator() + mess, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showToast(final String tag, final String mess, final String mess2) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    showToast(tag, mess, mess2);
                }
            });
            return;
        }
        if (toast != null) toast.cancel();
        toast = Toast.makeText(context, tag + System.lineSeparator() + mess + System.lineSeparator() + mess2, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showToast(int string) {
        String text = context.getString(string);
        showToast(text);
    }

    public static void showToast(int string, int string2) {
        String text = context.getString(string);
        String text2 = context.getString(string2);
        showToast(text, text2);
    }

    public static void showToast(String string, int string2) {
        String text2 = context.getString(string2);
        showToast(string, text2);
    }

    public static void showToast(int string, String string2) {
        String text = context.getString(string);
        showToast(text, string2);
    }

    public static void showToastWithTag(final int string, final int string2) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    showToastWithTag(string, string2);
                }
            });
            return;
        }
        String text = context.getString(string) + "\n" + context.getString(string2);
        Spannable centeredText = new SpannableString(text);
        centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, text.length() - 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        if (toast != null) toast.cancel();
        toast = Toast.makeText(context, centeredText, Toast.LENGTH_SHORT);
        toast.show();
    }
}
