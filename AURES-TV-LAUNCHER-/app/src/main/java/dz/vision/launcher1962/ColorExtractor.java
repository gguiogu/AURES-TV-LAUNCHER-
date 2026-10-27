package dz.vision.launcher1962;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;

/**
 * Ultra-fast dominant color extractor for 1962 launcher tiles.
 * Downsamples app icon to 24x24 for 0-latency calculation on Mali-G31 GPU.
 */
public final class ColorExtractor {

    private static final int DEFAULT_GLOW = 0xFF007A3D; // Algerian Emerald Green
    private static final Map<String, Integer> CACHE = new HashMap<>();

    static {
        // Pre-seeded vibrant signature hues for popular TV apps
        CACHE.put("org.smarttube.stable", 0xFFFF3D00); // SmartTube Orange-Red
        CACHE.put("com.google.android.youtube.tv", 0xFFE50914); // YouTube Red
        CACHE.put("com.netflix.mediaclient", 0xFFE50914); // Netflix Red
        CACHE.put("net.mbc.shahidTV", 0xFF00B074); // Shahid Emerald
        CACHE.put("com.amazon.amazonvideo.livingroom", 0xFF00A8E1); // Prime Video Cyan
        CACHE.put("com.spotify.tv.android", 0xFF1DB954); // Spotify Green
        CACHE.put("com.disney.disneyplus", 0xFF113CCF); // Disney Blue
        CACHE.put("ver3.ycntivi.off", 0xFFE02020); // Yacine TV Red
        CACHE.put("com.ytv.pronew", 0xFF007A3D); // YTV Pro Emerald
        CACHE.put("cm.aptoidetv.pt", 0xFFFF6000); // Aptoide Orange
        CACHE.put("com.esaba.downloader", 0xFFFFA000); // Downloader Amber
        CACHE.put("com.yablio.sendfilestotv", 0xFF0088FF); // Send Files Blue
        CACHE.put("all_apps", 0xFF2979FF); // All apps Royal Blue
        CACHE.put("settings", 0xFF607D8B); // Settings Slate
    }

    public static int extract(String pkg, Drawable drawable) {
        if (pkg != null && CACHE.containsKey(pkg)) {
            return CACHE.get(pkg);
        }
        if (drawable == null) {
            return DEFAULT_GLOW;
        }

        try {
            int w = 24;
            int h = 24;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            drawable.setBounds(0, 0, w, h);
            drawable.draw(canvas);

            float maxSaturation = 0f;
            int bestColor = DEFAULT_GLOW;
            float[] hsv = new float[3];

            for (int y = 0; y < h; y += 2) {
                for (int x = 0; x < w; x += 2) {
                    int pixel = bmp.getPixel(x, y);
                    int a = Color.alpha(pixel);
                    if (a < 150) continue; // Skip semi-transparent pixels

                    int r = Color.red(pixel);
                    int g = Color.green(pixel);
                    int b = Color.blue(pixel);

                    int brightness = (r + g + b) / 3;
                    if (brightness < 35 || brightness > 230) continue; // Skip near black or washed out white

                    Color.colorToHSV(pixel, hsv);
                    float sat = hsv[1];
                    if (sat > maxSaturation && sat > 0.30f) {
                        maxSaturation = sat;
                        bestColor = pixel;
                    }
                }
            }
            bmp.recycle();

            if (pkg != null) {
                CACHE.put(pkg, bestColor);
            }
            return bestColor;
        } catch (Throwable t) {
            return DEFAULT_GLOW;
        }
    }
}
