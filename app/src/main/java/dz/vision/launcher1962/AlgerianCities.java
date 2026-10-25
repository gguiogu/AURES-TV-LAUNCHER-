package dz.vision.launcher1962;

/**
 * Bundled offline directory of Algerian Wilayas & Cities with GPS coordinates.
 */
public final class AlgerianCities {

    public static class City {
        public final String nameAr;
        public final String nameFr;
        public final String nameEn;
        public final double latitude;
        public final double longitude;

        public City(String nameAr, String nameFr, String nameEn, double latitude, double longitude) {
            this.nameAr = nameAr;
            this.nameFr = nameFr;
            this.nameEn = nameEn;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getDisplayName(Strings.Lang lang) {
            if (lang == Strings.Lang.FRENCH) return nameFr;
            if (lang == Strings.Lang.ENGLISH) return nameEn;
            return nameAr;
        }
    }

    public static final City[] CITIES = {
            new City("خنشلة (قايس - الأوراس)", "Khenchela (Kaïs - Aurès)", "Khenchela (Kaïs - Aurès)", 35.4925, 6.9344),
            new City("الجزائر العاصمة", "Alger", "Algiers", 36.7538, 3.0588),
            new City("باتنة (الأوراس)", "Batna", "Batna", 35.5560, 6.1741),
            new City("قسنطينة", "Constantine", "Constantine", 36.3650, 6.6147),
            new City("بسكرة (بوابة الصحراء)", "Biskra", "Biskra", 34.8504, 5.7280),
            new City("أم البواقي", "Oum El Bouaghi", "Oum El Bouaghi", 35.8755, 7.1135),
            new City("تبسة", "Tébessa", "Tebessa", 35.4042, 8.1242),
            new City("سطيف (عين الفوارة)", "Sétif", "Setif", 36.1911, 5.4137),
            new City("وهران (الباهية)", "Oran", "Oran", 35.6976, -0.6337),
            new City("عنابة (بونة)", "Annaba", "Annaba", 36.9000, 7.7667),
            new City("غرداية (وادي ميزاب)", "Ghardaïa", "Ghardaia", 32.4909, 3.6735),
            new City("ورقلة", "Ouargla", "Ouargla", 31.9493, 5.3250),
            new City("تلمسان (عاصمة الزيانيين)", "Tlemcen", "Tlemcen", 34.8783, -1.3150),
            new City("بجاية (يما قورايا)", "Béjaïa", "Bejaia", 36.7511, 5.0567),
            new City("سكيكدة", "Skikda", "Skikda", 36.8762, 6.9092)
    };

    public static City getDefaultCity() {
        return CITIES[0]; // Khenchela (Kaïs)
    }

    public static City getCityByIndex(int index) {
        if (index >= 0 && index < CITIES.length) {
            return CITIES[index];
        }
        return CITIES[0];
    }

    private AlgerianCities() {}
}
