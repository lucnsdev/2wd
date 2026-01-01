package lucns.robot2wd.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import lucns.robot2wd.R;

public abstract class BaseActivity extends Activity {

    public static class DialogClickCallback {

        public void onPositive() {
        }

        public void onNegative() {
        }
    }

    private boolean createdComplete, isPaused;
    private Dialog dialog;

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public Dialog generateDialog(int layoutId, boolean isCancelable) {
        hideDialog();
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(isCancelable);
        dialog.setContentView(layoutId);
        return dialog;
    }

    public Dialog generateDialog(int layoutId, int theme, boolean isCancelable) {
        hideDialog();
        dialog = new Dialog(this, theme);
        dialog.setCancelable(isCancelable);
        dialog.setContentView(layoutId);
        return dialog;
    }

    public void showDialogInfo(String title, DialogClickCallback dialogClickCallback) {
        hideDialog();
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_info);
        TextView textTitle = dialog.findViewById(R.id.textTitle);
        textTitle.setText(title);
        dialog.findViewById(R.id.buttonPositive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (dialogClickCallback != null) dialogClickCallback.onPositive();
            }
        });
        dialog.show();
    }

    public void showDialogConfirm(String title, DialogClickCallback dialogClickCallback) {
        hideDialog();
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (v.getId() == R.id.buttonNegative) {
                    dialogClickCallback.onNegative();
                } else if (v.getId() == R.id.buttonPositive) {
                    dialogClickCallback.onPositive();
                }
            }
        };
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_confirm);
        TextView textTitle = dialog.findViewById(R.id.textTitle);
        textTitle.setText(title);
        dialog.findViewById(R.id.buttonPositive).setOnClickListener(onClickListener);
        dialog.show();
    }

    public void hideDialog() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    public abstract boolean onCreated();

    public void onResumed() {
    }

    public void onPaused() {
    }

    public void onDestroyed() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createdComplete = onCreated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        if (createdComplete) onResumed();
    }

    public boolean isPaused() {
        return isPaused;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        if (createdComplete) onPaused();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        if (createdComplete) onDestroyed();
    }

    public void startActivity(Intent intent) {
        if (isFinishing() || isDestroyed()) return;
        super.startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
