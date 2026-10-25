package dz.vision.launcher1962;

import android.graphics.drawable.Drawable;

public class AppItem {
    public final String packageName;
    public final String activityName;
    public final String label;
    public final Drawable icon;
    public final int dominantColor;
    public final boolean isChannel;
    public final String channelId;
    public final boolean isFunctional;

    public AppItem(String packageName, String activityName, String label, Drawable icon, int dominantColor) {
        this(packageName, activityName, label, icon, dominantColor, false, null, false);
    }

    public AppItem(String packageName, String activityName, String label, Drawable icon, int dominantColor, boolean isFunctional) {
        this(packageName, activityName, label, icon, dominantColor, false, null, isFunctional);
    }

    public AppItem(String packageName, String activityName, String label, Drawable icon, int dominantColor, boolean isChannel, String channelId) {
        this(packageName, activityName, label, icon, dominantColor, isChannel, channelId, false);
    }

    public AppItem(String packageName, String activityName, String label, Drawable icon, int dominantColor, boolean isChannel, String channelId, boolean isFunctional) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.label = label;
        this.icon = icon;
        this.dominantColor = dominantColor;
        this.isChannel = isChannel;
        this.channelId = channelId;
        this.isFunctional = isFunctional;
    }
}
