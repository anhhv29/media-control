package com.example.android.mediacontroller;

import android.content.Context;
import android.service.notification.NotificationListenerService;

import androidx.core.app.NotificationManagerCompat;

public class NotificationListener extends NotificationListenerService {
    // Helper method to check if our notification listener is enabled. In order to get active media
    // sessions, we need an enabled notification listener component.
    public static boolean isEnabled(Context context) {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.getPackageName());
    }
}
