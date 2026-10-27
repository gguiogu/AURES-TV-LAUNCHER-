package dz.vision.launcher1962;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Full-Screen Salat Mode Activity for AURÈS TV Launcher.
 * Features:
 * - Natural Arabic voice announcement from bundled studio MP3 assets.
 * - Islamic crescent / mosque aesthetic with Aurès Mountains background.
 * - Exact Arabic typography for all 5 prayers, including Fajr's specific subtitle.
 * - Live clock, scheduled time, next prayer and real-time countdown.
 * - Pauses entertainment audio.
 * - Full D-pad TV remote accessible (OK/Center stops voice, Back dismisses).
 */
public class SalatActivity extends Activity {

    public static final String PREF_DISMISS_SECONDS = "salat_dismiss_seconds"; // default 30

    private FrameLayout rootFrame;
    private ImageView backdropView;
    private TextView prayerTitleView;
    private TextView prayerSubtitleView;
    private TextView announcementTextView;
    private TextView clockView;
    private TextView prayerScheduledTimeView;
    private TextView nextPrayerCountdownView;
    private TextView cityView;
    private TextView speechIndicator;
    private TextView btnDismiss;
    private TextView btnStopAudio;

    private int prayerIndex = 0;
    private long scheduledTimeMs = 0;
    private boolean isVoiceSpeaking = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoDismissRunnable;
    private Runnable liveClockRunnable;
    private int dismissTimeoutSeconds = 30;

    private PrayerManager prayerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn screen on and show above lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Force RTL for Arabic layout
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        prayerIndex = getIntent().getIntExtra(SalatAlarmScheduler.EXTRA_PRAYER_INDEX, 0);
        scheduledTimeMs = getIntent().getLongExtra(SalatAlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis());

        SharedPreferences sp = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        dismissTimeoutSeconds = sp.getInt(PREF_DISMISS_SECONDS, 30);

        // Initialize PrayerManager for Next Prayer Countdown
        prayerManager = new PrayerManager();
        int cityIndex = sp.getInt("salat_city_index", 0);
        AlgerianCities.City city = AlgerianCities.getCityByIndex(cityIndex);
        prayerManager.setLocation(city.latitude, city.longitude);

        // Prevent re-playing missed prayer if waking up hours late
        long now = System.currentTimeMillis();
        int lateMins = sp.getInt(SalatAlarmScheduler.PREF_LATE_POLICY_MINUTES, 5);
        long maxLateMs = lateMins * 60 * 1000L;
        if (scheduledTimeMs > 0 && (now - scheduledTimeMs) > maxLateMs) {
            SalatAlarmScheduler.scheduleNextPrayerAlarm(this, prayerManager);
            finish();
            return;
        }

        // Mark announced atomically and schedule next prayer alarm
        SalatAlarmScheduler.markAnnounced(this, prayerIndex, now);
        SalatAlarmScheduler.scheduleNextPrayerAlarm(this, prayerManager);

        buildUi(city.nameAr);

        // Start Arabic voice announcement pipeline (ADHANALL.mp3)
        startAnnouncement();

        // Start Live Clock and Countdown timer
        startLiveTimer();

        handleIncomingDialogIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingDialogIntent(intent);
    }

    private void handleIncomingDialogIntent(Intent intent) {
        if (intent == null) return;
        final String diag = intent.getStringExtra("extra_show_dialog");
        if (diag != null || intent.getBooleanExtra("extra_open_settings", false)) {
            if (autoDismissRunnable != null) handler.removeCallbacks(autoDismissRunnable);
            handler.postDelayed(() -> {
                if ("assets".equals(diag)) {
                    SalatUiHelper.showAssetIntegrityDialog(SalatActivity.this);
                } else if ("test_mode".equals(diag)) {
                    SalatUiHelper.showDeveloperTestDialog(SalatActivity.this);
                } else if ("audio_mode".equals(diag)) {
                    SalatUiHelper.showAudioModeDialog(SalatActivity.this, null);
                } else if ("volume".equals(diag)) {
                    SalatUiHelper.showVolumeDialog(SalatActivity.this, null);
                } else if ("import_adhan".equals(diag)) {
                    SalatUiHelper.showAdhanImportDialog(SalatActivity.this, null);
                } else if ("tts_status".equals(diag)) {
                    SalatUiHelper.showTtsStatusDialog(SalatActivity.this);
                } else {
                    SalatUiHelper.showSalatSettingsDialog(SalatActivity.this, prayerManager, () -> updateNextPrayerDisplay());
                }
            }, 300);
        }
    }

    private void buildUi(String cityName) {
        rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(0xFF070B11);

        // 1. Cinematic Aurès Mountains Wallpaper Background
        backdropView = new ImageView(this);
        backdropView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadWallpaperImage();
        rootFrame.addView(backdropView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 2. Dark Islamic Emerald / Slate Vignette Scrim
        View scrim = new View(this);
        GradientDrawable vig = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF2070D14, 0xDD0A141E, 0xFA05080E}
        );
        rootFrame.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. Main Center Content Container with Frosted Islamic Card
        LinearLayout centerBox = new LinearLayout(this);
        centerBox.setOrientation(LinearLayout.VERTICAL);
        centerBox.setGravity(Gravity.CENTER);
        centerBox.setPadding(48, 14, 48, 16);

        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(0xD9060C14); // Deep frosted obsidian card
        boxBg.setCornerRadius(28);
        boxBg.setStroke(2, 0x4400E676); // Subtle emerald border
        centerBox.setBackground(boxBg);

        // Top Crescent & Star Symbol with Emerald Glow
        TextView crescentSymbol = new TextView(this);
        crescentSymbol.setText("☪");
        crescentSymbol.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        crescentSymbol.setTextColor(0xFF00E676);
        crescentSymbol.setGravity(Gravity.CENTER);
        crescentSymbol.setShadowLayer(12, 0, 0, 0xFF00E676);
        centerBox.addView(crescentSymbol);

        // City & Clock Header
        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER);
        metaRow.setPadding(0, 2, 0, 8);

        cityView = new TextView(this);
        cityView.setText("📍 " + cityName + " (الجزائر)");
        cityView.setTextColor(0xFF00E5FF);
        cityView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        cityView.setTypeface(Typeface.DEFAULT_BOLD);
        metaRow.addView(cityView);

        TextView dot = new TextView(this);
        dot.setText(" • ");
        dot.setTextColor(0xFF6B7C8F);
        dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        metaRow.addView(dot);

        clockView = new TextView(this);
        clockView.setText(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()));
        clockView.setTextColor(0xFFFFFFFF);
        clockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        clockView.setTypeface(Typeface.DEFAULT_BOLD);
        metaRow.addView(clockView);

        centerBox.addView(metaRow);

        // Prayer Main Title (e.g. صلاة الفجر)
        prayerTitleView = new TextView(this);
        prayerTitleView.setText(PrayerAnnouncements.getPrayerTitle(prayerIndex));
        prayerTitleView.setTextColor(0xFFFFFFFF);
        prayerTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        prayerTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        prayerTitleView.setShadowLayer(8, 0, 2, 0xDD000000);
        prayerTitleView.setGravity(Gravity.CENTER);
        centerBox.addView(prayerTitleView);

        // Subtitle (for Fajr: وتسمى أيضاً صلاة الصبح)
        prayerSubtitleView = new TextView(this);
        if (prayerIndex == 0) {
            prayerSubtitleView.setText(PrayerAnnouncements.FAJR_SUBTITLE);
            prayerSubtitleView.setVisibility(View.VISIBLE);
        } else {
            prayerSubtitleView.setVisibility(View.GONE);
        }
        prayerSubtitleView.setTextColor(0xFF00E676);
        prayerSubtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        prayerSubtitleView.setTypeface(Typeface.DEFAULT_BOLD);
        prayerSubtitleView.setGravity(Gravity.CENTER);
        prayerSubtitleView.setPadding(0, 2, 0, 8);
        prayerSubtitleView.setShadowLayer(8, 0, 2, 0xDD000000);
        centerBox.addView(prayerSubtitleView);

        // Full Exact Announcement Text Box
        FrameLayout announcementCard = new FrameLayout(this);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xEE08131E);
        cardBg.setCornerRadius(18);
        cardBg.setStroke(2, 0xFF00E676);
        announcementCard.setBackground(cardBg);
        announcementCard.setPadding(36, 16, 36, 16);

        announcementTextView = new TextView(this);
        announcementTextView.setText(PrayerAnnouncements.getAnnouncementText(prayerIndex));
        announcementTextView.setTextColor(0xFFFFFFFF);
        announcementTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        announcementTextView.setTypeface(Typeface.DEFAULT_BOLD);
        announcementTextView.setGravity(Gravity.CENTER);
        announcementCard.addView(announcementTextView);

        LinearLayout.LayoutParams acParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        acParams.setMargins(0, 10, 0, 12);
        centerBox.addView(announcementCard, acParams);

        // Scheduled time display
        prayerScheduledTimeView = new TextView(this);
        String schedStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(scheduledTimeMs));
        prayerScheduledTimeView.setText("الوقت المحدد للأذان: " + schedStr);
        prayerScheduledTimeView.setTextColor(0xFFB0BEC5);
        prayerScheduledTimeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        prayerScheduledTimeView.setGravity(Gravity.CENTER);
        centerBox.addView(prayerScheduledTimeView);

        // Next Prayer & Countdown
        nextPrayerCountdownView = new TextView(this);
        nextPrayerCountdownView.setTextColor(0xFF81D4FA);
        nextPrayerCountdownView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        nextPrayerCountdownView.setGravity(Gravity.CENTER);
        nextPrayerCountdownView.setPadding(0, 4, 0, 6);
        updateNextPrayerDisplay();
        centerBox.addView(nextPrayerCountdownView);

        // Speech Audio Indicator
        speechIndicator = new TextView(this);
        speechIndicator.setText("🔊 جاري نداء الصلاة بالصوت العربي...");
        speechIndicator.setTextColor(0xFF00E5FF);
        speechIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        speechIndicator.setTypeface(Typeface.DEFAULT_BOLD);
        speechIndicator.setPadding(0, 6, 0, 14);
        speechIndicator.setGravity(Gravity.CENTER);
        centerBox.addView(speechIndicator);

        // Bottom Remote Buttons (Stop Audio & Dismiss)
        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setGravity(Gravity.CENTER);

        btnStopAudio = createActionButton("كتم الصوت (Stop Audio)", 0xFF202A38, 0xFFFFFFFF, v -> {
            SalatSpeechManager.getInstance(this).stopAll();
            speechIndicator.setText("تم إيقاف الصوت");
        });

        btnDismiss = createActionButton("إغلاق الشاشة (Dismiss)", 0xFF00E676, 0xFF000000, v -> {
            dismissSalatScreen();
        });

        TextView btnSettings = createActionButton("⚙️ إعدادات الصوت (Audio Settings)", 0xFF1E2836, 0xFF00E5FF, v -> {
            if (autoDismissRunnable != null) handler.removeCallbacks(autoDismissRunnable);
            SalatUiHelper.showSalatSettingsDialog(this, prayerManager, () -> updateNextPrayerDisplay());
        });

        buttonsRow.addView(btnStopAudio);
        View gap1 = new View(this);
        buttonsRow.addView(gap1, new LinearLayout.LayoutParams(16, 1));
        buttonsRow.addView(btnDismiss);
        View gap2 = new View(this);
        buttonsRow.addView(gap2, new LinearLayout.LayoutParams(16, 1));
        buttonsRow.addView(btnSettings);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 10, 0, 0);
        centerBox.addView(buttonsRow, btnParams);

        FrameLayout.LayoutParams cbParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbParams.gravity = Gravity.CENTER;
        rootFrame.addView(centerBox, cbParams);

        setContentView(rootFrame);

        // Initially focus Dismiss button so user can easily exit with OK or Back
        handler.postDelayed(() -> {
            if (btnDismiss != null) btnDismiss.requestFocus();
        }, 200);
    }

    private void updateNextPrayerDisplay() {
        if (prayerManager != null && nextPrayerCountdownView != null) {
            prayerManager.update(System.currentTimeMillis());
            String nextText = "الصلاة القادمة: " + prayerManager.getNextPrayerCountdownLabel(Strings.Lang.ARABIC);
            nextPrayerCountdownView.setText(nextText);
        }
    }

    private void startLiveTimer() {
        liveClockRunnable = new Runnable() {
            @Override
            public void run() {
                if (clockView != null) {
                    clockView.setText(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()));
                }
                updateNextPrayerDisplay();
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(liveClockRunnable, 1000);
    }

    private TextView createActionButton(String text, int bgColor, int textColor, View.OnClickListener click) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setFocusable(true);
        btn.setClickable(true);
        btn.setPadding(26, 10, 26, 10);

        GradientDrawable defBg = new GradientDrawable();
        defBg.setColor(bgColor);
        defBg.setCornerRadius(20);

        GradientDrawable fcsBg = new GradientDrawable();
        fcsBg.setColor(0xFFFFFFFF);
        fcsBg.setCornerRadius(20);
        fcsBg.setStroke(3, 0xFF00E5FF);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_focused}, fcsBg);
        sld.addState(new int[]{}, defBg);
        btn.setBackground(sld);

        btn.setOnFocusChangeListener((v, hasFocus) -> {
            btn.setTextColor(hasFocus ? 0xFF000000 : textColor);
            v.setScaleX(hasFocus ? 1.08f : 1.0f);
            v.setScaleY(hasFocus ? 1.08f : 1.0f);
        });

        btn.setOnClickListener(click);
        return btn;
    }

    private void startAnnouncement() {
        SalatSpeechManager sm = SalatSpeechManager.getInstance(this);
        SalatSpeechManager.State state = sm.getCurrentState();

        SalatSpeechManager.PlaybackListener listener = new SalatSpeechManager.PlaybackListener() {
            @Override
            public void onSpeechStarted() {
                isVoiceSpeaking = true;
                if (speechIndicator != null) {
                    speechIndicator.setText("🔊 جاري نداء الصلاة بالصوت العربي...");
                    speechIndicator.setTextColor(0xFF00E676);
                }
            }

            @Override
            public void onSpeechCompleted() {
                isVoiceSpeaking = false;
                if (speechIndicator != null) {
                    speechIndicator.setText("✓ اكتمل نداء الصلاة");
                    speechIndicator.setTextColor(0xFFB0BEC5);
                }
                scheduleAutoDismiss();
            }

            @Override
            public void onAdhanStarted() {
                if (speechIndicator != null) {
                    speechIndicator.setText("🕌 جاري رفع الأذان الكامل (ADHANALL.mp3)...");
                    speechIndicator.setTextColor(0xFF00E5FF);
                }
            }

            @Override
            public void onPlaybackFinished() {
                isVoiceSpeaking = false;
                if (speechIndicator != null) {
                    speechIndicator.setText("✓ اكتمل رفع الأذان بنجاح");
                    speechIndicator.setTextColor(0xFF00E676);
                }
                scheduleAutoDismiss();
            }

            @Override
            public void onError(String message) {
                isVoiceSpeaking = false;
                if (speechIndicator != null) {
                    speechIndicator.setText("⚠️ " + message);
                    speechIndicator.setTextColor(0xFFFF7043);
                }
                scheduleAutoDismiss();
            }
        };

        if (state == SalatSpeechManager.State.ADHAN_PLAYING) {
            listener.onAdhanStarted();
        } else if (state == SalatSpeechManager.State.VOICE_SPEAKING) {
            listener.onSpeechStarted();
        } else {
            isVoiceSpeaking = true;
            sm.announcePrayer(prayerIndex, null, listener);
        }
    }

    private void scheduleAutoDismiss() {
        if (dismissTimeoutSeconds <= 0) return; // Manual dismiss only
        if (autoDismissRunnable != null) handler.removeCallbacks(autoDismissRunnable);

        autoDismissRunnable = () -> dismissSalatScreen();
        handler.postDelayed(autoDismissRunnable, dismissTimeoutSeconds * 1000L);
    }

    private void dismissSalatScreen() {
        if (autoDismissRunnable != null) handler.removeCallbacks(autoDismissRunnable);
        if (liveClockRunnable != null) handler.removeCallbacks(liveClockRunnable);
        SalatSpeechManager.getInstance(this).stopAll();
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            // Back key always dismisses
            if (code == KeyEvent.KEYCODE_BACK) {
                dismissSalatScreen();
                return true;
            }
            // OK key stops voice if speaking
            if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
                if (isVoiceSpeaking) {
                    SalatSpeechManager.getInstance(this).stopAll();
                    if (speechIndicator != null) speechIndicator.setText("تم إيقاف الصوت");
                    isVoiceSpeaking = false;
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void loadWallpaperImage() {
        try (InputStream is = getAssets().open("wallpaper_khenchela.jpg")) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            backdropView.setImageBitmap(bmp);
        } catch (Throwable t1) {
            try (InputStream is = getAssets().open("wallpaper_algiers.jpg")) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                backdropView.setImageBitmap(bmp);
            } catch (Throwable t2) {
                backdropView.setBackgroundColor(0xFF070B11);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoDismissRunnable != null) handler.removeCallbacks(autoDismissRunnable);
        if (liveClockRunnable != null) handler.removeCallbacks(liveClockRunnable);
        SalatSpeechManager.getInstance(this).stopAll();
    }
}
