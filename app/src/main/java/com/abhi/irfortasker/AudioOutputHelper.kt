package com.abhi.irfortasker

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log


class AudioOutputHelper(context: Context) {
    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalVolumeLevel: Int = 0
    private var maxiMumVolumeLevel: Int = 0
    private var headsetInfo: AudioDeviceInfo? = null

    // perform cleanup only if the app changed any audio settings
    private var hasStateChanged = false

    init {
        maxiMumVolumeLevel = am.getStreamMaxVolume(TRANSMISSION_STREAM)
        originalVolumeLevel = am.getStreamVolume(TRANSMISSION_STREAM)
        headsetInfo = getHeadsetInfo().also {
            Log.i(TAG, "init: selected output device ${it?.type}- ${it?.productName} ")
        }
    }

    private fun getHeadsetInfo(): AudioDeviceInfo? {
        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter {
            it.isSink && it.type in listOf(
                AudioDeviceInfo.TYPE_LINE_ANALOG, // found this one is for a DIY IR blaster
                AudioDeviceInfo.TYPE_AUX_LINE,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            )
        }.let { devices ->
            devices.forEach {
                Log.i(TAG, "getHeadsetInfo: device ${it.type} - ${it.productName} ")
            }
            return devices.firstOrNull()
        }
    }

    fun isOnWiredHeadphone() = headsetInfo != null

    fun prepareAudioOutput(): Boolean {
        hasStateChanged = true
        return try {
            //selecting output mode
            headsetInfo?.let { headphone ->
//                am.setMode(AudioManager.MODE_IN_COMMUNICATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.setCommunicationDevice(headphone)
                } else {
                    am.stopBluetoothSco()
                    am.setSpeakerphoneOn(false)
                    am.setWiredHeadsetOn(true)
                    true
                }
            } ?: true
        } catch (e: Exception) {
            Log.e(TAG, "prepareAudioOutput: failed to select output mode", e)
            false
        } finally {
            // setting volume max
            am.getStreamMaxVolume(TRANSMISSION_STREAM).let { maxVolume ->
                am.setStreamVolume(TRANSMISSION_STREAM, maxVolume, AudioManager.FLAG_SHOW_UI)
                Log.d(TAG, "prepareAudioOutput: max volume set to $maxVolume")
            }
        }
    }

    fun cleanupAudioOutput() {
        if (!hasStateChanged) return
        try {
            // resetting audio output
//            am.setMode(AudioManager.MODE_NORMAL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.clearCommunicationDevice()
            } else {
                am.startBluetoothSco()
                am.setSpeakerphoneOn(true)
                am.setWiredHeadsetOn(true)
            }
            Log.i(TAG, "cleanupAudioOutput: completed resetting output")
        } catch (e: Exception) {
            Log.e(TAG, "cleanupAudioOutput: failed to reset audio output", e)
        } finally {
            // restoring volume level
            am.setStreamVolume(TRANSMISSION_STREAM, originalVolumeLevel, 0)
        }
    }

    companion object {
        private const val TAG = "AudioOutputHelper"
        private const val TRANSMISSION_STREAM = AudioManager.STREAM_MUSIC
    }
}
