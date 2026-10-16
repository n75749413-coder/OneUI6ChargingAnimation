package com.nazar.charginganimation;

import android.app.Activity;
import android.os.Bundle;
import android.os.BatteryManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.animation.ValueAnimator;

public class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private ChargingView chargingView;
    private BroadcastReceiver batteryReceiver;
    private int percent = 0;
    private boolean receiverRegistered = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        Intent sticky = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (sticky != null) updateBattery(sticky);

        chargingView = new ChargingView(this);
        chargingView.setPercent(percent);
        setContentView(chargingView);
        chargingView.startAnimation();

        batteryReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                updateBattery(intent);
                if (chargingView != null) chargingView.setPercent(percent);
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        receiverRegistered = true;
        handler.postDelayed(() -> finish(), 3000L);
    }

    private void updateBattery(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level >= 0 && scale > 0) percent = Math.max(0, Math.min(100, Math.round(level * 100f / scale)));
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered && batteryReceiver != null) {
            try { unregisterReceiver(batteryReceiver); } catch (IllegalArgumentException ignored) { }
        }
        super.onDestroy();
    }

    private class ChargingView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path bolt = new Path();
        private float rotation = 0f;
        private float appear = 0f;
        private int batteryPercent = 0;
        private final int green = Color.rgb(39, 220, 100);

        ChargingView(Context context) { super(context); setBackgroundColor(Color.BLACK); }

        void setPercent(int value) { batteryPercent = value; invalidate(); }

        void startAnimation() {
            ValueAnimator a = ValueAnimator.ofFloat(0f, 360f);
            a.setDuration(1800L);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setInterpolator(new DecelerateInterpolator());
            a.addUpdateListener(v -> { rotation = (float) v.getAnimatedValue(); invalidate(); });
            a.start();
            ValueAnimator b = ValueAnimator.ofFloat(0f, 1f);
            b.setDuration(420L);
            b.addUpdateListener(v -> { appear = (float) v.getAnimatedValue(); invalidate(); });
            b.start();
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float scale = Math.min(getWidth(), getHeight()) / 390f;
            c.save(); c.translate(cx, cy); c.scale(scale, scale);

            p.setAlpha(255); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3.5f);
            p.setStrokeCap(Paint.Cap.ROUND); p.setColor(Color.rgb(38, 67, 48));
            c.drawCircle(0, 0, 48, p);

            p.setColor(green); p.setStrokeWidth(3.8f);
            c.save(); c.rotate(rotation);
            c.drawArc(-48, -48, 48, 48, -92, 285, false, p);
            c.restore();

            p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE);
            p.setAlpha((int)(255 * appear));
            p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            p.setTextAlign(Paint.Align.CENTER); p.setTextSize(28f);
            c.drawText(batteryPercent + "%", 0, 10, p);

            bolt.reset();
            bolt.moveTo(7, -72); bolt.lineTo(-8, -52); bolt.lineTo(1, -52);
            bolt.lineTo(-5, -36); bolt.lineTo(13, -59); bolt.lineTo(4, -59); bolt.close();
            p.setColor(Color.WHITE); p.setAlpha((int)(255 * appear));
            c.drawPath(bolt, p);
            c.restore();
        }
    }
}
