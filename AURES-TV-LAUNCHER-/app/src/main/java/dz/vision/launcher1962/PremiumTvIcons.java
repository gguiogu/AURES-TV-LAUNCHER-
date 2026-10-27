package dz.vision.launcher1962;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * World-Class Precision Glass Card Surface for 1962 Android TV Launcher.
 * tvOS-grade aesthetics: specular top-edge light reflection,
 * smooth squircle curvature, and GPU-efficient rendering.
 * All application icons are loaded exclusively from PackageManager.
 */
public final class PremiumTvIcons {

    public static Drawable createGlassCardDrawable(int width, int height, boolean isTv) {
        return new GlassCardDrawable(width, height, isTv);
    }

    /**
     * tvOS-Style Glass Card Surface with specular top-edge light reflection.
     */
    public static class GlassCardDrawable extends Drawable {
        private final int width;
        private final int height;
        private final boolean isTv;
        private boolean isFocused = false;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();

        public GlassCardDrawable(int width, int height, boolean isTv) {
            this.width = width;
            this.height = height;
            this.isTv = isTv;
            borderPaint.setStyle(Paint.Style.STROKE);
            rimPaint.setStyle(Paint.Style.STROKE);
            rimPaint.setStrokeWidth(1.2f);
        }

        public void setFocused(boolean focused) {
            if (this.isFocused != focused) {
                this.isFocused = focused;
                invalidateSelf();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            float w = getBounds().width() > 0 ? getBounds().width() : width;
            float h = getBounds().height() > 0 ? getBounds().height() : height;
            bounds.set(0, 0, w, h);
            float r = 20f; // Apple TV squircle corner radius

            // 1. Base Glass Surface Gradient
            LinearGradient bgGrad;
            if (isFocused) {
                if (isTv) {
                    // Focused TV: rich emerald dark glass
                    bgGrad = new LinearGradient(0, 0, 0, h, 0xFF0D3320, 0xFF061A10, Shader.TileMode.CLAMP);
                } else {
                    // Focused Card: lifted slate glass
                    bgGrad = new LinearGradient(0, 0, 0, h, 0xFF182230, 0xFF0F1620, Shader.TileMode.CLAMP);
                }
                borderPaint.setColor(0xFF00C878); // Algerian Emerald Green focus ring
                borderPaint.setStrokeWidth(2.4f);
            } else {
                // Unfocused: deep premium dark glass
                bgGrad = new LinearGradient(0, 0, 0, h, 0xFF111720, 0xFF0A0E14, Shader.TileMode.CLAMP);
                borderPaint.setColor(0xFF1E2834);
                borderPaint.setStrokeWidth(1.0f);
            }
            bgPaint.setShader(bgGrad);
            canvas.drawRoundRect(bounds, r, r, bgPaint);

            // 2. Specular Top-Edge Light Reflection (Apple TV Glass Look)
            float specH = h * 0.28f;
            LinearGradient specGrad = new LinearGradient(
                    0, 0, 0, specH,
                    isFocused ? 0x2AFFFFFF : 0x16FFFFFF,
                    0x00FFFFFF,
                    Shader.TileMode.CLAMP
            );
            specularPaint.setShader(specGrad);
            canvas.drawRoundRect(new RectF(1.5f, 1.5f, w - 1.5f, specH), r, r, specularPaint);

            // 3. Top Rim Subtle Highlight (1px line)
            rimPaint.setColor(isFocused ? 0x55FFFFFF : 0x26FFFFFF);
            canvas.drawLine(r, 1.2f, w - r, 1.2f, rimPaint);

            // 4. Outer Border
            canvas.drawRoundRect(bounds, r, r, borderPaint);
        }

        @Override public void setAlpha(int a) { bgPaint.setAlpha(a); }
        @Override public void setColorFilter(ColorFilter cf) { bgPaint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
