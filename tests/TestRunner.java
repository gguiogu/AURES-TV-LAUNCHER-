package tests;

import dz.vision.launcher1962.AlgerianCities;
import dz.vision.launcher1962.DateHelper;
import dz.vision.launcher1962.PrayerCalculator;
import dz.vision.launcher1962.PrayerManager;
import dz.vision.launcher1962.Strings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TestRunner {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("   AURÃˆS TV (1962) â€” Automated Test Suite Verification    ");
        System.out.println("==========================================================");

        testAlgerianCities();
        testPrayerCalculatorGoldenReference();
        testPrayerCalculatorWilayas();
        testDateHelper();
        testStringsLocalization();

        System.out.println("==========================================================");
        System.out.println("Test Results: " + passedTests + " passed, " + failedTests + " failed, total: " + totalTests);
        if (failedTests == 0) {
            System.out.println("ALL TESTS PASSED! (100% Verification Rate)");
            System.out.println("==========================================================");
            System.exit(0);
        } else {
            System.out.println("SOME TESTS FAILED!");
            System.out.println("==========================================================");
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
        } else {
            failedTests++;
            System.err.println("  [FAIL] " + testName);
        }
    }

    private static void testAlgerianCities() {
        System.out.println("\n[Suite 1] Algerian Wilayas Directory");
        assertTrue("Cities count is exactly 15", AlgerianCities.CITIES != null && AlgerianCities.CITIES.length == 15);

        AlgerianCities.City defaultCity = AlgerianCities.getDefaultCity();
        assertTrue("Default city is Khenchela / KaÃ¯s", defaultCity != null && defaultCity.nameEn.contains("Khenchela"));
        assertTrue("KaÃ¯s latitude is ~35.49Â° N", Math.abs(defaultCity.latitude - 35.4925) < 0.001);
        assertTrue("KaÃ¯s longitude is ~6.93Â° E", Math.abs(defaultCity.longitude - 6.9344) < 0.001);

        for (int i = 0; i < AlgerianCities.CITIES.length; i++) {
            AlgerianCities.City c = AlgerianCities.CITIES[i];
            assertTrue("City #" + (i + 1) + " (" + c.nameEn + ") within Algeria bounds",
                    c.latitude >= 19.0 && c.latitude <= 37.5 && c.longitude >= -9.0 && c.longitude <= 12.5);
        }
    }

    private static void testPrayerCalculatorGoldenReference() {
        System.out.println("\n[Suite 2] Algerian Ministry Astronomical Golden Reference (25 Oct 2026)");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"));
        cal.set(2026, Calendar.OCTOBER, 25, 12, 0, 0);

        PrayerCalculator.PrayerTimes times = PrayerCalculator.calculate(cal, PrayerCalculator.KAIS_LATITUDE, PrayerCalculator.KAIS_LONGITUDE);

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.US);
        timeFmt.setTimeZone(TimeZone.getTimeZone("Africa/Algiers"));

        String fajrStr = timeFmt.format(times.fajr);
        String sunriseStr = timeFmt.format(times.sunrise);
        String dhuhrStr = timeFmt.format(times.dhuhr);
        String asrStr = timeFmt.format(times.asr);
        String maghribStr = timeFmt.format(times.maghrib);
        String ishaStr = timeFmt.format(times.isha);

        System.out.println("    Calculated KaÃ¯s Prayer Times: " +
                "Fajr=" + fajrStr + ", Sunrise=" + sunriseStr + ", Dhuhr=" + dhuhrStr +
                ", Asr=" + asrStr + ", Maghrib=" + maghribStr + ", Isha=" + ishaStr);

        // Verification tolerance: Â±2 minutes of Ministry reference timetable
        assertTrue("Fajr matches Ministry reference (~05:22)", fajrStr.equals("05:21") || fajrStr.equals("05:22") || fajrStr.equals("05:23"));
        assertTrue("Sunrise matches Ministry reference (~06:47)", sunriseStr.equals("06:46") || sunriseStr.equals("06:47") || sunriseStr.equals("06:48"));
        assertTrue("Dhuhr matches Ministry reference (~12:16)", dhuhrStr.equals("12:15") || dhuhrStr.equals("12:16") || dhuhrStr.equals("12:17"));
        assertTrue("Asr matches Ministry reference (~15:21)", asrStr.equals("15:20") || asrStr.equals("15:21") || asrStr.equals("15:22"));
        assertTrue("Maghrib matches Ministry reference (~17:48)", maghribStr.equals("17:47") || maghribStr.equals("17:48") || maghribStr.equals("17:49"));
        assertTrue("Isha matches Ministry reference (~19:06)", ishaStr.equals("19:05") || ishaStr.equals("19:06") || ishaStr.equals("19:07"));
    }

    private static void testPrayerCalculatorWilayas() {
        System.out.println("\n[Suite 3] Multiple Wilayas Calculation");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"));

        // Algiers
        PrayerCalculator.PrayerTimes algiersTimes = PrayerCalculator.calculate(cal, 36.7538, 3.0588);
        assertTrue("Algiers prayer times calculated", algiersTimes != null && algiersTimes.fajr != null);

        // Oran
        PrayerCalculator.PrayerTimes oranTimes = PrayerCalculator.calculate(cal, 35.6976, -0.6337);
        assertTrue("Oran prayer times calculated", oranTimes != null && oranTimes.fajr != null);

        // Constantine
        PrayerCalculator.PrayerTimes constTimes = PrayerCalculator.calculate(cal, 36.3650, 6.6147);
        assertTrue("Constantine prayer times calculated", constTimes != null && constTimes.fajr != null);
    }

    private static void testDateHelper() {
        System.out.println("\n[Suite 4] Date and Calendar Localization");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"));
        cal.set(2026, Calendar.OCTOBER, 25);
        Date d = cal.getTime();

        String gregAr = DateHelper.getGregorianDateString(d, Strings.Lang.ARABIC);
        assertTrue("Gregorian date in Arabic contains 2026", gregAr != null && gregAr.contains("2026"));

        String gregFr = DateHelper.getGregorianDateString(d, Strings.Lang.FRENCH);
        assertTrue("Gregorian date in French contains 2026", gregFr != null && gregFr.contains("2026"));

        String hijri = DateHelper.getHijriDateString(d, Strings.Lang.ARABIC);
        assertTrue("Hijri date calculated", hijri != null && !hijri.isEmpty());
    }

    private static void testStringsLocalization() {
        System.out.println("\n[Suite 5] Strings Multilingual Localization");
        assertTrue("Arabic motto exists", Strings.get(Strings.Lang.ARABIC, "motto") != null);
        assertTrue("French motto exists", Strings.get(Strings.Lang.FRENCH, "motto").contains("Alg"));
        assertTrue("English motto exists", Strings.get(Strings.Lang.ENGLISH, "motto").contains("Algeria"));
        assertTrue("Tamazight motto exists", Strings.get(Strings.Lang.TAMAZIGHT, "motto").contains("Dzayer"));
    }
}