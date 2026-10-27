package dz.vision.launcher1962;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Manages daily prayer schedule, real-time transitions, and next-prayer countdown.
 * Bound to Africa/Algiers timezone and Kaïs, Khenchela coordinates.
 */
public class PrayerManager {

    public static final String[] NAMES_AR = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    public static final String[] NAMES_FR = {"Fajr", "Chourouk", "Dhohr", "Asr", "Maghreb", "Icha"};
    public static final String[] NAMES_EN = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    public static final String[] NAMES_TZ = {"ⵜⴰⴼⴰⵡⵜ", "ⴰⵙⵏⵇⵔ", "ⵜⵉⵣⵡⴰⵔⵏⵉⵏ", "ⵜⴰⴽⵡⵥⵉⵏ", "ⵜⵉⵏⵡⵓⵜⵛⵉ", "ⵜⵉⵢⵢⵉⴹⵙ"};

    private final SimpleDateFormat timeFormat;
    private final TimeZone timeZone;

    private double latitude = PrayerCalculator.KAIS_LATITUDE;
    private double longitude = PrayerCalculator.KAIS_LONGITUDE;

    private int cachedYear = -1;
    private int cachedMonth = -1;
    private int cachedDay = -1;

    private PrayerCalculator.PrayerTimes todayPrayers;
    private PrayerCalculator.PrayerTimes tomorrowPrayers;
    private final String[] formattedTimes = new String[6];

    private int activeIndex = -1;
    private int nextIndex = -1;
    private long nextTargetMs = 0;
    private long countdownMs = 0;

    public PrayerManager() {
        timeZone = TimeZone.getTimeZone(PrayerCalculator.TIMEZONE_ID);
        timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        timeFormat.setTimeZone(timeZone);
    }

    public synchronized void update(long nowMs) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(nowMs);

        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        // Recalculate schedule if day changed or initial run
        if (todayPrayers == null || y != cachedYear || m != cachedMonth || d != cachedDay) {
            cachedYear = y;
            cachedMonth = m;
            cachedDay = d;

            todayPrayers = PrayerCalculator.calculate(cal, latitude, longitude);

            Calendar nextDayCal = (Calendar) cal.clone();
            nextDayCal.add(Calendar.DAY_OF_MONTH, 1);
            tomorrowPrayers = PrayerCalculator.calculate(nextDayCal, latitude, longitude);

            for (int i = 0; i < 6; i++) {
                formattedTimes[i] = timeFormat.format(todayPrayers.getByIndex(i));
            }
        }

        long t0 = todayPrayers.fajr.getTime();
        long t1 = todayPrayers.sunrise.getTime();
        long t2 = todayPrayers.dhuhr.getTime();
        long t3 = todayPrayers.asr.getTime();
        long t4 = todayPrayers.maghrib.getTime();
        long t5 = todayPrayers.isha.getTime();
        long tNextFajr = tomorrowPrayers.fajr.getTime();

        if (nowMs < t0) {
            // After midnight until Fajr:
            // Displayed schedule is today's schedule.
            // Active prayer period is Isha (night period before dawn).
            // Next prayer is today's Fajr.
            activeIndex = 5;
            nextIndex = 0;
            nextTargetMs = t0;
        } else if (nowMs < t1) {
            // Fajr started
            activeIndex = 0;
            nextIndex = 1;
            nextTargetMs = t1;
        } else if (nowMs < t2) {
            // Sunrise started
            activeIndex = 1;
            nextIndex = 2;
            nextTargetMs = t2;
        } else if (nowMs < t3) {
            // Dhuhr started
            activeIndex = 2;
            nextIndex = 3;
            nextTargetMs = t3;
        } else if (nowMs < t4) {
            // Asr started
            activeIndex = 3;
            nextIndex = 4;
            nextTargetMs = t4;
        } else if (nowMs < t5) {
            // Maghrib started
            activeIndex = 4;
            nextIndex = 5;
            nextTargetMs = t5;
        } else {
            // Isha started
            activeIndex = 5;
            nextIndex = 0;
            nextTargetMs = tNextFajr;
        }

        countdownMs = Math.max(0, nextTargetMs - nowMs);
    }

    public synchronized int getActiveIndex() {
        return activeIndex;
    }

    public synchronized int getNextIndex() {
        return nextIndex;
    }

    public synchronized long getCountdownMs() {
        return countdownMs;
    }

    public synchronized String getFormattedTime(int index) {
        if (index >= 0 && index < 6) {
            return formattedTimes[index];
        }
        return "--:--";
    }

    public synchronized String getPrayerName(int index, Strings.Lang lang) {
        if (index < 0 || index >= 6) return "";
        switch (lang) {
            case FRENCH: return NAMES_FR[index];
            case ENGLISH: return NAMES_EN[index];
            case TAMAZIGHT: return NAMES_TZ[index];
            case ARABIC:
            default:
                return NAMES_AR[index];
        }
    }

    public synchronized String getFormattedCountdown() {
        long totalSec = countdownMs / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    public synchronized String getNextPrayerCountdownLabel(Strings.Lang lang) {
        String nextName = getPrayerName(nextIndex, lang);
        String countdown = getFormattedCountdown();
        switch (lang) {
            case FRENCH:
                return nextName + ": " + countdown;
            case ENGLISH:
                return nextName + ": " + countdown;
            case TAMAZIGHT:
                return nextName + ": " + countdown;
            case ARABIC:
            default:
                return nextName + ": " + countdown;
        }
    }

    public void setLocation(double lat, double lng) {
        this.latitude = lat;
        this.longitude = lng;
        this.todayPrayers = null; // force recalculation
    }

    public synchronized double getLatitude() {
        return latitude;
    }

    public synchronized double getLongitude() {
        return longitude;
    }
}
