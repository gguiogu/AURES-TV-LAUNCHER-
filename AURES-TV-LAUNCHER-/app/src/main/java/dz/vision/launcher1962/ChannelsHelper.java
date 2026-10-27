package dz.vision.launcher1962;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Algerian TV Channels Shelf Shortcuts.
 * Connects ENTV, Canal Algérie, Echorouk TV, Ennahar TV to Yacine TV.
 * Package: ver3.ycntivi.off
 * Leanback TV UI Activity: dev.google.yacinetv.tvui.splash.SplashActivity
 */
public final class ChannelsHelper {

    public static final String CH_ENTV = "dz_entv";
    public static final String CH_CANAL_ALGERIE = "dz_canal_algerie";
    public static final String CH_ECHOROUK = "dz_echorouk";
    public static final String CH_ENNAHAR = "dz_ennahar";

    // Verified Yacine TV Package & Launch Components on Tanix TX1
    public static final String YACINE_TV_PACKAGE = "ver3.ycntivi.off";
    public static final String YACINE_TV_TVUI_ACTIVITY = "dev.google.yacinetv.tvui.splash.SplashActivity";
    public static final String YACINE_TV_MOBILE_ACTIVITY = "dev.google.yacinetv.ui.splash.SplashActivity";

    /**
     * Checks whether Yacine TV is installed on the device.
     */
    public static boolean isYacineTvInstalled(PackageManager pm) {
        try {
            pm.getPackageInfo(YACINE_TV_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks whether Yacine TV supports direct channel navigation via Intent.
     * Note: Yacine TV does not declare deep link intent-filters or public channel endpoints.
     */
    public static boolean supportsDirectChannelNavigation(PackageManager pm, String channelId) {
        try {
            Intent directIntent = new Intent("ver3.ycntivi.off.PLAY_CHANNEL");
            directIntent.setPackage(YACINE_TV_PACKAGE);
            directIntent.putExtra("channel_id", channelId);
            List<ResolveInfo> list = pm.queryIntentActivities(directIntent, 0);
            return list != null && !list.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Launches the selected Algerian channel or Yacine TV.
     * 1. Check if Yacine TV is installed.
     * 2. If installed and supports opening the channel directly, launch that channel.
     * 3. If direct channel navigation is not supported, open Yacine TV (TV Leanback UI).
     * 4. If Yacine TV is not installed, show a clear message explaining that it must be installed.
     */
    public static void launchChannel(Context context, String channelId, String channelTitle) {
        PackageManager pm = context.getPackageManager();

        // 1. Check if Yacine TV is installed
        if (!isYacineTvInstalled(pm)) {
            String msg = getNotInstalledMessage(context);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Check if Yacine TV supports direct channel navigation
        if (supportsDirectChannelNavigation(pm, channelId)) {
            try {
                Intent direct = new Intent("ver3.ycntivi.off.PLAY_CHANNEL");
                direct.setPackage(YACINE_TV_PACKAGE);
                direct.putExtra("channel_id", channelId);
                direct.putExtra("channel_title", channelTitle);
                direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(direct);
                return;
            } catch (Exception ignored) {}
        }

        // 3. Fallback: Launch Yacine TV's TV UI directly so user can select the channel
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.setComponent(new ComponentName(YACINE_TV_PACKAGE, YACINE_TV_TVUI_ACTIVITY));
        launchIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        // Verify component resolves, otherwise fallback to standard launch intent
        List<ResolveInfo> resolved = pm.queryIntentActivities(launchIntent, 0);
        if (resolved == null || resolved.isEmpty()) {
            Intent fallback = pm.getLeanbackLaunchIntentForPackage(YACINE_TV_PACKAGE);
            if (fallback == null) {
                fallback = pm.getLaunchIntentForPackage(YACINE_TV_PACKAGE);
            }
            if (fallback != null) {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                launchIntent = fallback;
            }
        }

        try {
            String toastMsg = getOpeningChannelMessage(context, channelTitle);
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();
            context.startActivity(launchIntent);
        } catch (Exception e) {
            Toast.makeText(context, "تعذر تشغيل Yacine TV", Toast.LENGTH_SHORT).show();
        }
    }

    private static String getNotInstalledMessage(Context context) {
        SharedPreferences sp = context.getSharedPreferences("launcher_1962_prefs", Context.MODE_PRIVATE);
        String langStr = sp.getString("current_language", "ARABIC");
        if ("FRENCH".equalsIgnoreCase(langStr)) {
            return "L'application Yacine TV n'est pas installée. Veuillez l'installer pour regarder les chaînes.";
        } else if ("ENGLISH".equalsIgnoreCase(langStr)) {
            return "Yacine TV is not installed. Please install it to watch Algerian channels.";
        } else if ("TAMAZIGHT".equalsIgnoreCase(langStr)) {
            return "Yacine TV ur yettwasbedd ara. Ttxil-k sbedd-it akken ad twaliḍ tiliẓriwin.";
        }
        return "تطبيق Yacine TV غير مثبت على الجهاز. يرجى تثبيته لمشاهدة القنوات الجزائرية.";
    }

    private static String getOpeningChannelMessage(Context context, String channelTitle) {
        SharedPreferences sp = context.getSharedPreferences("launcher_1962_prefs", Context.MODE_PRIVATE);
        String langStr = sp.getString("current_language", "ARABIC");
        if ("FRENCH".equalsIgnoreCase(langStr)) {
            return "Ouverture de " + channelTitle + " via Yacine TV...";
        } else if ("ENGLISH".equalsIgnoreCase(langStr)) {
            return "Opening " + channelTitle + " via Yacine TV...";
        } else if ("TAMAZIGHT".equalsIgnoreCase(langStr)) {
            return "Tuldin n " + channelTitle + " s Yacine TV...";
        }
        return "فتح " + channelTitle + " عبر Yacine TV...";
    }
}
