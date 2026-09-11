package com.example.hourkiosk;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView clock;
    private DevicePolicyManager dpm;
    private ComponentName admin;
    private final Runnable ticker = new Runnable() {
        public void run() {
            clock.setText(new SimpleDateFormat("HH\nmm", Locale.getDefault()).format(new Date()));
            long delay = 1000 - System.currentTimeMillis() % 1000;
            handler.postDelayed(this, delay);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        dpm = (DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, KioskAdminReceiver.class);
        buildUi();
        applyImmersive();
        enterKiosk();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(120);
        clock.setGravity(Gravity.CENTER);
        clock.setIncludeFontPadding(false);
        root.addView(clock, new FrameLayout.LayoutParams(-1, -1));

        TextView exit = new TextView(this);
        exit.setText("×");
        exit.setTextColor(0x66FFFFFF);
        exit.setTextSize(16);
        exit.setGravity(Gravity.CENTER);
        int size = dp(36), margin = dp(6);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.END);
        p.setMargins(margin, margin, margin, margin);
        exit.setOnClickListener(v -> exitKiosk());
        root.addView(exit, p);
        setContentView(root);
    }

    private void enterKiosk() {
        if (dpm.isDeviceOwnerApp(getPackageName())) {
            dpm.setLockTaskPackages(admin, new String[]{getPackageName()});
            if (Build.VERSION.SDK_INT >= 28) dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
            try { dpm.setStatusBarDisabled(admin, true); } catch (Exception ignored) {}
            try { dpm.setKeyguardDisabled(admin, true); } catch (Exception ignored) {}
            startLockTask();
        } else {
            try { startLockTask(); } catch (Exception ignored) {}
        }
    }

    private void exitKiosk() {
        try { stopLockTask(); } catch (Exception ignored) {}
        if (dpm.isDeviceOwnerApp(getPackageName())) {
            try { dpm.setStatusBarDisabled(admin, false); } catch (Exception ignored) {}
            try { dpm.setKeyguardDisabled(admin, false); } catch (Exception ignored) {}
        }
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finishAndRemoveTask();
    }

    private void applyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if (focus) applyImmersive(); }
    @Override public void onBackPressed() {}
    @Override protected void onResume() { super.onResume(); ticker.run(); applyImmersive(); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(ticker); }
    @Override protected void onDestroy() { super.onDestroy(); handler.removeCallbacks(ticker); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
