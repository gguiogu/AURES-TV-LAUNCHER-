package dz.vision.launcher1962;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateHelper {

    private static final String[] HIJRI_MONTHS_AR = {
            "", "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };

    public static String getGregorianDateString(Date date, Strings.Lang lang) {
        Locale loc = (lang == Strings.Lang.FRENCH) ? Locale.FRENCH :
                     (lang == Strings.Lang.ENGLISH) ? Locale.ENGLISH : new Locale("ar");
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d MMMM yyyy", loc);
        sdf.setTimeZone(TimeZone.getTimeZone("Africa/Algiers"));
        String s = sdf.format(date);
        if (lang == Strings.Lang.ARABIC) {
            s = normalizeArabicDigits(s);
        }
        return s;
    }

    public static String getHijriDateString(Date date, Strings.Lang lang) {
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"));
            c.setTime(date);
            LocalDate localDate = LocalDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
            HijrahDate hijrahDate = HijrahChronology.INSTANCE.date(localDate);
            int year = hijrahDate.get(ChronoField.YEAR);
            int month = hijrahDate.get(ChronoField.MONTH_OF_YEAR);
            int day = hijrahDate.get(ChronoField.DAY_OF_MONTH);

            String monthName = (month >= 1 && month <= 12) ? HIJRI_MONTHS_AR[month] : "";
            return day + " " + monthName + " " + year + " هـ  🌙";
        } catch (Throwable t) {
            // Fallback algorithm if java.time HijrahChronology is unavailable
            return fallbackHijri(date);
        }
    }

    private static String fallbackHijri(Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"));
        cal.setTime(date);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        int d = cal.get(Calendar.DAY_OF_MONTH);
        // Simple astronomical conversion
        double jd;
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        jd = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
        double l = Math.floor(jd - 1948440 + 10632);
        double n = Math.floor((l - 1) / 10631);
        l = l - 10631 * n + 354;
        double j = (Math.floor((10985 - l) / 5316)) * (Math.floor((50 * l) / 17719)) + (Math.floor(l / 5670)) * (Math.floor((43 * l) / 15238));
        l = l - (Math.floor((30 - j) / 15)) * (Math.floor((17719 * j) / 50)) - (Math.floor(j / 16)) * (Math.floor((15238 * j) / 43)) + 29;
        int hijriMonth = (int) Math.floor((24 * l) / 709);
        int hijriDay = (int) (l - Math.floor((709 * hijriMonth) / 24));
        int hijriYear = (int) (30 * n + j - 30);
        String monthName = (hijriMonth >= 1 && hijriMonth <= 12) ? HIJRI_MONTHS_AR[hijriMonth] : "";
        return hijriDay + " " + monthName + " " + hijriYear + " هـ  🌙";
    }

    public static String normalizeArabicDigits(String input) {
        if (input == null) return "";
        return input.replace('٠','0').replace('١','1').replace('٢','2')
                .replace('٣','3').replace('٤','4').replace('٥','5')
                .replace('٦','6').replace('٧','7').replace('٨','8')
                .replace('٩','9');
    }
}
