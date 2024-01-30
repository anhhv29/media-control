package com.example.android.mediacontroller.tasks

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.session.MediaSessionManager
import com.example.android.mediacontroller.MediaAppDetails

class FindMediaSessionAppsTask constructor(
    private val mediaSessionManager: MediaSessionManager,
    private val listenerComponent: ComponentName,
    private val packageManager: PackageManager,
    private val resources: Resources,
    callback: AppListUpdatedCallback
) : FindMediaAppsTask(callback, sortAlphabetical = false) {

    override val mediaApps: List<MediaAppDetails>
        get() = MediaAppControllerUtils.getMediaAppsFromControllers(
            mediaSessionManager.getActiveSessions(listenerComponent),
            packageManager,
            resources
        )
}