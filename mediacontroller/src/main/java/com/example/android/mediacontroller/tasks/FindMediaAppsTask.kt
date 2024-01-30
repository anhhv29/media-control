@file:Suppress("DEPRECATION")

package com.example.android.mediacontroller.tasks

import android.os.AsyncTask
import com.example.android.mediacontroller.MediaAppDetails

@Suppress("DEPRECATION")
abstract class FindMediaAppsTask constructor(
    private val callback: AppListUpdatedCallback, private val sortAlphabetical: Boolean
) : AsyncTask<Void, Void, List<MediaAppDetails>>() {

    interface AppListUpdatedCallback {
        fun onAppListUpdated(mediaAppEntries: List<MediaAppDetails>)
    }

    protected abstract val mediaApps: List<MediaAppDetails>

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void): List<MediaAppDetails> {
        val mediaApps = ArrayList(mediaApps)
        if (sortAlphabetical) {
            // Sort the list by localized app name for convenience.
            mediaApps.sortWith(Comparator { left, right ->
                left.appName.compareTo(right.appName, ignoreCase = true)
            })
        }
        return mediaApps
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(mediaAppEntries: List<MediaAppDetails>) {
        callback.onAppListUpdated(mediaAppEntries)
    }
}