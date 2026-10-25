package dz.vision.launcher1962;

/**
 * Exact Arabic announcement texts for the five daily prayers.
 * Preserves the exact specified phrasing byte-for-byte.
 */
public final class PrayerAnnouncements {

    public static final String FAJR_TITLE = "صلاة الفجر";
    public static final String FAJR_SUBTITLE = "وتسمى أيضاً صلاة الصبح";
    public static final String FAJR_TEXT = "حان الآن وقت صلاة الفجر، وتسمى أيضاً صلاة الصبح.";

    public static final String DHUHR_TITLE = "صلاة الظهر";
    public static final String DHUHR_TEXT = "حان الآن وقت صلاة الظهر.";

    public static final String ASR_TITLE = "صلاة العصر";
    public static final String ASR_TEXT = "حان الآن وقت صلاة العصر.";

    public static final String MAGHRIB_TITLE = "صلاة المغرب";
    public static final String MAGHRIB_TEXT = "حان الآن وقت صلاة المغرب.";

    public static final String ISHA_TITLE = "صلاة العشاء";
    public static final String ISHA_TEXT = "حان الآن وقت صلاة العشاء.";

    public static final String[] PRAYER_TITLES = {
            FAJR_TITLE,
            "الشروق",
            DHUHR_TITLE,
            ASR_TITLE,
            MAGHRIB_TITLE,
            ISHA_TITLE
    };

    public static final String[] ANNOUNCEMENT_TEXTS = {
            FAJR_TEXT,
            "", // Sunrise has no salat announcement
            DHUHR_TEXT,
            ASR_TEXT,
            MAGHRIB_TEXT,
            ISHA_TEXT
    };

    public static String getAnnouncementText(int prayerIndex) {
        if (prayerIndex >= 0 && prayerIndex < ANNOUNCEMENT_TEXTS.length) {
            return ANNOUNCEMENT_TEXTS[prayerIndex];
        }
        return "";
    }

    public static String getAssetFilename(int prayerIndex) {
        switch (prayerIndex) {
            case 0: return "fajr.mp3";
            case 2: return "dhuhr.mp3";
            case 3: return "asr.mp3";
            case 4: return "maghrib.mp3";
            case 5: return "isha.mp3";
            default: return null;
        }
    }

    public static String getPrayerTitle(int prayerIndex) {
        if (prayerIndex >= 0 && prayerIndex < PRAYER_TITLES.length) {
            return PRAYER_TITLES[prayerIndex];
        }
        return "";
    }

    public static final String TEST_ASSET_FILENAME = "test.mp3";
    public static final String TEST_ANNOUNCEMENT_TEXT = "هذا اختبار تجريبي لنظام الإعلان الصوتي العربي والأذان في تلفاز الأوراس.";

    private PrayerAnnouncements() {}
}
