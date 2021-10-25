package com.exifthumbnailadder.app;

import android.app.ActivityManager;
import android.content.Context;

public class ServiceUtil {

    // https://stackoverflow.com/a/5921190/15401262
    @SuppressWarnings("deprecation")
    // warning: [deprecation] getRunningServices(int) in ActivityManager has been deprecated
    // This method was deprecated in API level 26. As of Build.VERSION_CODES.O, this method is no longer available to third party applications. For backwards compatibility, it will still return the caller's own services.
    public static boolean isServiceRunning(Context ctx, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
