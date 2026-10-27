package dz.vision.launcher1962;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Astronomical Solar Prayer Times Calculator.
 * Implements the official calculation method of the Algerian Ministry of
 * Religious Affairs and Wakfs (وزارة الشؤون الدينية والأوقاف الجزائرية):
 * - Fajr twilight angle: 18.0°
 * - Isha twilight angle: 17.0°
 * - Asr shadow ratio: 1.0 (Standard / Maliki / Shafi / Hanbali)
 * - Sunrise & Sunset zenith: 90.833°
 * - Timezone: Africa/Algiers (UTC+1)
 * Default location: Kaïs, Wilaya of Khenchela, Algeria (35.4925° N, 6.9344° E, ~930m elevation).
 */
public final class PrayerCalculator {

    public static final double KAIS_LATITUDE = 35.4925;
    public static final double KAIS_LONGITUDE = 6.9344;
    public static final String TIMEZONE_ID = "Africa/Algiers";

    public static final double FAJR_ANGLE = 18.0;
    public static final double ISHA_ANGLE = 17.0;
    public static final double ASR_FACTOR = 1.0;

    public static class PrayerTimes {
        public final Date fajr;
        public final Date sunrise;
        public final Date dhuhr;
        public final Date asr;
        public final Date maghrib;
        public final Date isha;

        public PrayerTimes(Date fajr, Date sunrise, Date dhuhr, Date asr, Date maghrib, Date isha) {
            this.fajr = fajr;
            this.sunrise = sunrise;
            this.dhuhr = dhuhr;
            this.asr = asr;
            this.maghrib = maghrib;
            this.isha = isha;
        }

        public Date getByIndex(int index) {
            switch (index) {
                case 0: return fajr;
                case 1: return sunrise;
                case 2: return dhuhr;
                case 3: return asr;
                case 4: return maghrib;
                case 5: return isha;
                default: return fajr;
            }
        }
    }

    public static PrayerTimes calculate(Calendar cal, double lat, double lng) {
        TimeZone tz = TimeZone.getTimeZone(TIMEZONE_ID);
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(cal.getTimeInMillis());

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);

        double timezoneOffsetHours = tz.getOffset(c.getTimeInMillis()) / 3600000.0;

        // Julian Date
        double jd = julianDate(year, month, day);
        double d = jd - 2451545.0;

        // Mean anomaly of the Sun
        double g = fixAngle(357.529 + 0.98560028 * d);
        // Mean longitude of the Sun
        double q = fixAngle(280.459 + 0.98564736 * d);
        // Ecliptic longitude
        double L = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g));

        // Obliquity of the ecliptic
        double e = 23.439 - 0.00000036 * d;
        // Right ascension in hours [0, 24)
        double ra = fixHour(Math.toDegrees(Math.atan2(cosDeg(e) * sinDeg(L), cosDeg(L))) / 15.0);
        // Declination in degrees
        double dec = Math.toDegrees(Math.asin(sinDeg(e) * sinDeg(L)));

        // Equation of Time in hours
        double eqTime = (q / 15.0) - ra;
        while (eqTime < -12.0) eqTime += 24.0;
        while (eqTime > 12.0) eqTime -= 24.0;

        // Solar Noon (Meridian transit)
        double noon = 12.0 + timezoneOffsetHours - (lng / 15.0) - eqTime;

        // Hour angles for twilight / horizon
        double fajrHa = hourAngle(-FAJR_ANGLE, lat, dec);
        double sunriseHa = hourAngle(-0.8333, lat, dec);
        double sunsetHa = sunriseHa;
        double ishaHa = hourAngle(-ISHA_ANGLE, lat, dec);

        // Asr hour angle (Maliki shadow factor = 1.0)
        double asrAlt = Math.toDegrees(Math.atan(1.0 / (ASR_FACTOR + Math.tan(Math.toRadians(Math.abs(lat - dec))))));
        double asrHa = hourAngle(asrAlt, lat, dec);

        // Prayer times in fractional hours
        double fajrTime = noon - fajrHa;
        double sunriseTime = noon - sunriseHa;
        double dhuhrTime = noon;
        double asrTime = noon + asrHa;
        // Elevation buffer for Kaïs (high plateau in Aurès mountains, ~930m)
        double maghribTime = noon + sunsetHa + (2.0 / 60.0);
        double ishaTime = noon + ishaHa;

        return new PrayerTimes(
                toDate(c, fajrTime),
                toDate(c, sunriseTime),
                toDate(c, dhuhrTime),
                toDate(c, asrTime),
                toDate(c, maghribTime),
                toDate(c, ishaTime)
        );
    }

    private static double julianDate(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    private static double hourAngle(double alpha, double lat, double dec) {
        double cosHa = (sinDeg(alpha) - sinDeg(lat) * sinDeg(dec)) / (cosDeg(lat) * cosDeg(dec));
        if (cosHa > 1.0) cosHa = 1.0;
        if (cosHa < -1.0) cosHa = -1.0;
        return Math.toDegrees(Math.acos(cosHa)) / 15.0;
    }

    private static double sinDeg(double deg) { return Math.sin(Math.toRadians(deg)); }
    private static double cosDeg(double deg) { return Math.cos(Math.toRadians(deg)); }

    private static double fixAngle(double a) {
        a = a - 360.0 * Math.floor(a / 360.0);
        return a < 0 ? a + 360.0 : a;
    }

    private static double fixHour(double h) {
        h = h - 24.0 * Math.floor(h / 24.0);
        return h < 0 ? h + 24.0 : h;
    }

    private static Date toDate(Calendar base, double hours) {
        Calendar c = (Calendar) base.clone();
        int h = (int) hours;
        double rem = (hours - h) * 60.0;
        int m = (int) Math.round(rem);
        if (m >= 60) {
            h += 1;
            m -= 60;
        }
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
