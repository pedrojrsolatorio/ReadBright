package com.readbright.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ConfettiView extends View {

    private static final int PARTICLE_COUNT = 100;
    private static final long DURATION_MS = 2500;

    private static final int[] COLORS = {
            Color.parseColor("#FF1744"), // Red
            Color.parseColor("#FF9100"), // Orange
            Color.parseColor("#FFEA00"), // Yellow
            Color.parseColor("#00E676"), // Green
            Color.parseColor("#00B0FF"), // Blue
            Color.parseColor("#D500F9"), // Purple
            Color.parseColor("#FF4081")  // Pink
    };

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private static TextToSpeech fallbackTts;

    public ConfettiView(Context context) {
        super(context);
        init();
    }

    public ConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
    }

    public static void show(Activity activity) {
        show(activity, null);
    }

    public static void show(Activity activity, TextToSpeech tts) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        activity.runOnUiThread(() -> {
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            ConfettiView confettiView = new ConfettiView(activity);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            decorView.addView(confettiView, params);
            confettiView.startConfetti();

            speakCongratulations(activity, tts);
        });
    }

    private static void speakCongratulations(Context context, TextToSpeech activityTts) {
        if (activityTts != null) {
            activityTts.speak("Congratulations!", TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            if (fallbackTts == null) {
                fallbackTts = new TextToSpeech(context.getApplicationContext(), status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        fallbackTts.setLanguage(Locale.US);
                        fallbackTts.speak("Congratulations!", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                });
            } else {
                fallbackTts.speak("Congratulations!", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
    }

    private void startConfetti() {
        post(() -> {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                width = getResources().getDisplayMetrics().widthPixels;
                height = getResources().getDisplayMetrics().heightPixels;
            }

            Random random = new Random();
            particles.clear();
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                particles.add(new Particle(width, height, random));
            }

            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(DURATION_MS);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();
                for (Particle p : particles) {
                    p.update(progress);
                }
                invalidate();
            });

            animator.start();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (animator != null) animator.cancel();
                ViewGroup parent = (ViewGroup) getParent();
                if (parent != null) {
                    parent.removeView(ConfettiView.this);
                }
            }, DURATION_MS + 200);
        });
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (Particle p : particles) {
            p.draw(canvas, paint);
        }
    }

    private static class Particle {
        float x, y;
        float startX, startY;
        float vx, vy;
        float rotation, rotationSpeed;
        float size;
        int color;
        int shape;

        Particle(int screenWidth, int screenHeight, Random random) {
            startX = random.nextFloat() * screenWidth;
            startY = -random.nextFloat() * (screenHeight * 0.3f);
            x = startX;
            y = startY;

            vx = (random.nextFloat() - 0.5f) * screenWidth * 0.4f;
            vy = screenHeight * (0.8f + random.nextFloat() * 0.5f);

            rotation = random.nextFloat() * 360f;
            rotationSpeed = (random.nextFloat() - 0.5f) * 720f;

            size = 12f + random.nextFloat() * 18f;
            color = COLORS[random.nextInt(COLORS.length)];
            shape = random.nextInt(3);
        }

        void update(float progress) {
            x = startX + vx * progress;
            y = startY + vy * progress + 0.5f * 980f * progress * progress;
            rotation += rotationSpeed * 0.016f;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setColor(color);
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(rotation);

            if (shape == 0) {
                canvas.drawRect(-size / 2, -size / 4, size / 2, size / 4, paint);
            } else if (shape == 1) {
                canvas.drawCircle(0, 0, size / 3, paint);
            } else {
                canvas.drawOval(new RectF(-size / 2, -size / 3, size / 2, size / 3), paint);
            }

            canvas.restore();
        }
    }
}
