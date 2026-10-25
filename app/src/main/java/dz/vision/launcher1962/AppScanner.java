package dz.vision.launcher1962;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Real Device Application Scanner & Shelf Manager.
 * Scans genuinely installed applications via PackageManager.
 * Preserves original icon artwork, real labels, and launch intents.
 * Zero placeholder apps, zero fake icons.
 */
public final class AppScanner {

    private static final String PREFS_NAME = "launcher_1962_prefs";
    private static final String KEY_SHELF_ORDER = "shelf_app_order";

    // Known TV and player packages for prioritization
    private static final List<String> TV_PLAYER_PACKAGES = Arrays.asList(
            "ver3.ycntivi.off",                  // Yacine TV
            "net.mbc.shahidTV",                  // Shahid TV
            "com.ytv.pronew",                    // YTV Player Pro
            "io.freemytv"                        // FreeMyTV
    );

    // Known streaming media packages
    private static final List<String> STREAMING_PACKAGES = Arrays.asList(
            "org.smarttube.stable",              // SmartTube
            "com.google.android.youtube.tv",     // YouTube Android TV
            "com.netflix.mediaclient",           // Netflix
            "com.amazon.amazonvideo.livingroom", // Prime Video
            "com.disney.disneyplus",             // Disney+
            "com.hitv.venom",                    // HiTV
            "com.haystack.android"               // Haystack News
    );

    // Packages to exclude from the ordinary app list (internal components and duplicate launchers)
    private static final Set<String> EXCLUDED_PACKAGES = new HashSet<>(Arrays.asList(
            "dz.vision.launcher1962",            // Self
            "com.ansub.tvhome",                  // Stock launcher (accessible in Settings recovery)
            "com.oranth.tvlauncher",             // AliceUX internal launcher
            "com.google.android.tvlauncher",     // Google TV Launcher
            "com.google.android.leanbacklauncher", // Leanback Launcher
            "com.oranth.homepicker",             // Internal picker
            "com.oranth.atvapplist",             // Internal stock applist activity
            "com.android.tv.settings"            // System settings (dedicated functional button)
    ));

    // Icon cache to avoid decoding multiple times during focus/animations
    private static final Map<String, Drawable> ICON_CACHE = new HashMap<>();

    /**
     * Scan all eligible, launchable applications on the TV box.
     */
    public static List<AppItem> scanAllApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, AppItem> appMap = new HashMap<>();

        // 1. Query Leanback (Android TV) activities
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> leanbackList = pm.queryIntentActivities(leanbackIntent, 0);
        for (ResolveInfo ri : leanbackList) {
            processResolveInfo(pm, ri, appMap);
        }

        // 2. Query Standard Launcher activities (catches sideloaded or standard apps)
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launcherList = pm.queryIntentActivities(launcherIntent, 0);
        for (ResolveInfo ri : launcherList) {
            processResolveInfo(pm, ri, appMap);
        }

        List<AppItem> allApps = new ArrayList<>(appMap.values());

        // Sort alphabetically by label (Arabic / English aware)
        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(allApps, (a, b) -> collator.compare(a.label, b.label));

        return allApps;
    }

    private static void processResolveInfo(PackageManager pm, ResolveInfo ri, Map<String, AppItem> appMap) {
        if (ri.activityInfo == null) return;
        String pkg = ri.activityInfo.packageName;
        if (EXCLUDED_PACKAGES.contains(pkg)) return;
        if (appMap.containsKey(pkg)) return;

        try {
            String actName = ri.activityInfo.name;
            CharSequence rawLabel = ri.loadLabel(pm);
            String label = (rawLabel != null && rawLabel.length() > 0) ? rawLabel.toString().trim() : pkg;

            // Load real installed icon
            Drawable icon = ICON_CACHE.get(pkg);
            if (icon == null) {
                icon = ri.loadIcon(pm);
                if (icon == null) {
                    icon = pm.getApplicationIcon(pkg);
                }
                if (icon != null) {
                    ICON_CACHE.put(pkg, icon);
                }
            }

            int dominantColor = ColorExtractor.extract(pkg, icon);
            appMap.put(pkg, new AppItem(pkg, actName, label, icon, dominantColor, false, null));
        } catch (Throwable ignored) {}
    }

    /**
     * Build the Main Shelf from genuine installed applications according to user rules:
     * 1. Genuinely installed TV / player apps first.
     * 2. Genuinely installed streaming / media apps next.
     * 3. Other genuinely installed launchable apps.
     * 4. Dedicated functional All Apps and Settings tiles.
     * 5. Never show uninstalled apps (e.g. Spotify is omitted).
     */
    public static List<AppItem> buildMainShelf(Context context, List<AppItem> allApps) {
        Map<String, AppItem> installedMap = new HashMap<>();
        for (AppItem item : allApps) {
            installedMap.put(item.packageName, item);
        }

        List<AppItem> shelf = new ArrayList<>();
        Set<String> added = new HashSet<>();

        // Check if custom order is stored
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedOrder = sp.getString(KEY_SHELF_ORDER, null);
        if (savedOrder != null && savedOrder.trim().length() > 0) {
            String[] pkgList = savedOrder.split(",");
            for (String p : pkgList) {
                if (shelf.size() >= 7) break;
                String pkg = p.trim();
                if (installedMap.containsKey(pkg) && !added.contains(pkg)) {
                    shelf.add(installedMap.get(pkg));
                    added.add(pkg);
                }
            }
        }

        // 1. Prioritize TV / Player apps (Yacine TV, Shahid, YTV Pro, etc.)
        for (String tvPkg : TV_PLAYER_PACKAGES) {
            if (shelf.size() >= 1) break;
            if (installedMap.containsKey(tvPkg) && !added.contains(tvPkg)) {
                shelf.add(installedMap.get(tvPkg));
                added.add(tvPkg);
            }
        }

        // 2. YouTube client (SmartTube preferred, then Google YouTube TV)
        if (shelf.size() < 2) {
            if (installedMap.containsKey("org.smarttube.stable") && !added.contains("org.smarttube.stable")) {
                shelf.add(installedMap.get("org.smarttube.stable"));
                added.add("org.smarttube.stable");
            } else if (installedMap.containsKey("com.google.android.youtube.tv") && !added.contains("com.google.android.youtube.tv")) {
                shelf.add(installedMap.get("com.google.android.youtube.tv"));
                added.add("com.google.android.youtube.tv");
            }
        }

        // 3. Streaming packages (Shahid, Netflix, Prime Video, Disney+, HiTV)
        for (String streamPkg : STREAMING_PACKAGES) {
            if (shelf.size() >= 7) break;
            if (installedMap.containsKey(streamPkg) && !added.contains(streamPkg)) {
                shelf.add(installedMap.get(streamPkg));
                added.add(streamPkg);
            }
        }

        // 4. Fill remaining slots up to 7 with other genuine installed apps
        for (AppItem app : allApps) {
            if (shelf.size() >= 7) break;
            if (!added.contains(app.packageName)) {
                shelf.add(app);
                added.add(app.packageName);
            }
        }

        return shelf;
    }

    /**
     * Clear icon cache to release bitmap memory when app is stopped or restarted.
     */
    public static void clearCache() {
        ICON_CACHE.clear();
    }
}
