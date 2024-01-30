package com.example.android.mediacontroller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.android.mediacontroller.databinding.MediaControlsBinding
import com.example.android.mediacontroller.tasks.FindMediaAppsTask.AppListUpdatedCallback
import com.example.android.mediacontroller.tasks.FindMediaSessionAppsTask
import com.example.android.mediacontroller.tasks.MediaAppControllerUtils.getMediaAppsFromControllers

@Suppress("DEPRECATION")
class MediaControlActivity : AppCompatActivity() {
    private lateinit var binding: MediaControlsBinding
    private var mMediaSessionListener: MediaSessionListener? = null
    private var mController: MediaControllerCompat? = null
    private var mBrowser: MediaBrowserCompat? = null
    private var listAppsRunning = listOf<MediaAppDetails>()
    private val playings = intArrayOf(
        PlaybackStateCompat.STATE_PLAYING,
        PlaybackStateCompat.STATE_BUFFERING,
        PlaybackStateCompat.STATE_FAST_FORWARDING,
        PlaybackStateCompat.STATE_REWINDING,
        PlaybackStateCompat.STATE_SKIPPING_TO_NEXT,
        PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM,
        PlaybackStateCompat.STATE_CONNECTING
    )
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MediaControlsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mMediaSessionListener = MediaSessionListener(this) {
            listAppsRunning = it
            getAppRunning()
        }

        mMediaSessionListener?.onCreate()

        binding.apply {
            actionPlay.setOnClickListener {
                if (isPlaying()) {
                    mController?.transportControls?.pause()
                } else mController?.transportControls?.play()
            }
            actionSkipNext.setOnClickListener {
                mController?.transportControls?.skipToNext()
            }
            actionSkipPrevious.setOnClickListener {
                mController?.transportControls?.skipToPrevious()
            }
            mediaArt.setOnClickListener {
                mController?.packageName?.let { it1 -> openAppOrStore(it1, false) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mMediaSessionListener?.onStart(this)
    }

    override fun onStop() {
        mMediaSessionListener?.onStop()
        super.onStop()
    }

    private class MediaSessionListener(
        private val activity: Activity,
        private val returnMediaAppDetails: (List<MediaAppDetails>) -> Unit,
    ) {
        private var mMediaSessionManager: MediaSessionManager? = null
        private val mSessionAppsUpdated: AppListUpdatedCallback = object : AppListUpdatedCallback {
            override fun onAppListUpdated(
                mediaAppEntries: List<MediaAppDetails>,
            ) {
                returnMediaAppDetails.invoke(mediaAppEntries)
            }
        }

        private val mSessionsChangedListener =
            OnActiveSessionsChangedListener { list: List<MediaController?>? ->
                list?.let {
                    mSessionAppsUpdated.onAppListUpdated(
                        getMediaAppsFromControllers(
                            list, activity.packageManager, activity.resources
                        )
                    )
                }
            }

        fun onCreate() {
            mMediaSessionManager =
                activity.getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        }

        fun onStart(context: Context) {
            if (!NotificationListener.isEnabled(context)) {
                showRequestPermission(context)
            }

            mMediaSessionManager?.let {
                try {
                    val listenerComponent =
                        ComponentName(context, NotificationListener::class.java)
                    it.addOnActiveSessionsChangedListener(
                        mSessionsChangedListener, listenerComponent
                    )
                    FindMediaSessionAppsTask(
                        it, listenerComponent,
                        context.packageManager, context.resources, mSessionAppsUpdated
                    ).execute()
                } catch (ex: Exception) {
                    showRequestPermission(context)
                }
            } ?: run { return }
        }

        fun onStop() {
            mMediaSessionManager?.removeOnActiveSessionsChangedListener(mSessionsChangedListener)
        }

        private fun showRequestPermission(context: Context) {
            context.startActivity(
                Intent(
                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                )
            )
            return
        }
    }

    private fun getAppRunning() {
        for ((i, apps) in listAppsRunning.withIndex()) {
            setupMedia(apps)
            if (i == listAppsRunning.size - 1) {
                if (this.mController == null) {
                    handler.postDelayed(runnableCheckUpdateState, 1000)
                }
            }
        }
    }

    private fun setupMedia(mMediaAppDetails: MediaAppDetails) {
        // Should now have a viable details.. connect to browser and service as needed.
        if (mMediaAppDetails.componentName != null) {
            mBrowser = MediaBrowserCompat(
                this, mMediaAppDetails.componentName,
                object : MediaBrowserCompat.ConnectionCallback() {
                    override fun onConnected() {
                        setupMediaController(mMediaAppDetails)
                    }

                    override fun onConnectionSuspended() {
                    }

                    override fun onConnectionFailed() {
                    }
                }, null
            )
            mBrowser?.connect()
        } else if (mMediaAppDetails.sessionToken != null) {
            setupMediaController(mMediaAppDetails)
        }
    }

    private fun setupMediaController(mMediaAppDetails: MediaAppDetails) {
        try {
            var token = mMediaAppDetails.sessionToken
            if (token == null) {
                token = mBrowser?.sessionToken
            }
            val mController = MediaControllerCompat(this, token)
            for (i in playings.indices) {
                if (mController.playbackState?.state == playings[i]) {
                    this.mController = mController
                    this.mController?.registerCallback(mCallback)
                    mCallback.onPlaybackStateChanged(this.mController?.playbackState)
                    mCallback.onMetadataChanged(this.mController?.metadata)
                    fetchMediaInfo()
                    break
                }
            }
            return
        } catch (remoteException: RemoteException) {
            remoteException.printStackTrace()
        }
    }

    private val mCallback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
                onUpdate()

            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                onUpdate()
            }

            override fun onSessionDestroyed() {
            }

            private fun onUpdate() {
                fetchMediaInfo()
            }
        }

    private fun fetchMediaInfo() {
        mController?.let { mController ->
            handler.removeCallbacks(runnableCheckUpdateState)
            buttonsPlay()
            binding.apply {
                mediaTitle.text =
                    mController.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: ""
                mediaArtist.text =
                    mController.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: ""

                var art: Bitmap? =
                    mController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON)
                if (art == null) {
                    art = mController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
                }
                if (art == null) {
                    art =
                        mController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
                }
                if (art == null) {
                    art = mController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
                }
                if (art == null) {
                    art = mController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
                }

                if (art != null) {
                    mediaArt.setImageBitmap(art)
                } else {
                    mediaArt.setImageDrawable(packageManager.getApplicationIcon(mController.packageName))
                }
            }
        }
    }

    private val runnableCheckUpdateState = Runnable {
        Log.e("123123123", "runnableCheckUpdateState")
        if (mController == null) {
            getAppRunning()
        }
    }

    private fun openAppOrStore(packageName: String, requestInstall: Boolean): Boolean {
        if (TextUtils.isEmpty(packageName)) {
            return false
        }
        val pm = packageManager ?: return false
        try {
            val intent = pm.getLaunchIntentForPackage(packageName)
            intent?.putExtra("from_touch_screen", true)
            startActivity(intent)
        } catch (e: java.lang.Exception) {
            if (requestInstall) {
                openStore(packageName)
            }
            return false
        }
        return true
    }

    private fun openStore(packageName: String) {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (anfe: java.lang.Exception) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            startActivity(intent)
        }
    }

    private fun isPlaying(): Boolean {
        if (mController == null || mController?.playbackState == null) {
            return false
        }
        mController?.let {
            val state = it.playbackState.state
            for (i in playings.indices) {
                if (state == playings[i])
                    return true
            }
        }
        return false
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun buttonsPlay() {
        if (isPlaying()) {
            binding.actionPlay.setImageDrawable(getDrawable(R.drawable.ic_pause))
        } else binding.actionPlay.setImageDrawable(getDrawable(R.drawable.ic_play))

    }
}