package dz.vision.launcher1962;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * Boot Completed Receiver for 1962 Android TV Launcher.
 * Ensures 1962 initializes cleanly and is displayed immediately after TV box power-on / reboot.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            // Initialize safety watchdog
            SafetyWatchdog.init(context);

            // Delay slightly (500ms) to allow WindowManagerService to settle
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent home = new Intent(context, MainActivity.class);
                    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    context.startActivity(home);
                } catch (Throwable ignored) {}
            }, 500);
        }
    }
}
