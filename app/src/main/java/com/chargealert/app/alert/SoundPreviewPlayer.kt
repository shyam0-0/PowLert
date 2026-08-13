package com.chargealert.app.alert

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

/**
 * Plays a short preview of a candidate alert sound, entirely separate from
 * BatteryAlertManager -- previewing must never touch charging-session state
 * or count as a real alert. Holds at most one Ringtone at a time so picking
 * a new preview (or leaving the picker) always stops the previous one.
 */
object SoundPreviewPlayer {

    private const val TAG = "SoundPreviewPlayer"
    private var current: Ringtone? = null

    /** Returns false if the sound could not be played (invalid/deleted/revoked URI). */
    fun preview(context: Context, soundId: String): Boolean {
        stop()
        val uri = if (soundId == com.chargealert.app.domain.AlertSettings.DEFAULT_SOUND) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            Uri.parse(soundId)
        }

        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return false
            ringtone.play()
            current = ringtone
            true
        } catch (e: Exception) {
            Log.w(TAG, "Preview failed for $uri", e)
            false
        }
    }

    fun stop() {
        current?.let { if (it.isPlaying) it.stop() }
        current = null
    }
}
