package com.example.hourkiosk;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH\nmm", Locale.getDefault());

    private TextView clock;
    private DevicePolicyManager dpm;
    private ComponentName admin;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (clock != null) clock.setText(timeFormat.format(new Date()));
            handler.postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );

        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, KioskAdminReceiver.class);

        buildUi();
        applyImmersiveMode();
        enterKioskMode();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(120);
        clock.setGravity(Gravity.CENTER);
        clock.setIncludeFontPadding(false);
        clock.setClickable(false);
        clock.setFocusable(false);

        root.addView(clock, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        TextView exitButton = new TextView(this);
        exitButton.setText("×");
        exitButton.setTextColor(0x66FFFFFF);
        exitButton.setTextSize(16);
        exitButton.setGravity(Gravity.CENTER);
        exitButton.setContentDescription("Exit kiosk");

        int size = dp(36);
        int margin = dp(6);

        FrameLayout.LayoutParams exitParams = new FrameLayout.LayoutParams(
            size,
            size,
            Gravity.TOP | Gravity.END
        );

        exitParams.setMargins(margin, margin, margin, margin);
        exitButton.setOnClickListener(view -> exitKioskMode());

        root.addView(exitButton, exitParams);
        setContentView(root);
    }

    private void enterKioskMode() {
        try {
            if (dpm.isDeviceOwnerApp(getPackageName())) {
                dpm.setLockTaskPackages(admin, new String[]{getPackageName()});

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.setLockTaskFeatures(
                        admin,
                        DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                    );
                }

                dpm.setStatusBarDisabled(admin, true);
                dpm.setKeyguardDisabled(admin, true);
            }

            startLockTask();
        } catch (Exception ignored) {
        }
    }

    private void exitKioskMode() {
        handler.removeCallbacks(ticker);

        try {
            stopLockTask();
        } catch (Exception ignored) {
        }

        if (dpm.isDeviceOwnerApp(getPackageName())) {
            try {
                dpm.setStatusBarDisabled(admin, false);
                dpm.setKeyguardDisabled(admin, false);
            } catch (Exception ignored) {
            }
        }

        finishAndRemoveTask();
    }

    private void applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();

            if (controller != null) {
                controller.hide(
                    WindowInsets.Type.statusBars() |
                    WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        ticker.run();
        applyImmersiveMode();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(
            value * getResources().getDisplayMetrics().density
        );
    }
}
