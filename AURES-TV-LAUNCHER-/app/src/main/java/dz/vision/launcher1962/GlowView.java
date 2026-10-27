package dz.vision.launcher1962;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * OLED Dynamic Ambient Glow Engine for 1962 Android TV Launcher.
 * tvOS-grade soft ambient backlighting behind focused app tiles.
 * - Multi-stop smooth radial diffusion (zero harsh edges, pure OLED black preserved).
 * - Subtle bottom floor ambient color wash.
 * - 240-280 ms cross-fade color transition.
 * - Fast procedural shader rendering with zero GPU memory overhead.
 */
public class GlowView extends View {

    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint washPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArgbEvaluator evaluator = new ArgbEvaluator();

    private int currentColor = 0xFF00C878; // Default Algerian Emerald Green
    private int targetColor = 0xFF00C878;

    private float currentTileX = 960f;
    private float currentTileY = 540f;
    private float targetTileX = 960f;
    private float targetTileY = 540f;

    private ValueAnimator colorAnimator;
    private ValueAnimator posAnimator;

    public GlowView(Context context) {
        super(context);
        haloPaint.setDither(true);
        washPaint.setDither(true);
    }

    /**
     * Animate glow color and position with a smooth 260 ms cross-fade.
     */
    public void setFocusTarget(int newColor, float tileCenterX, float tileCenterY) {
        if (colorAnimator != null && colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }
        if (posAnimator != null && posAnimator.isRunning()) {
            posAnimator.cancel();
        }

        final int fromColor = currentColor;
        targetColor = newColor;

        final float fromX = currentTileX;
        final float fromY = currentTileY;
        targetTileX = tileCenterX;
        targetTileY = tileCenterY;

        // 260 ms color cross-fade
        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(260);
        colorAnimator.setInterpolator(new DecelerateInterpolator());
        colorAnimator.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            currentColor = (int) evaluator.evaluate(f, fromColor, targetColor);
            invalidate();
        });
        colorAnimator.start();

        // 220 ms smooth position glide
        posAnimator = ValueAnimator.ofFloat(0f, 1f);
        posAnimator.setDuration(220);
        posAnimator.setInterpolator(new DecelerateInterpolator());
        posAnimator.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            currentTileX = fromX + (targetTileX - fromX) * f;
            currentTileY = fromY + (targetTileY - fromY) * f;
            invalidate();
        });
        posAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int r = Color.red(currentColor);
        int g = Color.green(currentColor);
        int b = Color.blue(currentColor);

        // 1. Soft Multi-Stop Radial Halo behind focused tile
        float density = getResources().getDisplayMetrics().density;
        float haloRadius = 240f * density;

        int c1 = Color.argb(72, r, g, b); // Soft core
        int c2 = Color.argb(36, r, g, b); // Mid diffusion
        int c3 = Color.argb(12, r, g, b); // Outer edge
        int c4 = Color.argb(0, r, g, b);  // Pure transparent (OLED black preserved)

        RadialGradient haloGradient = new RadialGradient(
                currentTileX, currentTileY,
                haloRadius,
                new int[]{c1, c2, c3, c4},
                new float[]{0.0f, 0.35f, 0.70f, 1.0f},
                Shader.TileMode.CLAMP
        );
        haloPaint.setShader(haloGradient);
        canvas.drawCircle(currentTileX, currentTileY, haloRadius, haloPaint);

        // 2. Subtle Bottom Ambient Wash (soft floor reflection)
        float washHeight = 150f * density;
        float washTop = h - washHeight;
        int washPeak = Color.argb(40, r, g, b);
        int washMid = Color.argb(14, r, g, b);
        int washZero = Color.argb(0, r, g, b);

        LinearGradient washGradient = new LinearGradient(
                0, washTop,
                0, h,
                new int[]{washZero, washMid, washPeak},
                new float[]{0.0f, 0.55f, 1.0f},
                Shader.TileMode.CLAMP
        );
        washPaint.setShader(washGradient);
        canvas.drawRect(0, washTop, w, h, washPaint);
    }
}
