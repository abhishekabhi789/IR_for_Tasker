package com.abhi.irfortasker

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.ceil
import kotlin.math.sin

/**
This class generates audio tones for the IR pulses and [transmit] as IR pulese through 3.5mm audio jack.
 */
class TransmitAsAudioPulse(private var frequency: Int, private var pattern: IntArray) {

    private var duration: Float
    private var period: Float = 1f / frequency
    private val TAG = javaClass.simpleName

    init {
        Log.d(TAG, "pattern:" + pattern.joinToString(","))
        duration = pattern.sum() * period
        Log.d(TAG, "init: duration: $duration")
    }

    fun transmit(context: Context) {
        //source: https://github.com/g-r-a-v-i-t-y-w-a-v-e/ProntoDroid/blob/master/app/src/main/java/paronomasia/prontodroid/Pronto.java
        val handler = Handler(Looper.getMainLooper())
        val r = Runnable {
            //preparing samples
            var count = (44100.0 * 2.0 * duration).toInt()
            Log.d(TAG, "transmit: count: $count")
            val minBufferSize = AudioTrack.getMinBufferSize(
                44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSizeInBytes = count * (Short.SIZE_BYTES)
            if (bufferSizeInBytes < minBufferSize) {
                Log.w(
                    TAG,
                    "lowBufferSize error: bufferSizeInBytes($bufferSizeInBytes) < minBufferSize($minBufferSize)"
                )
                val requiredIncreaseInCount = (minBufferSize - bufferSizeInBytes) / Short.SIZE_BYTES
                count += requiredIncreaseInCount
                val requiredIncreaseInPulse =
                    ceil(requiredIncreaseInCount / (44100 * 2 * period)).toInt()
                pattern[pattern.size - 1] += requiredIncreaseInPulse
                Log.d(TAG,
                    buildString {
                        append("transmit: workarounds applied: ")
                        append("increaseInCount: $requiredIncreaseInCount")
                        append(" | increaseInLastPulse: $requiredIncreaseInPulse")
                        append(" | newBufferSize: " + (count * Short.SIZE_BYTES).toString())
                    }
                )
            }
            val samples = ShortArray(count)
            var offset = 0
            for (i in pattern.indices) {
                val currentVal: Int = pattern[i]
                val generated = if (i % 2 == 0) {
                    generateTones(period * currentVal)
                } else {
                    generateSilence(period * currentVal)
                }
                generated.indices.forEach { j ->
                    samples[offset + j] = generated[j]
                }
                offset += generated.size
            }

            //preparing audio
            val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

            val audioFormat = AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(44100).build()

            val track = AudioTrack.Builder().setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            try {
                // Set up the audio track and adjust volumes
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val origVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(
                    AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
                )
                am.mode = AudioManager.MODE_NORMAL
                am.isWiredHeadsetOn = true

                track.notificationMarkerPosition = count * Short.SIZE_BYTES / 4
                track.setPlaybackPositionUpdateListener(object :
                    AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(audioTrack: AudioTrack) {
                        // when done, reset the volume to its original value
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, origVol, 0)
                    }

                    override fun onPeriodicNotification(audioTrack: AudioTrack) {
                    }
                })

                try {
                    if (track.state != AudioTrack.STATE_UNINITIALIZED) {
                        track.play()
                        track.write(samples, 0, count)
                    }
                    //re enable bt headset
                    am.isBluetoothScoOn = true
                    Log.d(TAG, "transmit: Finished")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play: ${e.message}".trimIndent())
                }
            } catch (e: NullPointerException) {
                Log.e(TAG, "NullPointerException in playing: +${e.message}".trimIndent())
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException in playing: +${e.message}".trimIndent())
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "Other exception: ${e.message}".trimIndent())
            }
            track.flush()
        }
        handler.post(r)
    }

    //source: https://gist.github.com/slightfoot/6330866
    private fun generateTones(duration: Float): ShortArray {
        val count = (44100.0 * 2.0 * duration).toInt() and 1.inv()
        val samples = ShortArray(count)
        var i = 0
        while (i < count) {
            val sample = (sin(Math.PI * i / (44100.0 / frequency)) * 0x7FFF).toInt().toShort()
            samples[i] = sample
            samples[i + 1] = (-1 * sample).toShort()
            i += 2
        }
        return samples
    }

    private fun generateSilence(duration: Float): ShortArray {
        val count = (44100.0 * 2.0 * duration).toInt() and 1.inv()
        val samples = ShortArray(count)
        var i = 0
        while (i < count) {
            samples[i] = 0
            samples[i + 1] = 0
            i += 2
        }
        return samples
    }
}