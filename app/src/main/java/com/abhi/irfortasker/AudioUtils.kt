package com.abhi.irfortasker

import android.content.Context
import android.media.AudioManager


object AudioUtils {
    fun isOnWiredHeadset(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isWiredHeadsetOn
    }
}