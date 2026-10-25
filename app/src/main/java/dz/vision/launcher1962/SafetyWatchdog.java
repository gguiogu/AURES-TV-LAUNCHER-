package dz.vision.launcher1962;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Safety Watchdog for 1962 Launcher:
 * 1. Automatic Fallback: If launcher crashes 3 times within 5 minutes (300,000 ms),
 *    it restores the stock/alternative launcher to prevent TV box lock-out.
 * 2. Manual Exit: "Back to original launcher" setting restores the previous launcher.
 */
public final class SafetyWatchdog {

    private static final String TAG = "SafetyWatchdog";
    private static final String PREF_NAME = "safety_watchdog_prefs";
    private static final String KEY_CRASH_TIMESTAMPS = "crash_timestamps";
    private static final int MAX_CRASHES = 3;
    private static final long TIME_WINDOW_MS = 5 * 60 * 1000; // 5 minutes

    public static void init(final Context context) {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e(TAG, "Uncaught exception in 1962 launcher", throwable);
                recordCrash(context);
                if (shouldTriggerFallback(context)) {
                    Log.w(TAG, "3 crashes within 5 minutes detected! Triggering automatic fallback to stock launcher.");
                    launchStockLauncher(context);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in watchdog handler", t);
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        // If launcher runs stably for 15 seconds, prune expired crashes
        new Handler(Looper.getMainLooper()).postDelayed(() -> pruneCrashes(context), 15000);
    }

    private static synchronized void recordCrash(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String existing = sp.getString(KEY_CRASH_TIMESTAMPS, "");
        long now = System.currentTimeMillis();
        String updated = existing.isEmpty() ? String.valueOf(now) : existing + "," + now;
        sp.edit().putString(KEY_CRASH_TIMESTAMPS, updated).commit();
    }

    private static synchronized boolean shouldTriggerFallback(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String data = sp.getString(KEY_CRASH_TIMESTAMPS, "");
        if (data.isEmpty()) return false;

        String[] parts = data.split(",");
        long now = System.currentTimeMillis();
        int recentCount = 0;
        for (String p : parts) {
            try {
                long t = Long.parseLong(p.trim());
                if (now - t <= TIME_WINDOW_MS) {
                    recentCount++;
                }
            } catch (Exception ignored) {}
        }
        return recentCount >= MAX_CRASHES;
    }

    private static synchronized void pruneCrashes(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String data = sp.getString(KEY_CRASH_TIMESTAMPS, "");
        if (data.isEmpty()) return;

        String[] parts = data.split(",");
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            try {
                long t = Long.parseLong(p.trim());
                if (now - t <= TIME_WINDOW_MS) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(t);
                }
            } catch (Exception ignored) {}
        }
        sp.edit().putString(KEY_CRASH_TIMESTAMPS, sb.toString()).apply();
    }

    /**
     * Finds and launches an alternative/stock HOME launcher.
     */
    public static void launchStockLauncher(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> list = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        String currentPkg = context.getPackageName();
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            if (!pkg.equals(currentPkg)) {
                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.setComponent(new ComponentName(pkg, ri.activityInfo.name));
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(launch);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start fallback launcher " + pkg, e);
                }
            }
        }

        // If no explicit alternative found, open Android Home settings
        try {
            Intent settingsIntent = new Intent(Settings.ACTION_HOME_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
        } catch (Exception e) {
            Intent anySettings = new Intent(Settings.ACTION_SETTINGS);
            anySettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(anySettings);
        }
    }
}
