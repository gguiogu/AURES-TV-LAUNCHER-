package dz.vision.launcher1962;

public final class Strings {

    public enum Lang {
        ARABIC,
        FRENCH,
        ENGLISH,
        TAMAZIGHT
    }

    public static String get(Lang lang, String key) {
        switch (lang) {
            case FRENCH:
                return getFrench(key);
            case ENGLISH:
                return getEnglish(key);
            case TAMAZIGHT:
                return getTamazight(key);
            case ARABIC:
            default:
                return getArabic(key);
        }
    }

    private static String getArabic(String key) {
        switch (key) {
            case "app_name": return "1962";
            case "motto": return "\u0627\u0644\u062c\u0632\u0627\u0626\u0631 \u062f\u0627\u0626\u0645\u0627\u064b \u0648\u0623\u0628\u062f\u0627\u064b";
            case "all_apps": return "\u0627\u0644\u062a\u0637\u0628\u064a\u0642\u0627\u062a";
            case "settings": return "\u0627\u0644\u0625\u0639\u062f\u0627\u062f\u0627\u062a";
            case "tv_channels": return "\u0642\u0646\u0648\u0627\u062a \u062c\u0632\u0627\u0626\u0631\u064a\u0629";
            case "all_apps_btn": return "\u062c\u0645\u064a\u0639 \u0627\u0644\u062a\u0637\u0628\u064a\u0642\u0627\u062a";
            case "back_to_stock": return "\u0627\u0644\u0639\u0648\u062f\u0629 \u0625\u0644\u0649 \u0627\u0644\u0648\u0627\u062c\u0647\u0629 \u0627\u0644\u0623\u0635\u0644\u064a\u0629";
            case "hide_app": return "\u0625\u062e\u0641\u0627\u0621 \u0627\u0644\u062a\u0637\u0628\u064a\u0642";
            case "move_app": return "\u0646\u0642\u0644 \u0627\u0644\u062a\u0637\u0628\u064a\u0642";
            case "app_options": return "\u062e\u064a\u0627\u0631\u0627\u062a \u0627\u0644\u062a\u0637\u0628\u064a\u0642";
            case "algerian_tv": return "\u0627\u0644\u062a\u0644\u0641\u0632\u064a\u0648\u0646";
            case "entv": return "\u0627\u0644\u062a\u0644\u0641\u0632\u064a\u0648\u0646 \u0627\u0644\u062c\u0632\u0627\u0626\u0631\u064a";
            case "canal_algerie": return "\u0643\u0646\u0627\u0644 \u0623\u0644\u062c\u064a\u0631\u064a";
            case "echorouk": return "\u0627\u0644\u0634\u0631\u0648\u0642 TV";
            case "ennahar": return "\u0627\u0644\u0646\u0647\u0627\u0631 TV";
            default: return key;
        }
    }

    private static String getFrench(String key) {
        switch (key) {
            case "app_name": return "1962";
            case "motto": return "L'Alg\u00e9rie pour toujours";
            case "all_apps": return "Applications";
            case "settings": return "Param\u00e8tres";
            case "tv_channels": return "Cha\u00eenes Alg\u00e9riennes";
            case "all_apps_btn": return "Toutes les applications";
            case "back_to_stock": return "Retour au lanceur d'origine";
            case "hide_app": return "Masquer l'application";
            case "move_app": return "D\u00e9placer l'application";
            case "app_options": return "Options";
            case "algerian_tv": return "T\u00e9l\u00e9vision";
            case "entv": return "ENTV";
            case "canal_algerie": return "Canal Alg\u00e9rie";
            case "echorouk": return "Echorouk TV";
            case "ennahar": return "Ennahar TV";
            default: return key;
        }
    }

    private static String getEnglish(String key) {
        switch (key) {
            case "app_name": return "1962";
            case "motto": return "Algeria Always & Forever";
            case "all_apps": return "Applications";
            case "settings": return "Settings";
            case "tv_channels": return "Algerian Channels";
            case "all_apps_btn": return "All Applications";
            case "back_to_stock": return "Back to Original Launcher";
            case "hide_app": return "Hide App";
            case "move_app": return "Move App";
            case "app_options": return "App Options";
            case "algerian_tv": return "Live TV";
            case "entv": return "ENTV";
            case "canal_algerie": return "Canal Alg\u00e9rie";
            case "echorouk": return "Echorouk TV";
            case "ennahar": return "Ennahar TV";
            default: return key;
        }
    }

    private static String getTamazight(String key) {
        switch (key) {
            case "app_name": return "1962";
            case "motto": return "Dzayer i lebda";
            case "all_apps": return "Tisnasin";
            case "settings": return "I\u0263ewwa\u1e5den";
            case "tv_channels": return "Tibidiwiyin Tizzayriyin";
            case "all_apps_btn": return "Akk Tisnasin";
            case "back_to_stock": return "U\u0263al \u0263er umahwal n tazwara";
            case "hide_app": return "Ffer tasnesst";
            case "move_app": return "Smeskel tasnesst";
            case "app_options": return "Ifran n tesnesst";
            case "algerian_tv": return "Tili\u1e93ri";
            case "entv": return "ENTV";
            case "canal_algerie": return "Canal Alg\u00e9rie";
            case "echorouk": return "Echorouk TV";
            case "ennahar": return "Ennahar TV";
            default: return key;
        }
    }

    private Strings() {}
}