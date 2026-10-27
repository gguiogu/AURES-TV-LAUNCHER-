package dz.vision.launcher1962;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1962 Android TV Launcher — Phase 1 Exact Reference Match.
 * Target: Tanix TX1 (Allwinner H313, Mali-G31 MP2, Android 10, 1280x720 @ 160dpi).
 */
public class MainActivity extends Activity {

    public static final String PREFS_NAME = "launcher_1962_prefs";
    public static final String KEY_WALLPAPER = "current_wallpaper";
    public static final String KEY_LANG = "current_language";

    // 1280x720 Native Proportions (1dp = 1px at 160dpi)
    private static final int CARD_H = 175;
    private static final int CARD_W_WEATHER = 225;
    private static final int CARD_W_PRAYER = 270;
    private static final int CARD_W_TODAY = 230;

    private static final int APP_TILE_W = 144;
    private static final int APP_TILE_H = 124;
    private static final int CH_TILE_W = 125;
    private static final int CH_TILE_H = 68;

    // View Hierarchy
    private FrameLayout rootFrame;
    private ImageView wallpaperView;
    private View darkGradientOverlay;
    private GlowView glowView;
    private ScrollView mainScrollView;
    private LinearLayout contentBox;

    // Header Views
    private Wordmark1962View wordmarkView;
    private TextView clockView;
    private TextView dateView;
    private TextView settingsPill;
    private TextView wifiPill;
    private TextView langPillAr;
    private TextView langPillFr;
    private TextView langPillEn;
    private TextView langPillTz;

    // Shelf & Rows
    private LinearLayout cardsRow;
    private LinearLayout mainShelfRow;
    private LinearLayout channelsRow;
    private View navIndicator;

    // Focus state
    private final List<View> focusableAppTiles = new ArrayList<>();
    private final List<View> focusableChannelTiles = new ArrayList<>();
    private int currentFocusRow = 0; // 0=AppShelf, 1=Channels, 2=Cards, 3=Header
    private int currentFocusCol = 0;

    // All Apps Overlay
    private FrameLayout allAppsOverlay;
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppScanner.clearCache();
            populateMainAppShelf();
            if (allAppsOverlay != null && allAppsOverlay.getVisibility() == View.VISIBLE) {
                showAllAppsScreen();
            }
        }
    };

    private String currentWallpaper = "wallpaper_algiers.jpg";
    private Strings.Lang currentLang = Strings.Lang.ARABIC;
    private Bitmap cachedWallpaperBmp = null;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    // Dynamic Prayer & Date Views
    private PrayerManager prayerManager;
    private TextView prayerSubLocation;
    private TextView prayerSubCountdown;
    private final LinearLayout[] prayerRowViews = new LinearLayout[6];
    private final TextView[] prayerNameViews = new TextView[6];
    private final TextView[] prayerTimeViews = new TextView[6];
    private GradientDrawable activePillDrawable;

    private TextView todayGregView;
    private TextView todayHijriView;

    private BroadcastReceiver timeReceiver;
    private SimpleDateFormat clockTimeFormat;
    private SimpleDateFormat headerDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SafetyWatchdog.init(this);
        PasswordManager.ensureInitialized(this);

        // Load Preferences
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentWallpaper = sp.getString(KEY_WALLPAPER, "wallpaper_algiers.jpg");
        String savedLang = sp.getString(KEY_LANG, Strings.Lang.ARABIC.name());
        try {
            currentLang = Strings.Lang.valueOf(savedLang);
        } catch (Throwable ignored) {}

        // Initialize Prayer Manager & Date formatters
        prayerManager = new PrayerManager();
        int cityIndex = sp.getInt("salat_city_index", 0);
        AlgerianCities.City defaultCity = AlgerianCities.getCityByIndex(cityIndex);
        prayerManager.setLocation(defaultCity.latitude, defaultCity.longitude);

        clockTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        headerDateFormat = new SimpleDateFormat("EEEE d MMMM yyyy", (currentLang == Strings.Lang.FRENCH) ? Locale.FRENCH :
                (currentLang == Strings.Lang.ENGLISH) ? Locale.ENGLISH : new Locale("ar"));
        clockTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone(PrayerCalculator.TIMEZONE_ID));
        headerDateFormat.setTimeZone(java.util.TimeZone.getTimeZone(PrayerCalculator.TIMEZONE_ID));

        // Force RTL for Arabic layout mirroring
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // Build Responsive UI
        buildUserInterface();

        // Populate Main App Shelf (7 Tiles, TV focused initially)
        populateMainAppShelf();

        // Populate Algerian Channels Shelf & All Apps Button
        populateChannelsShelf();

        // Initial update of Date, Time and Prayer calculations
        updateDateTimeAndPrayers();

        // Schedule exact alarms for Salat Mode
        SalatAlarmScheduler.scheduleNextPrayerAlarm(this, prayerManager);

        // Request initial focus on Tile 1 (التلفزيون)
        handler.postDelayed(() -> {
            if (!focusableAppTiles.isEmpty()) {
                focusableAppTiles.get(0).requestFocus();
            }
        }, 150);

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null) {
            if (intent.getBooleanExtra("extra_open_salat_settings", false)) {
                handler.postDelayed(() -> showPrayerSettingsDialog(), 200);
            } else if (intent.getBooleanExtra("extra_test_assets", false)) {
                handler.postDelayed(() -> SalatUiHelper.showAssetIntegrityDialog(this), 200);
            } else if (intent.getBooleanExtra("extra_test_mode", false)) {
                handler.postDelayed(() -> SalatUiHelper.showDeveloperTestDialog(this), 200);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Load / Restore wallpaper efficiently
        loadWallpaperBitmap();

        // Listen for installed/uninstalled packages to keep app shelf up to date
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addDataScheme("package");
            registerReceiver(packageReceiver, filter);
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Immediately refresh clock, dates, and prayer times
        updateDateTimeAndPrayers();
        startClock();

        // Listen for time ticks, time/timezone/date changes, and screen-on events
        if (timeReceiver == null) {
            timeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateDateTimeAndPrayers();
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_DATE_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            try {
                registerReceiver(timeReceiver, filter);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClock();
        if (timeReceiver != null) {
            try {
                unregisterReceiver(timeReceiver);
            } catch (Throwable ignored) {}
            timeReceiver = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(packageReceiver);
        } catch (Throwable ignored) {}

        // Release large wallpaper bitmap while user is in another app to maintain < 20 MB PSS
        if (cachedWallpaperBmp != null && !cachedWallpaperBmp.isRecycled()) {
            wallpaperView.setImageDrawable(null);
            cachedWallpaperBmp.recycle();
            cachedWallpaperBmp = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClock();
        if (timeReceiver != null) {
            try {
                unregisterReceiver(timeReceiver);
            } catch (Throwable ignored) {}
            timeReceiver = null;
        }
    }

    private void buildUserInterface() {
        float density = getResources().getDisplayMetrics().density; // 1.0 on Tanix TX1

        rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(Color.BLACK);
        rootFrame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 1. Wallpaper Layer
        wallpaperView = new ImageView(this);
        wallpaperView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rootFrame.addView(wallpaperView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 2. Dark Vignette / Contrast Overlay
        // Left 60% is deep black for cards/text contrast, right 40% shows illuminated Maqam Echahid & flag
        darkGradientOverlay = new View(this) {
            private final Paint paint = new Paint();
            @Override
            protected void onDraw(Canvas canvas) {
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;
                // Horizontal gradient: pitch black #000000 on left -> translucent on right
                LinearGradient grad = new LinearGradient(
                        0, 0, w, 0,
                        new int[]{0xFF000000, 0xEE000000, 0x99000000, 0x11000000, 0x00000000},
                        new float[]{0.0f, 0.40f, 0.62f, 0.85f, 1.0f},
                        Shader.TileMode.CLAMP
                );
                paint.setShader(grad);
                canvas.drawRect(0, 0, w, h, paint);
            }
        };
        rootFrame.addView(darkGradientOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 3. Dynamic Glow Layer
        glowView = new GlowView(this);
        rootFrame.addView(glowView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 4. Main Content Container
        mainScrollView = new ScrollView(this);
        mainScrollView.setVerticalScrollBarEnabled(false);
        mainScrollView.setClipChildren(false);
        mainScrollView.setClipToPadding(false);
        mainScrollView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        contentBox = new LinearLayout(this);
        contentBox.setOrientation(LinearLayout.VERTICAL);
        contentBox.setClipChildren(false);
        contentBox.setClipToPadding(false);
        contentBox.setPadding(
                (int) (32 * density),
                (int) (16 * density),
                (int) (32 * density),
                (int) (12 * density)
        );

        // --- TOP HEADER ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hLp.bottomMargin = (int) (14 * density);
        header.setLayoutParams(hLp);

        // Wordmark 1962 (Top Left)
        wordmarkView = new Wordmark1962View(this);
        header.addView(wordmarkView);

        // Spacer 1
        View sp1 = new View(this);
        header.addView(sp1, new LinearLayout.LayoutParams(0, 1, 1f));

        // Clock & Date (Top Center)
        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.CENTER);

        clockView = new TextView(this);
        clockView.setTextColor(Color.WHITE);
        clockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        clockView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        clockBox.addView(clockView);

        dateView = new TextView(this);
        dateView.setTextColor(0xFFCCCCCC);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        dateView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        clockBox.addView(dateView);

        header.addView(clockBox);

        // Spacer 2
        View sp2 = new View(this);
        header.addView(sp2, new LinearLayout.LayoutParams(0, 1, 1f));

        // Badges: Settings, Wifi, Languages (Top Right)
        LinearLayout badgesBox = new LinearLayout(this);
        badgesBox.setOrientation(LinearLayout.HORIZONTAL);
        badgesBox.setGravity(Gravity.CENTER_VERTICAL);

        settingsPill = createHeaderPill("⚙", v -> PasswordManager.authenticate(this, "لوحة الإعدادات", () -> showSettingsDialog(), null));
        wifiPill = createHeaderPill("📶", v -> PasswordManager.authenticate(this, "إعدادات Wi-Fi", () -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)), null));
        langPillAr = createHeaderPill("العربية ⌵", v -> PasswordManager.authenticate(this, "تغيير لغة الواجهة", () -> showLanguageDialog(), null));
        langPillFr = createHeaderPill("Français", v -> PasswordManager.authenticate(this, "تغيير لغة الواجهة", () -> { currentLang = Strings.Lang.FRENCH; recreate(); }, null));
        langPillEn = createHeaderPill("English", v -> PasswordManager.authenticate(this, "تغيير لغة الواجهة", () -> { currentLang = Strings.Lang.ENGLISH; recreate(); }, null));
        langPillTz = createHeaderPill("ⵜⴰⵎⴰⵣⵉⵖⵜ", v -> PasswordManager.authenticate(this, "تغيير لغة الواجهة", () -> { currentLang = Strings.Lang.TAMAZIGHT; recreate(); }, null));

        badgesBox.addView(settingsPill);
        badgesBox.addView(wifiPill);
        badgesBox.addView(langPillAr);
        badgesBox.addView(langPillFr);
        badgesBox.addView(langPillEn);
        badgesBox.addView(langPillTz);

        header.addView(badgesBox);
        contentBox.addView(header);

        // --- THREE INFORMATION CARDS (Upper Section) ---
        cardsRow = new LinearLayout(this);
        cardsRow.setOrientation(LinearLayout.HORIZONTAL);
        cardsRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams crLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        crLp.bottomMargin = (int) (18 * density);
        cardsRow.setLayoutParams(crLp);

        // Card 1: Weather
        View cardWeather = createWeatherCard();
        cardsRow.addView(cardWeather);

        // Card 2: Prayer Times — Fixed Kaïs Panel
        View cardPrayer = createPrayerCard();
        cardsRow.addView(cardPrayer);

        // Card 3: Today
        View cardToday = createTodayCard();
        cardsRow.addView(cardToday);

        contentBox.addView(cardsRow);

        // --- MAIN APP SHELF (7 Rounded Tiles) ---
        mainShelfRow = new LinearLayout(this);
        mainShelfRow.setOrientation(LinearLayout.HORIZONTAL);
        mainShelfRow.setClipToPadding(false);
        mainShelfRow.setClipChildren(false);
        mainShelfRow.setPadding(0, (int) (8 * density), 0, (int) (16 * density));
        contentBox.addView(mainShelfRow);

        // --- ALGERIAN TELEVISION SHORTCUTS & ALL APPS ---
        LinearLayout channelHeader = new LinearLayout(this);
        channelHeader.setOrientation(LinearLayout.HORIZONTAL);
        channelHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams chLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chLp.topMargin = (int) (10 * density);
        chLp.bottomMargin = (int) (8 * density);
        channelHeader.setLayoutParams(chLp);

        TextView chTitle = new TextView(this);
        chTitle.setText("قنوات جزائرية");
        chTitle.setTextColor(0xFFC7CDD3);
        chTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        chTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        channelHeader.addView(chTitle);

        View chLine = new View(this);
        chLine.setBackgroundColor(0x3327313A);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, (int) (1 * density), 1f);
        clp.rightMargin = (int) (14 * density);
        clp.leftMargin = (int) (14 * density);
        channelHeader.addView(chLine, clp);

        contentBox.addView(channelHeader);

        channelsRow = new LinearLayout(this);
        channelsRow.setOrientation(LinearLayout.HORIZONTAL);
        channelsRow.setGravity(Gravity.CENTER_VERTICAL);
        channelsRow.setClipToPadding(false);
        channelsRow.setClipChildren(false);
        contentBox.addView(channelsRow);

        // --- BOTTOM NAVIGATION INDICATOR ---
        navIndicator = new View(this);
        GradientDrawable navGd = new GradientDrawable();
        navGd.setCornerRadius(3 * density);
        navGd.setColor(0x88FFFFFF);
        navIndicator.setBackground(navGd);

        LinearLayout.LayoutParams niLp = new LinearLayout.LayoutParams((int) (130 * density), (int) (4 * density));
        niLp.gravity = Gravity.CENTER_HORIZONTAL;
        niLp.topMargin = (int) (16 * density);
        niLp.bottomMargin = (int) (4 * density);
        navIndicator.setLayoutParams(niLp);
        contentBox.addView(navIndicator);

        mainScrollView.addView(contentBox);
        rootFrame.addView(mainScrollView);

        // All Apps Fullscreen Leanback Overlay
        allAppsOverlay = new FrameLayout(this);
        allAppsOverlay.setBackgroundColor(0xF6070A0F); // 96% deep dark cinematic black
        allAppsOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        allAppsOverlay.setVisibility(View.GONE);
        allAppsOverlay.setFocusable(true);
        rootFrame.addView(allAppsOverlay);

        setContentView(rootFrame);
    }

    private TextView createHeaderPill(String text, View.OnClickListener listener) {
        float density = getResources().getDisplayMetrics().density;
        TextView pill = new TextView(this);
        pill.setText(text);
        pill.setTextColor(0xFFE0E0E0);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pill.setFocusable(true);
        pill.setClickable(true);
        pill.setOnClickListener(listener);

        pill.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.leftMargin = (int) (4 * density);
        lp.rightMargin = (int) (4 * density);
        pill.setLayoutParams(lp);

        pill.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                pill.setTextColor(Color.WHITE);
                GradientDrawable pGd = new GradientDrawable();
                pGd.setCornerRadius(12 * density);
                pGd.setColor(0x5500C878);
                pGd.setStroke((int) (1.5f * density), 0xFF00C878);
                pill.setBackground(pGd);
                pill.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start();
            } else {
                pill.setTextColor(0xFFCCCCCC);
                pill.setBackground(null);
                pill.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }
        });
        return pill;
    }

    // --- CARD 1: WEATHER ---
    private View createWeatherCard() {
        float density = getResources().getDisplayMetrics().density;
        FrameLayout card = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int) (CARD_W_WEATHER * density),
                (int) (CARD_H * density)
        );
        lp.rightMargin = (int) (12 * density);
        card.setLayoutParams(lp);

        // Glass surface
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(18 * density);
        gd.setColor(0xD0121A24);
        gd.setStroke((int) (1 * density), 0xFF27313A);
        card.setBackground(gd);
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                gd.setColor(0xF0182432);
                gd.setStroke((int) (2.5f * density), 0xFF00C878);
                card.animate().scaleX(1.04f).scaleY(1.04f).setDuration(160).start();
            } else {
                gd.setColor(0xD0121A24);
                gd.setStroke((int) (1 * density), 0xFF27313A);
                card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }
        });
        card.setOnClickListener(v -> PasswordManager.authenticate(this, "إعدادات الطقس والمدينة", () -> showWeatherSettingsDialog(), null));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (14 * density), (int) (12 * density), (int) (14 * density), (int) (12 * density));

        // Location Header
        TextView loc = new TextView(this);
        loc.setText("قايس، خنشلة 📍");
        loc.setTextColor(0xFFCCCCCC);
        loc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        loc.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        layout.addView(loc);

        // Temp & Sun Icon
        LinearLayout tempRow = new LinearLayout(this);
        tempRow.setOrientation(LinearLayout.HORIZONTAL);
        tempRow.setGravity(Gravity.CENTER_VERTICAL);
        tempRow.setPadding(0, (int) (6 * density), 0, (int) (4 * density));

        TextView sun = new TextView(this);
        sun.setText("☀️ ");
        sun.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        tempRow.addView(sun);

        TextView temp = new TextView(this);
        temp.setText("22°");
        temp.setTextColor(Color.WHITE);
        temp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        temp.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        tempRow.addView(temp);

        layout.addView(tempRow);

        // Condition
        TextView cond = new TextView(this);
        cond.setText("مشمس");
        cond.setTextColor(0xFFE0E0E0);
        cond.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        layout.addView(cond);

        // Range
        TextView range = new TextView(this);
        range.setText("17° / 26°");
        range.setTextColor(0xFF8899A6);
        range.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        layout.addView(range);

        card.addView(layout);
        return card;
    }

    // --- CARD 2: PRAYER TIMES — DYNAMIC KAÏS PANEL ---
    private View createPrayerCard() {
        float density = getResources().getDisplayMetrics().density;
        FrameLayout card = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int) (CARD_W_PRAYER * density),
                (int) (CARD_H * density)
        );
        lp.rightMargin = (int) (12 * density);
        card.setLayoutParams(lp);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(18 * density);
        gd.setColor(0xD0121A24);
        gd.setStroke((int) (1 * density), 0xFF27313A);
        card.setBackground(gd);
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                gd.setColor(0xF0182432);
                gd.setStroke((int) (2.5f * density), 0xFF00C878);
                card.animate().scaleX(1.04f).scaleY(1.04f).setDuration(160).start();
            } else {
                gd.setColor(0xD0121A24);
                gd.setStroke((int) (1 * density), 0xFF27313A);
                card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }
        });
        card.setOnClickListener(v -> showPrayerSettingsDialog());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (14 * density), (int) (10 * density), (int) (14 * density), (int) (10 * density));

        // Header Title
        TextView title = new TextView(this);
        title.setText("أوقات الصلاة ✦");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        layout.addView(title);

        // Sub Header: Location on right, Countdown on left
        LinearLayout subRow = new LinearLayout(this);
        subRow.setOrientation(LinearLayout.HORIZONTAL);
        subRow.setGravity(Gravity.CENTER_VERTICAL);
        subRow.setPadding(0, 0, 0, (int) (4 * density));

        prayerSubLocation = new TextView(this);
        prayerSubLocation.setText("📍 قايس، خنشلة");
        prayerSubLocation.setTextColor(0xFF00C878); // Algerian Green
        prayerSubLocation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        prayerSubLocation.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        subRow.addView(prayerSubLocation);

        View subSpacer = new View(this);
        subRow.addView(subSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        prayerSubCountdown = new TextView(this);
        prayerSubCountdown.setTextColor(0xFFFFD700); // Soft Algerian Gold accent
        prayerSubCountdown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        prayerSubCountdown.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        prayerSubCountdown.setSingleLine(true);
        subRow.addView(prayerSubCountdown);

        layout.addView(subRow);

        // Highlight pill drawable
        activePillDrawable = new GradientDrawable();
        activePillDrawable.setCornerRadius(6 * density);
        activePillDrawable.setColor(0xFF00C878); // Green highlight pill

        // 6 Timings Grid
        for (int i = 0; i < 6; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding((int) (4 * density), (int) (1.5f * density), (int) (4 * density), (int) (1.5f * density));

            TextView name = new TextView(this);
            name.setTextColor(0xFFCCCCCC);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            name.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView time = new TextView(this);
            time.setTextColor(Color.WHITE);
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            time.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            row.addView(time);

            prayerRowViews[i] = row;
            prayerNameViews[i] = name;
            prayerTimeViews[i] = time;

            layout.addView(row);
        }

        card.addView(layout);
        return card;
    }

    // --- CARD 3: TODAY ---
    private View createTodayCard() {
        float density = getResources().getDisplayMetrics().density;
        FrameLayout card = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int) (CARD_W_TODAY * density),
                (int) (CARD_H * density)
        );
        card.setLayoutParams(lp);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(18 * density);
        gd.setColor(0xD0121A24);
        gd.setStroke((int) (1 * density), 0xFF27313A);
        card.setBackground(gd);
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                gd.setColor(0xF0182432);
                gd.setStroke((int) (2.5f * density), 0xFF00C878);
                card.animate().scaleX(1.04f).scaleY(1.04f).setDuration(160).start();
            } else {
                gd.setColor(0xD0121A24);
                gd.setStroke((int) (1 * density), 0xFF27313A);
                card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }
        });
        card.setOnClickListener(v -> PasswordManager.authenticate(this, "إعدادات التاريخ والتقويم", () -> showDateSettingsDialog(), null));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (14 * density), (int) (14 * density), (int) (14 * density), (int) (14 * density));

        // Header
        TextView header = new TextView(this);
        header.setText("📅 التاريخ اليوم");
        header.setTextColor(Color.WHITE);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        header.setPadding(0, 0, 0, (int) (8 * density));
        layout.addView(header);

        // Gregorian Date
        todayGregView = new TextView(this);
        todayGregView.setTextColor(Color.WHITE);
        todayGregView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        todayGregView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        layout.addView(todayGregView);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(0xFF27313A);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (1 * density)
        );
        dLp.topMargin = (int) (16 * density);
        dLp.bottomMargin = (int) (16 * density);
        layout.addView(div, dLp);

        // Hijri Date
        todayHijriView = new TextView(this);
        todayHijriView.setTextColor(0xFF00C878); // Green crescent accent
        todayHijriView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        todayHijriView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        layout.addView(todayHijriView);

        card.addView(layout);
        return card;
    }

    // --- MAIN APP SHELF: Genuinely Installed Applications (tvOS Architecture) ---
    private void populateMainAppShelf() {
        mainShelfRow.removeAllViews();
        focusableAppTiles.clear();
        float density = getResources().getDisplayMetrics().density;

        List<AppItem> allApps = AppScanner.scanAllApps(this);
        List<AppItem> shelfApps = AppScanner.buildMainShelf(this, allApps);

        final int cardW = (int) (144 * density);
        final int cardH = (int) (104 * density);
        final int iconSize = (int) (70 * density);

        for (int i = 0; i < shelfApps.size(); i++) {
            final AppItem app = shelfApps.get(i);
            final int colIndex = i;
            final boolean isTv = "ver3.ycntivi.off".equals(app.packageName) || "tv".equals(app.packageName);

            // Container Column (Card + Label below on shared baseline)
            LinearLayout itemCol = new LinearLayout(this);
            itemCol.setOrientation(LinearLayout.VERTICAL);
            itemCol.setGravity(Gravity.CENTER_HORIZONTAL);
            itemCol.setClipChildren(false);
            itemCol.setClipToPadding(false);

            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                    cardW,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) colLp.rightMargin = (int) (16 * density);
            itemCol.setLayoutParams(colLp);

            itemCol.setFocusable(true);
            itemCol.setClickable(true);

            // 1. tvOS Specular Glass Card Container
            FrameLayout card = new FrameLayout(this);
            card.setLayoutParams(new LinearLayout.LayoutParams(cardW, cardH));
            final PremiumTvIcons.GlassCardDrawable glassBg =
                    new PremiumTvIcons.GlassCardDrawable(cardW, cardH, isTv);
            card.setBackground(glassBg);

            // Centered Real Installed Application Icon
            ImageView iconView = new ImageView(this);
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(iconSize, iconSize);
            ivLp.gravity = Gravity.CENTER;
            iconView.setLayoutParams(ivLp);
            iconView.setImageDrawable(app.icon);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(iconView);

            itemCol.addView(card);

            // 2. Real User-Visible Application Label Below Card (on shared baseline)
            TextView labelView = new TextView(this);
            labelView.setText(app.label);
            labelView.setTextColor(0xFFD0D7DE);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            labelView.setGravity(Gravity.CENTER);
            labelView.setSingleLine(true);
            labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labelView.setPadding(0, (int) (7 * density), 0, 0);
            labelView.setShadowLayer(4f * density, 0, 1.5f * density, 0xEE000000);
            itemCol.addView(labelView);

            // D-pad Focus and Ambient Glow handling
            itemCol.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    currentFocusRow = 0;
                    currentFocusCol = colIndex;

                    // tvOS-inspired 1.09x scale and subtle lift on the card itself
                    card.animate()
                            .scaleX(1.09f)
                            .scaleY(1.09f)
                            .translationY(-6 * density)
                            .setDuration(240)
                            .start();

                    glassBg.setFocused(true);
                    labelView.setTextColor(Color.WHITE);
                    labelView.setShadowLayer(6f * density, 0, 2f * density, 0xFF000000);

                    // Dynamic Ambient Glow behind the card using real icon's dominant color
                    int[] loc = new int[2];
                    card.getLocationOnScreen(loc);
                    float cx = loc[0] + card.getWidth() / 2f;
                    float cy = loc[1] + card.getHeight() / 2f;
                    glowView.setFocusTarget(app.dominantColor, cx, cy);

                } else {
                    card.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationY(0)
                            .setDuration(190)
                            .start();

                    glassBg.setFocused(false);
                    labelView.setTextColor(0xFFD0D7DE);
                    labelView.setShadowLayer(4f * density, 0, 1.5f * density, 0xEE000000);
                }
            });

            itemCol.setOnClickListener(v -> launchAppOrFeature(app.packageName, app.label));

            mainShelfRow.addView(itemCol);
            focusableAppTiles.add(itemCol);
        }
    }

    // --- ALGERIAN CHANNELS SHELF & ALL APPS BUTTON ---
    private void populateChannelsShelf() {
        channelsRow.removeAllViews();
        focusableChannelTiles.clear();
        float density = getResources().getDisplayMetrics().density;

        String[][] channels = {
                {ChannelsHelper.CH_ENTV, "ENTV", "dz.entv"},
                {ChannelsHelper.CH_CANAL_ALGERIE, "Canal Algérie", "dz.canal"},
                {ChannelsHelper.CH_ECHOROUK, "Echorouk TV", "dz.echorouk"},
                {ChannelsHelper.CH_ENNAHAR, "Ennahar TV", "dz.ennahar"}
        };

        final int badgeW = (int) (118 * density);
        final int badgeH = (int) (54 * density);

        for (int i = 0; i < channels.length; i++) {
            final String chId = channels[i][0];
            final String label = channels[i][1];
            final int colIndex = i;

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setClipChildren(false);
            col.setClipToPadding(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    badgeW,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.rightMargin = (int) (14 * density);
            col.setLayoutParams(lp);

            col.setFocusable(true);
            col.setClickable(true);

            // Channel vector badge
            ImageView badgeView = new ImageView(this);
            badgeView.setLayoutParams(new LinearLayout.LayoutParams(badgeW, badgeH));
            final ChannelLogos.ChannelBadgeDrawable badge = ChannelLogos.createChannelBadge(chId, badgeW, badgeH);
            badgeView.setImageDrawable(badge);
            col.addView(badgeView);

            // Channel text label below badge (matching reference screenshot)
            TextView labelView = new TextView(this);
            labelView.setText(label);
            labelView.setTextColor(0xFFD0D7DE);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            labelView.setGravity(Gravity.CENTER);
            labelView.setPadding(0, (int) (4 * density), 0, 0);
            labelView.setShadowLayer(3f * density, 0, 1.2f * density, 0xEE000000);
            col.addView(labelView);

            col.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    currentFocusRow = 1;
                    currentFocusCol = colIndex;
                    badgeView.animate().scaleX(1.08f).scaleY(1.08f).translationY(-4 * density).setDuration(200).start();
                    badge.setFocused(true);
                    labelView.setTextColor(Color.WHITE);
                    labelView.setShadowLayer(5f * density, 0, 1.5f * density, 0xFF000000);
                } else {
                    badgeView.animate().scaleX(1.0f).scaleY(1.0f).translationY(0).setDuration(160).start();
                    badge.setFocused(false);
                    labelView.setTextColor(0xFFD0D7DE);
                    labelView.setShadowLayer(3f * density, 0, 1.2f * density, 0xEE000000);
                }
            });

            col.setOnClickListener(v -> ChannelsHelper.launchChannel(this, chId, label));
            col.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_UP &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                    ChannelsHelper.launchChannel(MainActivity.this, chId, label);
                    return true;
                }
                return false;
            });

            channelsRow.addView(col);
            focusableChannelTiles.add(col);
        }

        // Spacer to push All Apps button to far right
        View sp = new View(this);
        channelsRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));

        // All Apps Button (Far Right, matches reference screenshot)
        LinearLayout allAppsCol = new LinearLayout(this);
        allAppsCol.setOrientation(LinearLayout.VERTICAL);
        allAppsCol.setGravity(Gravity.CENTER_HORIZONTAL);
        allAppsCol.setClipChildren(false);
        allAppsCol.setClipToPadding(false);

        FrameLayout allAppsBtn = new FrameLayout(this);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                (int) (175 * density),
                badgeH
        );
        allAppsBtn.setLayoutParams(btnLp);
        allAppsCol.addView(allAppsBtn);

        // Invisible placeholder label to align baseline with channel items
        TextView spacerLabel = new TextView(this);
        spacerLabel.setText(" ");
        spacerLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        spacerLabel.setPadding(0, (int) (4 * density), 0, 0);
        allAppsCol.addView(spacerLabel);

        allAppsCol.setFocusable(true);
        allAppsCol.setClickable(true);

        GradientDrawable btnGd = new GradientDrawable();
        btnGd.setCornerRadius(16 * density);
        btnGd.setColor(0xFF101720);
        btnGd.setStroke((int) (1 * density), 0xFF27313A);
        allAppsBtn.setBackground(btnGd);

        LinearLayout btnInner = new LinearLayout(this);
        btnInner.setOrientation(LinearLayout.HORIZONTAL);
        btnInner.setGravity(Gravity.CENTER);

        TextView gridIcon = new TextView(this);
        gridIcon.setText("⊞ ");
        gridIcon.setTextColor(0xFF90A4AE);
        gridIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        btnInner.addView(gridIcon);

        TextView btnText = new TextView(this);
        btnText.setText("جميع التطبيقات  ›");
        btnText.setTextColor(Color.WHITE);
        btnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        btnInner.addView(btnText);

        allAppsBtn.addView(btnInner);

        allAppsCol.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                currentFocusRow = 1;
                currentFocusCol = channels.length;
                btnGd.setColor(0xFF1E2836);
                btnGd.setStroke((int) (2 * density), 0xFF00C878);
                allAppsBtn.animate().scaleX(1.06f).scaleY(1.06f).translationY(-3 * density).setDuration(200).start();
                btnText.setTextColor(Color.WHITE);
            } else {
                btnGd.setColor(0xFF101720);
                btnGd.setStroke((int) (1 * density), 0xFF27313A);
                allAppsBtn.animate().scaleX(1.0f).scaleY(1.0f).translationY(0).setDuration(160).start();
                btnText.setTextColor(0xFFE0E0E0);
            }
        });

        allAppsCol.setOnClickListener(v -> showAllAppsScreen());

        channelsRow.addView(allAppsCol);
        focusableChannelTiles.add(allAppsCol);
    }

    private void loadWallpaperBitmap() {
        if (cachedWallpaperBmp != null && !cachedWallpaperBmp.isRecycled()) {
            wallpaperView.setImageBitmap(cachedWallpaperBmp);
            return;
        }

        try {
            InputStream is = getAssets().open(currentWallpaper);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            // In Low-RAM mode, decode directly in RGB_565 (2 bytes per pixel, uses only ~1.8 MB RAM!)
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inDither = true;

            cachedWallpaperBmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();

            if (cachedWallpaperBmp != null) {
                wallpaperView.setImageBitmap(cachedWallpaperBmp);
            }
        } catch (Exception e) {
            wallpaperView.setBackgroundColor(Color.BLACK);
        }
    }

    private void launchAppOrFeature(final String pkg, final String label) {
        if ("all_apps".equals(pkg)) {
            showAllAppsScreen();
            return;
        }
        if ("settings".equals(pkg)) {
            PasswordManager.authenticate(this, "الإعدادات", () -> showSettingsDialog(), null);
            return;
        }

        // System Settings Protection: Require password before opening Android TV Settings from launcher
        if (PasswordManager.isSystemSettingsPackage(pkg)) {
            PasswordManager.authenticate(this, label, () -> executeAppLaunch(pkg, label), null);
            return;
        }

        // Ordinary applications (Yacine TV, SmartTube, YouTube, Netflix, Prime Video, etc.) launch immediately
        executeAppLaunch(pkg, label);
    }

    private void executeAppLaunch(String pkg, String label) {
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(pkg);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(launch);
                return;
            } catch (Exception ignored) {}
        }

        // Try direct activity launch if launch intent was null
        try {
            Intent query = new Intent(Intent.ACTION_MAIN).setPackage(pkg);
            List<ResolveInfo> list = pm.queryIntentActivities(query, 0);
            if (!list.isEmpty()) {
                Intent explicit = new Intent(Intent.ACTION_MAIN);
                explicit.setClassName(pkg, list.get(0).activityInfo.name);
                explicit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(explicit);
                return;
            }
        } catch (Exception ignored) {}

        // Missing app fallback
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
    }

    /**
     * All Apps Leanback Screen — Complete & Accurate.
     * Displays all eligible launchable apps on the TV box in a 6-column grid
     * with real installed icons, real labels, D-pad navigation, and Back key dismiss.
     */
    public void showAllAppsScreen() {
        if (allAppsOverlay == null) return;
        allAppsOverlay.removeAllViews();
        allAppsOverlay.setVisibility(View.VISIBLE);

        float density = getResources().getDisplayMetrics().density;
        List<AppItem> allApps = AppScanner.scanAllApps(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (40 * density), (int) (24 * density), (int) (40 * density), (int) (20 * density));
        layout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Top Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hLp.bottomMargin = (int) (18 * density);
        header.setLayoutParams(hLp);

        TextView title = new TextView(this);
        title.setText("جميع التطبيقات");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        header.addView(title);

        TextView countBadge = new TextView(this);
        countBadge.setText(allApps.size() + " تطبيق مثبت");
        countBadge.setTextColor(0xFF00C878);
        countBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        countBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        countBadge.setPadding((int) (12 * density), (int) (4 * density), (int) (12 * density), (int) (4 * density));
        GradientDrawable badgeGd = new GradientDrawable();
        badgeGd.setCornerRadius(10 * density);
        badgeGd.setColor(0x2200C878);
        badgeGd.setStroke((int) (1 * density), 0x6600C878);
        countBadge.setBackground(badgeGd);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bLp.leftMargin = (int) (16 * density);
        bLp.rightMargin = (int) (16 * density);
        header.addView(countBadge, bLp);

        View sp = new View(this);
        header.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView backHint = new TextView(this);
        backHint.setText("اضغط ⮌ (BACK) للعودة للرئيسية");
        backHint.setTextColor(0xFF90A4AE);
        backHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        header.addView(backHint);

        layout.addView(header);

        // Grid ScrollView
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);

        final int cols = 6;
        final int cardW = (int) (144 * density);
        final int cardH = (int) (104 * density);
        final int iconSize = (int) (64 * density);

        LinearLayout currentRow = null;
        View firstAppView = null;

        for (int i = 0; i < allApps.size(); i++) {
            if (i % cols == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setClipChildren(false);
                currentRow.setClipToPadding(false);
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                rLp.bottomMargin = (int) (16 * density);
                currentRow.setLayoutParams(rLp);
                grid.addView(currentRow);
            }

            final AppItem app = allApps.get(i);

            LinearLayout itemCol = new LinearLayout(this);
            itemCol.setOrientation(LinearLayout.VERTICAL);
            itemCol.setGravity(Gravity.CENTER_HORIZONTAL);
            itemCol.setClipChildren(false);
            itemCol.setClipToPadding(false);

            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                    cardW,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i % cols < cols - 1) {
                colLp.rightMargin = (int) (14 * density);
            }
            itemCol.setLayoutParams(colLp);
            itemCol.setFocusable(true);
            itemCol.setClickable(true);

            FrameLayout card = new FrameLayout(this);
            card.setLayoutParams(new LinearLayout.LayoutParams(cardW, cardH));
            final PremiumTvIcons.GlassCardDrawable glassBg =
                    new PremiumTvIcons.GlassCardDrawable(cardW, cardH, false);
            card.setBackground(glassBg);

            ImageView iconView = new ImageView(this);
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(iconSize, iconSize);
            ivLp.gravity = Gravity.CENTER;
            iconView.setLayoutParams(ivLp);
            iconView.setImageDrawable(app.icon);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(iconView);

            itemCol.addView(card);

            TextView labelView = new TextView(this);
            labelView.setText(app.label);
            labelView.setTextColor(0xFFD0D7DE);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            labelView.setGravity(Gravity.CENTER);
            labelView.setSingleLine(true);
            labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labelView.setPadding(0, (int) (6 * density), 0, 0);
            itemCol.addView(labelView);

            itemCol.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    card.animate().scaleX(1.08f).scaleY(1.08f).translationY(-5 * density).setDuration(200).start();
                    glassBg.setFocused(true);
                    labelView.setTextColor(Color.WHITE);
                } else {
                    card.animate().scaleX(1.0f).scaleY(1.0f).translationY(0).setDuration(160).start();
                    glassBg.setFocused(false);
                    labelView.setTextColor(0xFFD0D7DE);
                }
            });

            itemCol.setOnClickListener(v -> {
                hideAllAppsOverlay();
                launchAppOrFeature(app.packageName, app.label);
            });

            if (currentRow != null) {
                currentRow.addView(itemCol);
            }
            if (firstAppView == null) {
                firstAppView = itemCol;
            }
        }

        scroll.addView(grid);
        layout.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        allAppsOverlay.addView(layout);

        final View fv = firstAppView;
        handler.postDelayed(() -> {
            if (fv != null) fv.requestFocus();
        }, 100);
    }

    public void hideAllAppsOverlay() {
        if (allAppsOverlay != null && allAppsOverlay.getVisibility() == View.VISIBLE) {
            allAppsOverlay.setVisibility(View.GONE);
            allAppsOverlay.removeAllViews();
            if (!focusableAppTiles.isEmpty()) {
                focusableAppTiles.get(0).requestFocus();
            }
        }
    }

    private void showSettingsDialog() {
        String[] options = {
                "🔑 تغيير كلمة مرور المسؤول / Change Admin Password",
                "🕌 إعدادات وضع الصلاة والنداء الصوتي / Salat Mode & Audio",
                "🧪 اختبار شاشة ونداء الصلاة (5 صلوات) / Test Salat Mode (Dev)",
                "🏠 تعيين 1962 كواجهة رئيسية افتراضية / Set 1962 as Default Home",
                "🔄 العودة إلى الواجهة الأصلية / Back to Original Launcher (Exit 1962)",
                "🖼️ تغيير الخلفية (مقام الشهيد / جبال الأوراس خنشلة)",
                "⚙️ إعدادات أندرويد / Android System Settings",
                "📶 شبكة Wi-Fi / Wi-Fi Network Settings",
                "📍 إعدادات موقع الصلاة والمدينة / Prayer City & Location",
                "🌤️ إعدادات الطقس والمدينة / Weather Settings",
                "⚠️ إعادة ضبط إعدادات 1962 / Reset Launcher Preferences"
        };

        new AlertDialog.Builder(this)
                .setTitle("1962 — لوحة تحكم المسؤول (محمية)")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        PasswordManager.showChangePasswordDialog(this);
                    } else if (which == 1) {
                        SalatUiHelper.showSalatSettingsDialog(this, prayerManager, () -> updateDateTimeAndPrayers());
                    } else if (which == 2) {
                        SalatUiHelper.showDeveloperTestDialog(this);
                    } else if (which == 3) {
                        try {
                            Intent homeSettings = new Intent(Settings.ACTION_HOME_SETTINGS);
                            homeSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(homeSettings);
                        } catch (Exception e) {
                            try {
                                Intent manageApps = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                                manageApps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(manageApps);
                            } catch (Exception ex) {
                                startActivity(new Intent(Settings.ACTION_SETTINGS));
                            }
                        }
                    } else if (which == 4) {
                        SafetyWatchdog.launchStockLauncher(this);
                    } else if (which == 5) {
                        // Toggle wallpaper
                        currentWallpaper = "wallpaper_algiers.jpg".equals(currentWallpaper) ?
                                "wallpaper_khenchela.jpg" : "wallpaper_algiers.jpg";
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit().putString(KEY_WALLPAPER, currentWallpaper).apply();
                        if (cachedWallpaperBmp != null) {
                            cachedWallpaperBmp.recycle();
                            cachedWallpaperBmp = null;
                        }
                        loadWallpaperBitmap();
                        Toast.makeText(this, "تم تغيير الخلفية", Toast.LENGTH_SHORT).show();
                    } else if (which == 6) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } else if (which == 7) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    } else if (which == 8) {
                        SalatUiHelper.showCitySelectorDialog(this, prayerManager, () -> updateDateTimeAndPrayers());
                    } else if (which == 9) {
                        showWeatherSettingsDialog();
                    } else if (which == 10) {
                        resetLauncherPreferences();
                    }
                })
                .show();
    }

    private void showLanguageDialog() {
        String[] languages = {
                "العربية (Default RTL)",
                "Français",
                "English",
                "ⵜⴰⵎⴰزيغت (Tamazight)"
        };
        new AlertDialog.Builder(this)
                .setTitle("تغيير لغة الواجهة")
                .setItems(languages, (dialog, which) -> {
                    if (which == 0) currentLang = Strings.Lang.ARABIC;
                    else if (which == 1) currentLang = Strings.Lang.FRENCH;
                    else if (which == 2) currentLang = Strings.Lang.ENGLISH;
                    else if (which == 3) currentLang = Strings.Lang.TAMAZIGHT;
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit().putString(KEY_LANG, currentLang.name()).apply();
                    Toast.makeText(this, "تم تغيير اللغة: " + languages[which], Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .show();
    }

    private void showWeatherSettingsDialog() {
        String[] cities = {
                "قايس، خنشلة (Kaïs, Khenchela) 📍",
                "الجزائر العاصمة (Algiers)",
                "وهران (Oran)",
                "قسنطينة (Constantine)",
                "سطيف (Sétif)",
                "باتنة (Batna)",
                "عنابة (Annaba)",
                "بسكرة (Biskra)",
                "ورقلة (Ouargla)"
        };
        double[][] coords = {
                {35.4925, 6.9344},
                {36.7538, 3.0588},
                {35.6971, -0.6308},
                {36.3650, 6.6147},
                {36.1905, 5.4137},
                {35.5558, 6.1741},
                {36.9000, 7.7667},
                {34.8500, 5.7333},
                {31.9500, 5.3333}
        };
        new AlertDialog.Builder(this)
                .setTitle("إعدادات الطقس والمدينة")
                .setItems(cities, (dialog, which) -> {
                    if (prayerManager != null) {
                        prayerManager.setLocation(coords[which][0], coords[which][1]);
                    }
                    if (prayerSubLocation != null) {
                        prayerSubLocation.setText(which == 0 ? "📍 قايس، خنشلة" : "📍 " + cities[which].split(" \\(")[0]);
                    }
                    updateDateTimeAndPrayers();
                    Toast.makeText(this, "تم تعيين المدينة: " + cities[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showPrayerSettingsDialog() {
        SalatUiHelper.showSalatSettingsDialog(this, prayerManager, () -> updateDateTimeAndPrayers());
    }

    private void showDateSettingsDialog() {
        String[] options = {
                "التقويم الهجري: تعديل يدوي (+1 يوم)",
                "التقويم الهجري: توقيت رسمي (0)",
                "التقويم الهجري: تعديل يدوي (-1 يوم)"
        };
        new AlertDialog.Builder(this)
                .setTitle("إعدادات التقويم الهجري")
                .setItems(options, (dialog, which) -> {
                    Toast.makeText(this, "تم تحديث التقويم", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void resetLauncherPreferences() {
        new AlertDialog.Builder(this)
                .setTitle("إعادة ضبط إعدادات 1962")
                .setMessage("هل أنت متأكد من رغبتك في إعادة ضبط إعدادات الواجهة إلى الوضع الافتراضي؟")
                .setPositiveButton("إعادة ضبط", (dialog, which) -> {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this, "تمت إعادة ضبط إعدادات الواجهة", Toast.LENGTH_LONG).show();
                    recreate();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void updateDateTimeAndPrayers() {
        Date now = new Date();
        long nowMs = now.getTime();

        // 1. Top Clock & Date
        if (clockView != null && clockTimeFormat != null) {
            clockView.setText(clockTimeFormat.format(now));
        }
        if (dateView != null && headerDateFormat != null) {
            String rawDate = headerDateFormat.format(now);
            dateView.setText(DateHelper.normalizeArabicDigits(rawDate));
        }

        // 2. Today Card
        if (todayGregView != null) {
            todayGregView.setText(DateHelper.getGregorianDateString(now, currentLang));
        }
        if (todayHijriView != null) {
            todayHijriView.setText(DateHelper.getHijriDateString(now, currentLang));
        }

        // 3. Prayer Tracker
        if (prayerManager != null) {
            prayerManager.update(nowMs);

            if (prayerSubLocation != null) {
                int cityIdx = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt("salat_city_index", 0);
                AlgerianCities.City c = AlgerianCities.getCityByIndex(cityIdx);
                prayerSubLocation.setText("📍 " + c.nameAr);
            }

            if (prayerSubCountdown != null) {
                prayerSubCountdown.setText(prayerManager.getNextPrayerCountdownLabel(currentLang));
            }

            int activeIdx = prayerManager.getActiveIndex();
            for (int i = 0; i < 6; i++) {
                if (prayerTimeViews[i] != null) {
                    prayerTimeViews[i].setText(prayerManager.getFormattedTime(i));
                }
                boolean isCurrent = (i == activeIdx);
                if (prayerRowViews[i] != null) {
                    if (isCurrent) {
                        prayerRowViews[i].setBackground(activePillDrawable);
                    } else {
                        prayerRowViews[i].setBackground(null);
                    }
                }
                if (prayerNameViews[i] != null) {
                    String baseName = prayerManager.getPrayerName(i, currentLang);
                    prayerNameViews[i].setText((isCurrent ? "🔊 " : "") + baseName);
                    prayerNameViews[i].setTextColor(isCurrent ? Color.BLACK : 0xFFCCCCCC);
                    prayerNameViews[i].setTypeface(Typeface.create("sans-serif", isCurrent ? Typeface.BOLD : Typeface.NORMAL));
                }
                if (prayerTimeViews[i] != null) {
                    prayerTimeViews[i].setTextColor(isCurrent ? Color.BLACK : Color.WHITE);
                }
            }
        }
    }

    private void startClock() {
        if (clockRunnable == null) {
            clockRunnable = new Runnable() {
                @Override
                public void run() {
                    updateDateTimeAndPrayers();
                    handler.postDelayed(this, 1000);
                }
            };
        }
        handler.removeCallbacks(clockRunnable);
        handler.post(clockRunnable);
    }

    private void stopClock() {
        if (clockRunnable != null) {
            handler.removeCallbacks(clockRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        if (allAppsOverlay != null && allAppsOverlay.getVisibility() == View.VISIBLE) {
            hideAllAppsOverlay();
            return;
        }
        // Keep 1962 launcher active on home screen
    }

    // --- D-PAD REMOTE CONTROL ---
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (allAppsOverlay != null && allAppsOverlay.getVisibility() == View.VISIBLE) {
                hideAllAppsOverlay();
                return true;
            }
            return true; // Consume Back on home screen so launcher never closes to empty screen
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (allAppsOverlay != null && allAppsOverlay.getVisibility() == View.VISIBLE) {
                return super.onKeyDown(keyCode, event);
            }
            if (currentFocusRow == 0 && !focusableChannelTiles.isEmpty()) {
                int targetCol = Math.min(currentFocusCol, focusableChannelTiles.size() - 1);
                focusableChannelTiles.get(targetCol).requestFocus();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (allAppsOverlay != null && allAppsOverlay.getVisibility() == View.VISIBLE) {
                return super.onKeyDown(keyCode, event);
            }
            if (currentFocusRow == 1 && !focusableAppTiles.isEmpty()) {
                int targetCol = Math.min(currentFocusCol, focusableAppTiles.size() - 1);
                focusableAppTiles.get(targetCol).requestFocus();
                return true;
            } else if (currentFocusRow == 0) {
                settingsPill.requestFocus();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
