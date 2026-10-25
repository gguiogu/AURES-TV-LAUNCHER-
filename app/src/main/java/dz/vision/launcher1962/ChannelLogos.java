package dz.vision.launcher1962;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Premium Algerian Television Channel Badges.
 * High-definition vector rendering matching official broadcast identities.
 * ENTV, Canal Algérie, Echorouk TV, Ennahar TV.
 */
public final class ChannelLogos {

    public static ChannelBadgeDrawable createChannelBadge(String channelId, int width, int height) {
        return new ChannelBadgeDrawable(channelId, width, height);
    }

    public static class ChannelBadgeDrawable extends Drawable {
        private final String id;
        private final int w;
        private final int h;
        private boolean isFocused = false;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private final Path path = new Path();

        public ChannelBadgeDrawable(String id, int w, int h) {
            this.id = id;
            this.w = w;
            this.h = h;

            borderPaint.setStyle(Paint.Style.STROKE);
            textPaint.setColor(Color.WHITE);
        }

        public void setFocused(boolean focused) {
            if (this.isFocused != focused) {
                this.isFocused = focused;
                invalidateSelf();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            float width = getBounds().width() > 0 ? getBounds().width() : w;
            float height = getBounds().height() > 0 ? getBounds().height() : h;
            bounds.set(0, 0, width, height);
            float r = 16f;

            // Border color & thickness based on focus
            if (isFocused) {
                borderPaint.setColor(0xFF00C878); // Algerian Emerald Green
                borderPaint.setStrokeWidth(2.2f);
            } else {
                borderPaint.setColor(0x33FFFFFF);
                borderPaint.setStrokeWidth(1.0f);
            }

            if (ChannelsHelper.CH_ENTV.equals(id)) {
                // 1. ENTV — Deep Mediterranean Blue Gradient
                LinearGradient bgGrad = new LinearGradient(
                        0, 0, 0, height,
                        isFocused ? 0xFF103D7C : 0xFF0B2B58,
                        isFocused ? 0xFF19559E : 0xFF13427C,
                        Shader.TileMode.CLAMP
                );
                bgPaint.setShader(bgGrad);
                canvas.drawRoundRect(bounds, r, r, bgPaint);

                // Stylized ENTV White/Cyan Globe with Dynamic Orbit
                float iconCx = width * 0.28f;
                float iconCy = height * 0.50f;
                float globeR = height * 0.26f;

                iconPaint.reset();
                iconPaint.setAntiAlias(true);
                iconPaint.setColor(0x22FFFFFF);
                iconPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(iconCx, iconCy, globeR * 1.15f, iconPaint);

                iconPaint.setColor(Color.WHITE);
                iconPaint.setStyle(Paint.Style.STROKE);
                iconPaint.setStrokeWidth(2.4f);
                canvas.drawCircle(iconCx, iconCy, globeR, iconPaint);

                // Dynamic Cyan Orbit Arc
                iconPaint.setColor(0xFF00E5FF); // Bright Cyan
                iconPaint.setStrokeWidth(2.6f);
                RectF orbitRect = new RectF(iconCx - globeR * 1.35f, iconCy - globeR * 0.65f, iconCx + globeR * 1.35f, iconCy + globeR * 0.65f);
                canvas.drawArc(orbitRect, 25, 220, false, iconPaint);

                // "ENTV" Bold Pure White Typography
                textPaint.setColor(Color.WHITE);
                textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
                textPaint.setTextSize(height * 0.44f);
                textPaint.setTextAlign(Paint.Align.LEFT);
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float textY = (height / 2f) - (fm.ascent + fm.descent) / 2f;
                canvas.drawText("ENTV", width * 0.46f, textY, textPaint);

            } else if (ChannelsHelper.CH_CANAL_ALGERIE.equals(id)) {
                // 2. Canal Algérie — Rich Emerald Green Gradient
                LinearGradient bgGrad = new LinearGradient(
                        0, 0, 0, height,
                        isFocused ? 0xFF005E28 : 0xFF00441C,
                        isFocused ? 0xFF008E44 : 0xFF006B32,
                        Shader.TileMode.CLAMP
                );
                bgPaint.setShader(bgGrad);
                canvas.drawRoundRect(bounds, r, r, bgPaint);

                // Stylized Crescent & Star Accent
                float iconCx = width * 0.18f;
                float iconCy = height * 0.50f;

                iconPaint.reset();
                iconPaint.setAntiAlias(true);
                iconPaint.setStyle(Paint.Style.FILL);

                // Red Crescent
                iconPaint.setColor(0xFFD21034); // Algerian Red
                canvas.drawCircle(iconCx, iconCy, height * 0.23f, iconPaint);
                iconPaint.setColor(isFocused ? 0xFF005E28 : 0xFF00441C);
                canvas.drawCircle(iconCx + 4f, iconCy, height * 0.17f, iconPaint);

                // Red Star
                iconPaint.setColor(0xFFD21034);
                canvas.drawCircle(iconCx + 7.5f, iconCy, 3.8f, iconPaint);

                // "Canal Algérie" Typography (proportioned to fit badge bounds cleanly)
                textPaint.setColor(Color.WHITE);
                textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                textPaint.setTextSize(height * 0.24f);
                textPaint.setTextAlign(Paint.Align.LEFT);
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float textY = (height / 2f) - (fm.ascent + fm.descent) / 2f;
                canvas.drawText("Canal Algérie", width * 0.32f, textY, textPaint);

            } else if (ChannelsHelper.CH_ECHOROUK.equals(id)) {
                // 3. Echorouk TV — Midnight Navy Gradient
                LinearGradient bgGrad = new LinearGradient(
                        0, 0, 0, height,
                        isFocused ? 0xFF0F2648 : 0xFF0A182E,
                        isFocused ? 0xFF183D6F : 0xFF10284A,
                        Shader.TileMode.CLAMP
                );
                bgPaint.setShader(bgGrad);
                canvas.drawRoundRect(bounds, r, r, bgPaint);

                // "TV" Badge Pill on left
                RectF tvPill = new RectF(width * 0.12f, height * 0.26f, width * 0.34f, height * 0.74f);
                iconPaint.reset();
                iconPaint.setAntiAlias(true);
                iconPaint.setStyle(Paint.Style.FILL);
                iconPaint.setColor(0x35FFFFFF);
                canvas.drawRoundRect(tvPill, 8f, 8f, iconPaint);

                textPaint.setColor(Color.WHITE);
                textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
                textPaint.setTextSize(height * 0.28f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float pillTextY = (height / 2f) - (fm.ascent + fm.descent) / 2f;
                canvas.drawText("TV", tvPill.centerX(), pillTextY, textPaint);

                // "الشروق" Arabic Typography
                textPaint.setTextSize(height * 0.40f);
                textPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("الشروق", width * 0.88f, pillTextY + 1f, textPaint);

            } else if (ChannelsHelper.CH_ENNAHAR.equals(id)) {
                // 4. Ennahar TV — Vibrant Red/Orange Gradient
                LinearGradient bgGrad = new LinearGradient(
                        0, 0, 0, height,
                        isFocused ? 0xFFE64A19 : 0xFFC5390F,
                        isFocused ? 0xFFFF5722 : 0xFFE64A19,
                        Shader.TileMode.CLAMP
                );
                bgPaint.setShader(bgGrad);
                canvas.drawRoundRect(bounds, r, r, bgPaint);

                // "TV" Dark Pill
                RectF tvPill = new RectF(width * 0.12f, height * 0.26f, width * 0.34f, height * 0.74f);
                iconPaint.reset();
                iconPaint.setAntiAlias(true);
                iconPaint.setStyle(Paint.Style.FILL);
                iconPaint.setColor(0x40000000);
                canvas.drawRoundRect(tvPill, 8f, 8f, iconPaint);

                textPaint.setColor(Color.WHITE);
                textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
                textPaint.setTextSize(height * 0.28f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float pillTextY = (height / 2f) - (fm.ascent + fm.descent) / 2f;
                canvas.drawText("TV", tvPill.centerX(), pillTextY, textPaint);

                // "النهار" Arabic Typography
                textPaint.setTextSize(height * 0.40f);
                textPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("النهار", width * 0.88f, pillTextY + 1f, textPaint);
            }

            // Top specular gloss
            float specH = height * 0.35f;
            LinearGradient specGrad = new LinearGradient(0, 0, 0, specH, 0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
            specularPaint.setShader(specGrad);
            canvas.drawRoundRect(new RectF(1.5f, 1.5f, width - 1.5f, specH), r, r, specularPaint);

            // Outer Border
            canvas.drawRoundRect(bounds, r, r, borderPaint);
        }

        @Override public void setAlpha(int a) { bgPaint.setAlpha(a); }
        @Override public void setColorFilter(ColorFilter cf) { bgPaint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
