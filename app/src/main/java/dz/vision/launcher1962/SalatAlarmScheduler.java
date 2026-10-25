package dz.vision.launcher1962;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Exact AlarmManager scheduler for daily prayers.
 * Ensures:
 * - Exact scheduling with setAlarmClock / setExactAndAllowWhileIdle.
 * - Individual toggles for each of the five prayers.
 * - Idempotency (lastAnnouncedKey = yyyy-MM-dd:prayer_index).
 * - Late policy (default within 5 min).
 * - Survives reboot and clock adjustments.
 */
public final class SalatAlarmScheduler {

    public static final String ACTION_SALAT_ALARM = "dz.vision.launcher1962.ACTION_SALAT_ALARM";
    public static final String EXTRA_PRAYER_INDEX = "prayer_index";
    public static final String EXTRA_SCHEDULED_TIME = "scheduled_time_ms";

    public static final String PREF_LAST_ANNOUNCED_KEY = "salat_last_announced_key";
    public static final String PREF_SALAT_ENABLED = "salat_mode_enabled";
    public static final String PREF_LATE_POLICY_MINUTES = "salat_late_policy_minutes";

    // Individual prayer alarm toggles (all default to true)
    public static final String PREF_ALARM_FAJR = "salat_alarm_fajr";
    public static final String PREF_ALARM_DHUHR = "salat_alarm_dhuhr";
    public static final String PREF_ALARM_ASR = "salat_alarm_asr";
    public static final String PREF_ALARM_MAGHRIB = "salat_alarm_maghrib";
    public static final String PREF_ALARM_ISHA = "salat_alarm_isha";

    private static final SimpleDateFormat KEY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    static {
        KEY_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(PrayerCalculator.TIMEZONE_ID));
    }

    public static boolean isPrayerEnabled(Context context, int prayerIndex) {
        SharedPreferences sp = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        switch (prayerIndex) {
            case 0: return sp.getBoolean(PREF_ALARM_FAJR, true);
            case 2: return sp.getBoolean(PREF_ALARM_DHUHR, true);
            case 3: return sp.getBoolean(PREF_ALARM_ASR, true);
            case 4: return sp.getBoolean(PREF_ALARM_MAGHRIB, true);
            case 5: return sp.getBoolean(PREF_ALARM_ISHA, true);
            default: return false; // Sunrise (1) is not an announced prayer
        }
    }

    public static void setPrayerEnabled(Context context, int prayerIndex, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        switch (prayerIndex) {
            case 0: ed.putBoolean(PREF_ALARM_FAJR, enabled); break;
            case 2: ed.putBoolean(PREF_ALARM_DHUHR, enabled); break;
            case 3: ed.putBoolean(PREF_ALARM_ASR, enabled); break;
            case 4: ed.putBoolean(PREF_ALARM_MAGHRIB, enabled); break;
            case 5: ed.putBoolean(PREF_ALARM_ISHA, enabled); break;
        }
        ed.apply();
    }

    /**
     * Reschedule the next upcoming prayer alarm based on calculation and enabled states.
     */
    public static synchronized void scheduleNextPrayerAlarm(Context context, PrayerManager prayerManager) {
        SharedPreferences sp = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = sp.getBoolean(PREF_SALAT_ENABLED, true);
        if (!enabled) {
            cancelAlarms(context);
            return;
        }

        long nowMs = System.currentTimeMillis();
        TimeZone tz = TimeZone.getTimeZone(PrayerCalculator.TIMEZONE_ID);

        // Calculate today and tomorrow's prayer times
        Calendar todayCal = Calendar.getInstance(tz);
        todayCal.setTimeInMillis(nowMs);
        PrayerCalculator.PrayerTimes todayPt = PrayerCalculator.calculate(
                todayCal, prayerManager.getLatitude(), prayerManager.getLongitude());

        Calendar tomorrowCal = (Calendar) todayCal.clone();
        tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
        PrayerCalculator.PrayerTimes tomorrowPt = PrayerCalculator.calculate(
                tomorrowCal, prayerManager.getLatitude(), prayerManager.getLongitude());

        // The 5 prayers of interest: 0 (Fajr), 2 (Dhuhr), 3 (Asr), 4 (Maghrib), 5 (Isha)
        int[] prayerIndices = {0, 2, 3, 4, 5};
        int selectedIndex = -1;
        long selectedTargetMs = 0;

        // 1. Check remaining enabled prayers for today
        for (int idx : prayerIndices) {
            long pTime = todayPt.getByIndex(idx).getTime();
            if (pTime > nowMs && isPrayerEnabled(context, idx)) {
                selectedIndex = idx;
                selectedTargetMs = pTime;
                break;
            }
        }

        // 2. If no remaining enabled prayer today, check tomorrow
        if (selectedIndex == -1) {
            for (int idx : prayerIndices) {
                if (isPrayerEnabled(context, idx)) {
                    selectedIndex = idx;
                    selectedTargetMs = tomorrowPt.getByIndex(idx).getTime();
                    break;
                }
            }
        }

        // If all prayers are disabled
        if (selectedIndex == -1 || selectedTargetMs <= 0) {
            cancelAlarms(context);
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        boolean showFullscreen = sp.getBoolean(SalatReceiver.PREF_SHOW_FULLSCREEN, true);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi;
        if (showFullscreen) {
            Intent salatIntent = new Intent(context, SalatActivity.class);
            salatIntent.putExtra(EXTRA_PRAYER_INDEX, selectedIndex);
            salatIntent.putExtra(EXTRA_SCHEDULED_TIME, selectedTargetMs);
            salatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pi = PendingIntent.getActivity(context, 1962, salatIntent, flags);
        } else {
            Intent intent = new Intent(context, SalatReceiver.class);
            intent.setAction(ACTION_SALAT_ALARM);
            intent.putExtra(EXTRA_PRAYER_INDEX, selectedIndex);
            intent.putExtra(EXTRA_SCHEDULED_TIME, selectedTargetMs);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            pi = PendingIntent.getBroadcast(context, 1962, intent, flags);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // setAlarmClock is treated as user-visible and exact
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(selectedTargetMs, pi);
                am.setAlarmClock(info, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, selectedTargetMs, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, selectedTargetMs, pi);
            }
        } catch (Throwable t) {
            am.set(AlarmManager.RTC_WAKEUP, selectedTargetMs, pi);
        }
    }

    public static void cancelAlarms(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, SalatReceiver.class);
        intent.setAction(ACTION_SALAT_ALARM);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 1962, intent, flags);
        am.cancel(pi);
    }

    /**
     * Check idempotency: returns true if this prayer has already been announced today.
     */
    public static boolean isAlreadyAnnounced(Context context, int prayerIndex, long nowMs) {
        SharedPreferences sp = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String lastKey = sp.getString(PREF_LAST_ANNOUNCED_KEY, "");
        String currentKey = buildKey(prayerIndex, nowMs);
        return currentKey.equals(lastKey);
    }

    /**
     * Mark prayer as announced atomically before starting playback.
     */
    public static void markAnnounced(Context context, int prayerIndex, long nowMs) {
        SharedPreferences sp = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String key = buildKey(prayerIndex, nowMs);
        sp.edit().putString(PREF_LAST_ANNOUNCED_KEY, key).apply();
    }

    private static String buildKey(int prayerIndex, long timeMs) {
        Date d = new Date(timeMs);
        return KEY_DATE_FORMAT.format(d) + ":" + prayerIndex;
    }
}
