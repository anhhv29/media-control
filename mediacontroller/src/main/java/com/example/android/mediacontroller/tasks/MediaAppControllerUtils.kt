package com.example.android.mediacontroller.tasks

import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.session.MediaController
import android.util.Log
import com.example.android.mediacontroller.MediaAppDetails

object MediaAppControllerUtils {
    @JvmStatic
    fun getMediaAppsFromControllers(
        controllers: List<MediaController?>,
        packageManager: PackageManager,
        resources: Resources,
    ): List<MediaAppDetails> {
        val mediaApps = ArrayList<MediaAppDetails>()
        for (controller in controllers) {
            val packageName = controller?.packageName
            val info: ApplicationInfo
            try {
                info = packageManager.getApplicationInfo(packageName ?: "", 0)
            } catch (e: PackageManager.NameNotFoundException) {
                // This should not happen. If we get a media session for a package, then the
                // package must be installed on the device.
                Log.e(ContentValues.TAG, "Unable to load package details", e)
                continue
            }

            mediaApps.add(
                MediaAppDetails(info, packageManager, resources, controller?.sessionToken)
            )
        }
        return mediaApps
    }
}