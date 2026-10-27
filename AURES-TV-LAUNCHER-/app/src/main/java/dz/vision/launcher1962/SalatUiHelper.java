package dz.vision.launcher1962;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * UI dialogs, settings management, and developer test helpers for Salat Mode and Arabic Audio announcements.
 */
public final class SalatUiHelper {

    /**
     * Main Salat Mode & Audio Settings Hub.
     */
    public static void showSalatSettingsDialog(final Activity activity, final PrayerManager prayerManager, final Runnable onUpdated) {
        final SharedPreferences sp = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = sp.getBoolean(SalatAlarmScheduler.PREF_SALAT_ENABLED, true);
        boolean showFullscreen = sp.getBoolean(SalatReceiver.PREF_SHOW_FULLSCREEN, true);
        float volume = sp.getFloat(SalatSpeechManager.PREF_VOLUME, 1.0f);
        int cityIndex = sp.getInt("salat_city_index", 0);
        AlgerianCities.City city = AlgerianCities.getCityByIndex(cityIndex);
        int dismissSec = sp.getInt(SalatActivity.PREF_DISMISS_SECONDS, 30);

        String[] options = {
                "🕌 نظام مقاطعة الوسائط والأذان التلقائي: " + (enabled ? "مفعّل (ON) ✓" : "معطّل (OFF)"),
                "🔔 تفعيل وتعطيل صلوات معينة (الفجر، الظهر، العصر، المغرب، العشاء)",
                "📺 عرض شاشة الصلاة الكاملة عند الأذان: " + (showFullscreen ? "مفعّل (ON) ✓" : "صوت فقط (خلفية)"),
                "🎵 ملف الأذان المعتمد: ADHANALL.mp3 (03:07 د - 7.15 MB)",
                "⚡ اختبار تشغيل أذان ADHANALL.mp3 فوراً (Test Interruption & Adhan)",
                "🧪 اختبار شاشات وتنبيهات الصلوات الخمس الفردية",
                "🔉 مستوى صوت الأذان والنداء: " + Math.round(volume * 100) + "%",
                "📊 تقرير نظام المقاطعة وتوافق التطبيقات (Interruption Status)",
                "📦 فحص سلامة الملفات الصوتية المدمجة في التطبيق (7/7)",
                "📍 المدينة والولاية: " + city.nameAr,
                "⏱️ مدة بقاء الشاشة التلقائية: " + (dismissSec <= 0 ? "يدوي" : dismissSec + " ثانية"),
                "🔄 استعادة الإعدادات الافتراضية للصوت والصلاة"
        };

        new AlertDialog.Builder(activity)
                .setTitle("🕌 إعدادات نظام الأذان ومقاطعة الوسائط (AURÈS TV)")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: { // Toggle enabled
                            boolean newEnabled = !enabled;
                            sp.edit().putBoolean(SalatAlarmScheduler.PREF_SALAT_ENABLED, newEnabled).apply();
                            if (newEnabled) {
                                SalatAlarmScheduler.scheduleNextPrayerAlarm(activity, prayerManager);
                                Toast.makeText(activity, "تم تفعيل نظام الأذان التلقائي ومقاطعة الوسائط", Toast.LENGTH_SHORT).show();
                            } else {
                                SalatAlarmScheduler.cancelAlarms(activity);
                                Toast.makeText(activity, "تم تعطيل نظام الأذان التلقائي", Toast.LENGTH_SHORT).show();
                            }
                            if (onUpdated != null) onUpdated.run();
                            showSalatSettingsDialog(activity, prayerManager, onUpdated);
                            break;
                        }
                        case 1: // Individual prayers toggle dialog
                            showIndividualPrayersDialog(activity, prayerManager, () -> {
                                if (onUpdated != null) onUpdated.run();
                                showSalatSettingsDialog(activity, prayerManager, onUpdated);
                            });
                            break;
                        case 2: { // Toggle Fullscreen display
                            boolean newShow = !showFullscreen;
                            sp.edit().putBoolean(SalatReceiver.PREF_SHOW_FULLSCREEN, newShow).apply();
                            Toast.makeText(activity, "عرض شاشة الصلاة: " + (newShow ? "مفعّل (شاشة كاملة)" : "معطّل (صوت في الخلفية فقط)"), Toast.LENGTH_SHORT).show();
                            if (onUpdated != null) onUpdated.run();
                            showSalatSettingsDialog(activity, prayerManager, onUpdated);
                            break;
                        }
                        case 3: // Adhan File details & preview dialog
                            showAdhanFileDetailsDialog(activity, () -> {
                                if (onUpdated != null) onUpdated.run();
                                showSalatSettingsDialog(activity, prayerManager, onUpdated);
                            });
                            break;
                        case 4: // Immediate test of ADHANALL.mp3
                            Toast.makeText(activity, "جاري بدء مقاطعة الوسائط ورفع أذان ADHANALL.mp3...", Toast.LENGTH_LONG).show();
                            SalatSpeechManager.getInstance(activity).testAdhanImmediately(new SalatSpeechManager.PlaybackListener() {
                                @Override public void onSpeechStarted() {}
                                @Override public void onSpeechCompleted() {}
                                @Override public void onAdhanStarted() {
                                    Toast.makeText(activity, "▶️ جاري تشغيل أذان ADHANALL.mp3 (03:07 دقيقة)", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onPlaybackFinished() {
                                    Toast.makeText(activity, "اكتمل تشغيل الأذان وتمت استعادة النظام", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onError(String message) {
                                    Toast.makeText(activity, "خطأ: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case 5: // Developer Test Dialog (5 individual prayer screens)
                            showDeveloperTestDialog(activity);
                            break;
                        case 6: // Volume Dialog
                            showVolumeDialog(activity, () -> {
                                if (onUpdated != null) onUpdated.run();
                                showSalatSettingsDialog(activity, prayerManager, onUpdated);
                            });
                            break;
                        case 7: // Interruption Status & Capabilities Report
                            showInterruptionReportDialog(activity);
                            break;
                        case 8: // Asset integrity
                            showAssetIntegrityDialog(activity);
                            break;
                        case 9: // City selector
                            showCitySelectorDialog(activity, prayerManager, () -> {
                                if (onUpdated != null) onUpdated.run();
                                showSalatSettingsDialog(activity, prayerManager, onUpdated);
                            });
                            break;
                        case 10: // Timeout Dialog
                            showTimeoutDialog(activity, () -> {
                                if (onUpdated != null) onUpdated.run();
                                showSalatSettingsDialog(activity, prayerManager, onUpdated);
                            });
                            break;
                        case 11: // Restore defaults
                            restoreDefaultSettings(activity, prayerManager, onUpdated);
                            break;
                    }
                })
                .setPositiveButton("إغلاق", null)
                .show();
    }

    /**
     * Individual Prayer Alarms Toggle Dialog.
     */
    public static void showIndividualPrayersDialog(final Activity activity, final PrayerManager prayerManager, final Runnable onUpdated) {
        final int[] prayerIndices = {0, 2, 3, 4, 5};
        final String[] prayerTitles = {
                "🌅 صلاة الفجر (Fajr)",
                "☀️ صلاة الظهر (Dhuhr)",
                "🌤️ صلاة العصر (Asr)",
                "🌇 صلاة المغرب (Maghrib)",
                "🌙 صلاة العشاء (Isha)"
        };
        final boolean[] checked = new boolean[prayerIndices.length];
        for (int i = 0; i < prayerIndices.length; i++) {
            checked[i] = SalatAlarmScheduler.isPrayerEnabled(activity, prayerIndices[i]);
        }

        new AlertDialog.Builder(activity)
                .setTitle("🔔 تفعيل / تعطيل أذان الصلوات الفردية")
                .setMultiChoiceItems(prayerTitles, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                    SalatAlarmScheduler.setPrayerEnabled(activity, prayerIndices[which], isChecked);
                })
                .setPositiveButton("حفظ وتحديث المواعيد", (dialog, which) -> {
                    SalatAlarmScheduler.scheduleNextPrayerAlarm(activity, prayerManager);
                    Toast.makeText(activity, "تم تحديث أذان الصلوات المفعّلة ✓", Toast.LENGTH_SHORT).show();
                    if (onUpdated != null) onUpdated.run();
                })
                .setNeutralButton("تفعيل الكل (All ON)", (dialog, which) -> {
                    for (int pIndex : prayerIndices) {
                        SalatAlarmScheduler.setPrayerEnabled(activity, pIndex, true);
                    }
                    SalatAlarmScheduler.scheduleNextPrayerAlarm(activity, prayerManager);
                    Toast.makeText(activity, "تم تفعيل جميع الصلوات الخمس ✓", Toast.LENGTH_SHORT).show();
                    if (onUpdated != null) onUpdated.run();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    /**
     * Adhan Audio File Details & Preview Dialog.
     */
    public static void showAdhanFileDetailsDialog(final Activity activity, final Runnable onUpdated) {
        SalatSpeechManager sm = SalatSpeechManager.getInstance(activity);
        File adhanFile = sm.getCachedAdhanAudioFile();
        boolean exists = (adhanFile != null && adhanFile.exists() && adhanFile.length() > 0);
        long bytes = exists ? adhanFile.length() : 0;
        String sizeMb = String.format("%.2f", bytes / (1024.0 * 1024.0));

        String[] items = {
                "▶️ تشغيل ومعاينة أذان ADHANALL.mp3 فوراً",
                "🛑 إيقاف تشغيل الصوت",
                "📄 الملف المعتمد: " + SalatSpeechManager.ADHAN_FILENAME,
                "⏱️ المدة الكاملة: 03:07 دقيقة (187.37 ثانية)",
                "💾 الحجم: " + sizeMb + " MB (7,494,893 بايت)",
                "🎚️ الجودة: 320 kbps CBR, 48 kHz Stereo",
                "📂 المسار المدمج: assets/adhan/ADHANALL.mp3",
                "🌐 التشغيل: 100% محلي بدون اتصال بالإنترنت (Offline)",
                "🕌 التخصيص: معتمد لجميع الصلوات الخمس (Fajr, Dhuhr, Asr, Maghrib, Isha)"
        };

        new AlertDialog.Builder(activity)
                .setTitle("🎵 ملف الأذان المعتمد — ADHANALL.mp3")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        Toast.makeText(activity, "جاري معاينة أذان ADHANALL.mp3...", Toast.LENGTH_SHORT).show();
                        sm.testAdhanImmediately(new SalatSpeechManager.PlaybackListener() {
                            @Override public void onSpeechStarted() {}
                            @Override public void onSpeechCompleted() {}
                            @Override public void onAdhanStarted() {}
                            @Override public void onPlaybackFinished() {
                                Toast.makeText(activity, "اكتملت معاينة الأذان بنجاح", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(String message) {
                                Toast.makeText(activity, "خطأ: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (which == 1) {
                        sm.stopAll();
                        Toast.makeText(activity, "تم إيقاف الصوت", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("إغلاق", null)
                .show();
    }

    /**
     * Interruption Status & Capabilities Report Dialog.
     */
    public static void showInterruptionReportDialog(final Activity activity) {
        SalatSpeechManager sm = SalatSpeechManager.getInstance(activity);
        String lastStatus = sm.getLastInterruptionStatus();

        String msg = "📊 تقرير نظام مقاطعة الوسائط (System-Wide Interruption):\n\n" +
                "1. حالة آخر مقاطعة:\n" +
                "   " + lastStatus + "\n\n" +
                "2. آلية المقاطعة الثلاثية المطبقة في AURÈS TV:\n" +
                "   • إرسال KEYCODE_MEDIA_PAUSE لإيقاف مشغلات الوسائط (YouTube, SmartTube, IPTV, ExoPlayer).\n" +
                "   • طلب تركيز صوتي حصري (AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE) مع USAGE_ALARM لإيقاف أو خفض صوت التطبيقات الأخرى.\n" +
                "   • كتم مؤقت لتيار الوسائط (STREAM_MUSIC) لضمان عدم تداخل أي صوت خارجي مع الأذان إطلاقاً.\n" +
                "   • إظهار شاشة الصلاة الكاملة عبر FullScreenIntent المستقلة.\n\n" +
                "3. بعد انتهاء الأذان أو الضغط على زر الإيقاف:\n" +
                "   • يتم استعادة تيار الوسائط (STREAM_MUSIC) فوراً.\n" +
                "   • تحرير التركيز الصوتي لتمكين المستخدم من متابعة المشاهدة بأمان.";

        new AlertDialog.Builder(activity)
                .setTitle("📊 تقرير مقاطعة الوسائط وتوافق التطبيقات")
                .setMessage(msg)
                .setPositiveButton("حسناً", null)
                .show();
    }

    /**
     * Volume Dialog.
     */
    public static void showVolumeDialog(final Activity activity, final Runnable onUpdated) {
        final float[] volumes = {1.0f, 0.75f, 0.50f, 0.25f};
        String[] labels = {
                "🔊 100% — أقصى صوت (الافتراضي للتلفاز)",
                "🔉 75% — صوت متوسط مرتفع",
                "🔈 50% — صوت معتدل",
                "🔈 25% — صوت هادئ"
        };

        new AlertDialog.Builder(activity)
                .setTitle("🔉 مستوى صوت الإعلان والأذان")
                .setItems(labels, (dialog, which) -> {
                    SalatSpeechManager.getInstance(activity).setVolume(volumes[which]);
                    Toast.makeText(activity, "تم ضبط الصوت على: " + labels[which], Toast.LENGTH_SHORT).show();
                    if (onUpdated != null) onUpdated.run();
                })
                .show();
    }

    /**
     * Audio Playback Mode Dialog.
     */
    public static void showAudioModeDialog(final Activity activity, final Runnable onUpdated) {
        final SalatSpeechManager.AudioMode[] modes = {
                SalatSpeechManager.AudioMode.FULL_ADHAN_ONLY,
                SalatSpeechManager.AudioMode.VOICE_ONLY,
                SalatSpeechManager.AudioMode.VOICE_THEN_ADHAN,
                SalatSpeechManager.AudioMode.VISUAL_ONLY,
                SalatSpeechManager.AudioMode.MUTE_ALL
        };

        String[] labels = {
                "🕌 رفع أذان ADHANALL.mp3 الكامل فقط (الافتراضي)",
                "🔊 نداء الصلاة بالصوت العربي فقط (Voice Only)",
                "🕌 نداء الصلاة بالصوت العربي يليه الأذان الكامل",
                "👁️ إعلان بصري كامل بدون أي صوت (Visual Only)",
                "🔇 كتم الصوت بالكامل (Mute All)"
        };

        new AlertDialog.Builder(activity)
                .setTitle("🎛️ اختر نمط الصوت للصلوات")
                .setItems(labels, (dialog, which) -> {
                    activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putString(SalatSpeechManager.PREF_AUDIO_MODE, modes[which].name()).apply();
                    Toast.makeText(activity, "تم ضبط نمط الصوت: " + labels[which], Toast.LENGTH_SHORT).show();
                    if (onUpdated != null) onUpdated.run();
                })
                .show();
    }

    /**
     * Developer Test Mode Dialog.
     */
    public static void showDeveloperTestDialog(final Activity activity) {
        String[] prayers = {
                "🌅 اختبار صلاة الفجر (مع: وتسمى أيضاً صلاة الصبح) [شاشة + صوت]",
                "☀️ اختبار صلاة الظهر [شاشة + صوت]",
                "🌤️ اختبار صلاة العصر [شاشة + صوت]",
                "🌇 اختبار صلاة المغرب [شاشة + صوت]",
                "🌙 اختبار صلاة العشاء [شاشة + صوت]",
                "🔊 اختبار الإعلان الصوتي التجريبي المدمج (Test Announcement)",
                "🛑 إيقاف أي صوت مشغل حالياً (Stop Audio)"
        };

        new AlertDialog.Builder(activity)
                .setTitle("🧪 اختبار وضع الصلاة والأصوات (Test Mode)")
                .setItems(prayers, (dialog, which) -> {
                    if (which == 5) {
                        // Play bundled test announcement
                        SalatSpeechManager sm = SalatSpeechManager.getInstance(activity);
                        sm.playTestAnnouncement(new SalatSpeechManager.PlaybackListener() {
                            @Override public void onSpeechStarted() {
                                Toast.makeText(activity, "جاري نداء الاختبار التجريبي...", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onSpeechCompleted() {}
                            @Override public void onAdhanStarted() {}
                            @Override public void onPlaybackFinished() {
                                Toast.makeText(activity, "اكتمل الاختبار بنجاح", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(String message) {
                                Toast.makeText(activity, "خطأ: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    if (which == 6) {
                        SalatSpeechManager.getInstance(activity).stopAll();
                        Toast.makeText(activity, "تم إيقاف الصوت", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int pIndex = 0;
                    if (which == 0) pIndex = 0; // Fajr
                    else if (which == 1) pIndex = 2; // Dhuhr
                    else if (which == 2) pIndex = 3; // Asr
                    else if (which == 3) pIndex = 4; // Maghrib
                    else if (which == 4) pIndex = 5; // Isha

                    Intent salatIntent = new Intent(activity, SalatActivity.class);
                    salatIntent.putExtra(SalatAlarmScheduler.EXTRA_PRAYER_INDEX, pIndex);
                    salatIntent.putExtra(SalatAlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis());
                    activity.startActivity(salatIntent);
                })
                .show();
    }

    /**
     * Import and Preview Adhan Recording Dialog.
     */
    public static void showAdhanImportDialog(final Activity activity, final Runnable onUpdated) {
        final SharedPreferences sp = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String currentPath = sp.getString(SalatSpeechManager.PREF_ADHAN_PATH, null);

        List<String> items = new ArrayList<>();
        items.add("🎵 معاينة تسجيل الأذان الحالي: " + (currentPath != null ? new File(currentPath).getName() : "غير محدد"));
        items.add("🛑 إيقاف المعاينة الصوتية");
        items.add("🔍 البحث التلقائي عن ملفات الأذان في التلفاز (Scan Storage)");
        items.add("✏️ إدخال مسار ملف صوتي يدوياً (Manual Path)");
        if (currentPath != null) {
            items.add("🗑️ إزالة تسجيل الأذان المحدد");
        }

        new AlertDialog.Builder(activity)
                .setTitle("📁 تسجيل الأذان الكامل (Full Adhan Audio)")
                .setItems(items.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        // Preview
                        if (currentPath == null || !(new File(currentPath).exists())) {
                            Toast.makeText(activity, "يرجى تحديد أو استيراد ملف أذان أولاً", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(activity, "جاري معاينة الأذان...", Toast.LENGTH_SHORT).show();
                        SalatSpeechManager.getInstance(activity).previewAdhan(currentPath, new SalatSpeechManager.PlaybackListener() {
                            @Override public void onSpeechStarted() {}
                            @Override public void onSpeechCompleted() {}
                            @Override public void onAdhanStarted() {}
                            @Override public void onPlaybackFinished() {
                                Toast.makeText(activity, "اكتملت معاينة الأذان", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(String message) {
                                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else if (which == 1) {
                        SalatSpeechManager.getInstance(activity).stopAll();
                        Toast.makeText(activity, "تم إيقاف الصوت", Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        scanAndPickAudioFiles(activity, onUpdated);
                    } else if (which == 3) {
                        promptManualAdhanPath(activity, onUpdated);
                    } else if (which == 4) {
                        sp.edit().remove(SalatSpeechManager.PREF_ADHAN_PATH).apply();
                        Toast.makeText(activity, "تمت إزالة مسار الأذان", Toast.LENGTH_SHORT).show();
                        if (onUpdated != null) onUpdated.run();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private static void scanAndPickAudioFiles(final Activity activity, final Runnable onUpdated) {
        final List<File> audioFiles = new ArrayList<>();
        File[] dirs = {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES),
                new File("/sdcard/Adhan"),
                new File("/sdcard/Download"),
                new File("/sdcard/Music")
        };

        for (File dir : dirs) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                File[] list = dir.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a");
                });
                if (list != null) {
                    for (File f : list) {
                        if (!audioFiles.contains(f)) audioFiles.add(f);
                    }
                }
            }
        }

        if (audioFiles.isEmpty()) {
            Toast.makeText(activity, "لم يتم العثور على ملفات صوتية في التخزين — يمكنك إدخال المسار يدوياً", Toast.LENGTH_LONG).show();
            promptManualAdhanPath(activity, onUpdated);
            return;
        }

        String[] names = new String[audioFiles.size()];
        for (int i = 0; i < audioFiles.size(); i++) {
            File f = audioFiles.get(i);
            names[i] = f.getName() + " (" + (f.length() / 1024) + " KB)";
        }

        new AlertDialog.Builder(activity)
                .setTitle("اختر ملف الأذان من التخزين")
                .setItems(names, (dialog, which) -> {
                    File selected = audioFiles.get(which);
                    activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putString(SalatSpeechManager.PREF_ADHAN_PATH, selected.getAbsolutePath()).apply();
                    Toast.makeText(activity, "تم تعيين ملف الأذان: " + selected.getName(), Toast.LENGTH_SHORT).show();
                    if (onUpdated != null) onUpdated.run();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private static void promptManualAdhanPath(final Activity activity, final Runnable onUpdated) {
        final EditText input = new EditText(activity);
        input.setHint("/sdcard/Music/adhan.mp3");

        new AlertDialog.Builder(activity)
                .setTitle("أدخل المسار الكامل لملف الأذان")
                .setView(input)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String path = input.getText().toString().trim();
                    if (!path.isEmpty()) {
                        File f = new File(path);
                        if (!f.exists()) {
                            Toast.makeText(activity, "تنبيه: الملف غير موجود حالياً في هذا المسار", Toast.LENGTH_LONG).show();
                        }
                        activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(SalatSpeechManager.PREF_ADHAN_PATH, path).apply();
                        Toast.makeText(activity, "تم حفظ مسار الأذان", Toast.LENGTH_SHORT).show();
                        if (onUpdated != null) onUpdated.run();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    /**
     * Bundled Audio Assets Integrity Dialog.
     */
    public static void showAssetIntegrityDialog(final Activity activity) {
        SalatSpeechManager.AssetCheckResult result = SalatSpeechManager.getInstance(activity).checkAssetIntegrity();

        StringBuilder sb = new StringBuilder();
        sb.append(result.summary).append("\n\n");
        sb.append("تفاصيل الملفات المدمجة داخل التطبيق:\n");
        for (String line : result.details) {
            sb.append(line).append("\n");
        }
        sb.append("\n• الصيغة: MP3 High Quality Audio\n");
        sb.append("• نوع التشغيل: 100% بدون إنترنت (Offline)");

        new AlertDialog.Builder(activity)
                .setTitle("📦 الملفات الصوتية العربية المدمجة")
                .setMessage(sb.toString())
                .setPositiveButton("حسناً", null)
                .show();
    }

    /**
     * System TTS Status Dialog.
     */
    public static void showTtsStatusDialog(final Activity activity) {
        SalatSpeechManager sm = SalatSpeechManager.getInstance(activity);
        boolean ok = sm.isSystemArabicSupported();
        String voice = sm.getInstalledArabicVoiceName();

        String msg = "حالة محرك الصوت في نظام أندرويد:\n" +
                "• دعم العربية في النظام: " + (ok ? "نعم ✓" : "غير مثبت في نظام التلفاز ⚠️") + "\n" +
                "• الصوت النشط: " + voice + "\n\n" +
                "✓ يعتمد تطبيق AURÈS TV كلياً على أصوات استوديو عربية نقية فائقة الجودة مدمجة محلياً داخل التطبيق، وتعمل دون الحاجة لأي حزم لغوية أو اتصال بالإنترنت.";

        new AlertDialog.Builder(activity)
                .setTitle("ℹ️ حالة الصوت العربي في الجهاز")
                .setMessage(msg)
                .setPositiveButton("حسناً", null)
                .show();
    }

    /**
     * City Selector Dialog.
     */
    public static void showCitySelectorDialog(final Activity activity, final PrayerManager prayerManager, final Runnable onUpdated) {
        final AlgerianCities.City[] cities = AlgerianCities.CITIES;
        String[] names = new String[cities.length];
        for (int i = 0; i < cities.length; i++) {
            names[i] = cities[i].nameAr;
        }

        new AlertDialog.Builder(activity)
                .setTitle("📍 اختر ولاية / مدينة الصلاة")
                .setItems(names, (dialog, which) -> {
                    AlgerianCities.City selected = cities[which];
                    activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putInt("salat_city_index", which).apply();

                    if (prayerManager != null) {
                        prayerManager.setLocation(selected.latitude, selected.longitude);
                    }
                    SalatAlarmScheduler.scheduleNextPrayerAlarm(activity, prayerManager);
                    Toast.makeText(activity, "تم تغيير موقع الصلاة إلى: " + selected.nameAr, Toast.LENGTH_SHORT).show();
                    if (onUpdated != null) onUpdated.run();
                })
                .show();
    }

    /**
     * Screen Timeout Dialog.
     */
    public static void showTimeoutDialog(final Activity activity, final Runnable onUpdated) {
        final int[] timeouts = {15, 30, 60, 0};
        String[] labels = {
                "15 ثانية (سريع)",
                "30 ثانية (الافتراضي المتوازن)",
                "60 ثانية (شاشة أطول)",
                "يدوي (تبقى حتى يضغط المستخدم زر الرجوع)"
        };

        new AlertDialog.Builder(activity)
                .setTitle("⏱️ مدة بقاء شاشة الصلاة")
                .setItems(labels, (dialog, which) -> {
                    activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putInt(SalatActivity.PREF_DISMISS_SECONDS, timeouts[which]).apply();
                    Toast.makeText(activity, "تم ضبط المدة: " + labels[which], Toast.LENGTH_SHORT).show();
                    if (onUpdated != null) onUpdated.run();
                })
                .show();
    }

    /**
     * Restore Default Settings.
     */
    public static void restoreDefaultSettings(final Activity activity, final PrayerManager prayerManager, final Runnable onUpdated) {
        SharedPreferences sp = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putBoolean(SalatAlarmScheduler.PREF_SALAT_ENABLED, true)
                .putBoolean(SalatReceiver.PREF_SHOW_FULLSCREEN, true)
                .putBoolean(SalatSpeechManager.PREF_SPEECH_ENABLED, false)
                .putBoolean(SalatSpeechManager.PREF_ADHAN_ENABLED, true)
                .putString(SalatSpeechManager.PREF_AUDIO_MODE, SalatSpeechManager.AudioMode.FULL_ADHAN_ONLY.name())
                .putFloat(SalatSpeechManager.PREF_VOLUME, 1.0f)
                .putInt("salat_city_index", 0) // Khenchela
                .putInt(SalatActivity.PREF_DISMISS_SECONDS, 30)
                .apply();

        for (int pIndex : new int[]{0, 2, 3, 4, 5}) {
            SalatAlarmScheduler.setPrayerEnabled(activity, pIndex, true);
        }

        SalatSpeechManager.getInstance(activity).setVolume(1.0f);

        if (prayerManager != null) {
            AlgerianCities.City city = AlgerianCities.getCityByIndex(0);
            prayerManager.setLocation(city.latitude, city.longitude);
        }
        SalatAlarmScheduler.scheduleNextPrayerAlarm(activity, prayerManager);

        Toast.makeText(activity, "تم استعادة كافة إعدادات الصوت والصلاة الافتراضية بنجاح ✓", Toast.LENGTH_LONG).show();
        if (onUpdated != null) onUpdated.run();
    }

    private static String getAudioModeLabel(String modeStr) {
        try {
            SalatSpeechManager.AudioMode mode = SalatSpeechManager.AudioMode.valueOf(modeStr);
            switch (mode) {
                case VOICE_ONLY: return "صوت عربي فقط (الافتراضي)";
                case FULL_ADHAN_ONLY: return "أذان كامل فقط";
                case VOICE_THEN_ADHAN: return "صوت عربي + أذان";
                case VISUAL_ONLY: return "بصري فقط";
                case MUTE_ALL: return "مكتوم";
            }
        } catch (Throwable ignored) {}
        return "صوت عربي فقط";
    }

    private SalatUiHelper() {}
}
