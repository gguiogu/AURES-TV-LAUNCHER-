package dz.vision.launcher1962;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;

/**
 * Handles incoming prayer alarms, system time change events, and Android 10 interruption.
 */
public class SalatReceiver extends BroadcastReceiver {

    public static final String PREF_SHOW_FULLSCREEN = "salat_show_fullscreen";
    private static final String CHANNEL_ID = "aures_salat_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (SalatAlarmScheduler.ACTION_SALAT_ALARM.equals(action)) {
            int prayerIndex = intent.getIntExtra(SalatAlarmScheduler.EXTRA_PRAYER_INDEX, 0);
            long scheduledTime = intent.getLongExtra(SalatAlarmScheduler.EXTRA_SCHEDULED_TIME, 0);
            long now = System.currentTimeMillis();

            // 0. Check Individual Prayer Enabled Toggle
            if (!SalatAlarmScheduler.isPrayerEnabled(context, prayerIndex)) {
                PrayerManager pm = new PrayerManager();
                SalatAlarmScheduler.scheduleNextPrayerAlarm(context, pm);
                return;
            }

            // 1. Check Late Policy (Prevent playing missed prayers when TV wakes up)
            SharedPreferences sp = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
            int lateMins = sp.getInt(SalatAlarmScheduler.PREF_LATE_POLICY_MINUTES, 5);
            long maxLateMs = lateMins * 60 * 1000L;

            if (scheduledTime > 0 && (now - scheduledTime) > maxLateMs) {
                // Device woke up too late, skip silently
                PrayerManager pm = new PrayerManager();
                SalatAlarmScheduler.scheduleNextPrayerAlarm(context, pm);
                return;
            }

            // 2. Check Idempotency (Prevent duplicate playback)
            if (SalatAlarmScheduler.isAlreadyAnnounced(context, prayerIndex, now)) {
                PrayerManager pm = new PrayerManager();
                SalatAlarmScheduler.scheduleNextPrayerAlarm(context, pm);
                return;
            }

            // 3. Mark announced atomically
            SalatAlarmScheduler.markAnnounced(context, prayerIndex, now);

            // 4. Wake up screen
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock wl = pm.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "1962:SalatWakeLock");
                wl.acquire(10000); // 10 seconds
            }

            // 4. Start Audio Interruption & Playback Immediately (Mutes music, pauses YouTube/media, plays ADHANALL.mp3)
            SalatSpeechManager.getInstance(context).announcePrayer(prayerIndex, null, null);

            boolean showFullscreen = sp.getBoolean(PREF_SHOW_FULLSCREEN, true);

            if (showFullscreen) {
                // 5. Launch Full-Screen Salat Mode Activity
                Intent salatIntent = new Intent(context, SalatActivity.class);
                salatIntent.putExtra(SalatAlarmScheduler.EXTRA_PRAYER_INDEX, prayerIndex);
                salatIntent.putExtra(SalatAlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime);
                salatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // Use Full-Screen Intent notification for robust Android 10 foreground interruption over YouTube / IPTV
                try {
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel channel = new NotificationChannel(
                                    CHANNEL_ID,
                                    "AURÈS TV Salat Interruption",
                                    NotificationManager.IMPORTANCE_HIGH);
                            channel.setDescription("Prayer time interruption and full-screen Adhan alert");
                            channel.setBypassDnd(true);
                            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                            nm.createNotificationChannel(channel);
                        }

                        PendingIntent pi = PendingIntent.getActivity(
                                context,
                                700 + prayerIndex,
                                salatIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
                        );

                        Notification.Builder notifBuilder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                ? new Notification.Builder(context, CHANNEL_ID)
                                : new Notification.Builder(context);

                        String prayerName = (prayerIndex >= 0 && prayerIndex < PrayerManager.NAMES_AR.length)
                                ? PrayerManager.NAMES_AR[prayerIndex] : "الصلاة";

                        Notification notification = notifBuilder
                                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                                .setContentTitle("حان الآن موعد صلاة " + prayerName)
                                .setContentText("AURÈS TV — رفع أذان صلاة " + prayerName)
                                .setFullScreenIntent(pi, true)
                                .setAutoCancel(true)
                                .setPriority(Notification.PRIORITY_MAX)
                                .build();

                        nm.notify(9000 + prayerIndex, notification);
                    }
                } catch (Throwable ignored) {}

                try {
                    context.startActivity(salatIntent);
                } catch (Throwable ignored) {}
            }

            // 6. Reschedule next prayer
            PrayerManager pManager = new PrayerManager();
            SalatAlarmScheduler.scheduleNextPrayerAlarm(context, pManager);

        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                   "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
                   Intent.ACTION_TIME_CHANGED.equals(action) ||
                   Intent.ACTION_TIMEZONE_CHANGED.equals(action) ||
                   Intent.ACTION_DATE_CHANGED.equals(action)) {
            
            PrayerManager pManager = new PrayerManager();
            SalatAlarmScheduler.scheduleNextPrayerAlarm(context, pManager);
        }
    }
}
