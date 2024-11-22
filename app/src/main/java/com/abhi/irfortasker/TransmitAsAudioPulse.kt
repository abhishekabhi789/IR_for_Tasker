package com.abhi.irfortasker

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.abhi.irfortasker.TransmitAsAudioPulse.transmit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin


/**
 * This object generates audio tones for IR pulses and [transmit] them as IR pulses through a 3.5mm audio jack.
 */
object TransmitAsAudioPulse {
    private const val TAG = "TransmitAsAudioPulse"
    private const val SAMPLE_RATE_IN_HERTZ = 44100

    /**
     * Transmit the infrared pattern as audio pulses
     *
     * @param frequency The IR carrier frequency in Hertz.
     * @param pattern The alternating on/off pattern in microseconds to transmit.
     */
    fun transmit(context: Context, frequency: Int, pattern: IntArray) {
        //based on: https://github.com/g-r-a-v-i-t-y-w-a-v-e/ProntoDroid/blob/master/app/src/main/java/paronomasia/prontodroid/Pronto.java
        Log.i(TAG, "frequency: $frequency,  pattern:${pattern.joinToString(",")}")
        CoroutineScope(Dispatchers.IO).launch {
            val patternInSeconds = pattern.map { it.toFloat() / 1_000_000 }.toFloatArray()
            Log.d(TAG, "transmit: patternInSeconds ${patternInSeconds.joinToString(",")}")
            val duration = patternInSeconds.sum()
            if (duration > 2) {
                Log.e(TAG, "transmit: duration is greater than 2s = $duration")
                return@launch //duration higher than 2 seconds in invalid
            }
            var count: Int = getCountForDuration(duration)
            Log.d(TAG, "transmit: count: $count duration: $duration")
            val minBufferSize = getMinBufferSize()
            val bufferSizeInBytes = count * Short.SIZE_BYTES
            if (bufferSizeInBytes < minBufferSize) {
                Log.w(TAG, "transmit: bufferSize less than minimum, trying to fix")
                Log.i(TAG, "transmit: bufferSize : $bufferSizeInBytes < $minBufferSize")
                val increaseInCount = (minBufferSize - bufferSizeInBytes) / Short.SIZE_BYTES
                count += increaseInCount
                val increaseInPulse: Float = increaseInCount / (SAMPLE_RATE_IN_HERTZ * 2F)
                //increasing the duration of the last pulse
                patternInSeconds[patternInSeconds.size - 1] += increaseInPulse
                Log.i(
                    TAG,
                    buildString {
                        append("transmit: workarounds applied: ")
                        append("increaseInCount: $increaseInCount")
                        append(" | increaseInLastPulse: $increaseInPulse")
                        append(" | newBufferSize: " + (count * Short.SIZE_BYTES).toString())
                    }
                )
                Log.d(TAG, "transmit: new pattern ${patternInSeconds.joinToString(",")}")
            }

            val samples = ShortArray(count)
            var offset = 0
            for (i in patternInSeconds.indices) {
                val pulseDuration = patternInSeconds[i]
                val generated = if (i % 2 == 0) {
                    generateTones(pulseDuration, frequency)
                } else {
                    generateSilence(pulseDuration)
                }
                generated.indices.forEach { j -> samples[offset + j] = generated[j] }
                offset += generated.size
            }
            val track = prepareAudioTrack()
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val originalVolumeLevel = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                try {
                    am.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
                    )
                } catch (e: SecurityException) {
                    Log.e(TAG, "transmit: audioManager setStreamVolume failed", e)
                }
                am.mode = AudioManager.MODE_NORMAL
                am.isWiredHeadsetOn = true

                track.notificationMarkerPosition = count * Short.SIZE_BYTES / 4
                track.setPlaybackPositionUpdateListener(object :
                    AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(audioTrack: AudioTrack) {
                        // when done, reset the volume to its original value
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolumeLevel, 0)
                    }

                    override fun onPeriodicNotification(audioTrack: AudioTrack) {
                    }
                })

                try {
                    if (track.state != AudioTrack.STATE_UNINITIALIZED) {
                        track.play()
                        track.write(samples, 0, count)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play: ${e.message}".trimIndent())
                } finally {
                    //re enable bt headset
                    am.isBluetoothScoOn = true
                    Log.d(TAG, "transmit: finished")
                }
            } catch (e: NullPointerException) {
                Log.e(TAG, "NullPointerException in playing: +${e.message}".trimIndent())
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException in playing: +${e.message}".trimIndent())
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "Other exception: ${e.message}".trimIndent())
            } finally {
                track.flush()
                track.release()
            }
        }
    }

    //source: https://gist.github.com/slightfoot/6330866
    private fun generateTones(duration: Float, frequency: Int): ShortArray {
        val count = getCountForDuration(duration)
        val samples = ShortArray(count)
        var i = 0
        while (i < count) {
            val sample =
                (sin(Math.PI * i / (SAMPLE_RATE_IN_HERTZ.toFloat() / frequency)) * 0x7FFF).toInt()
                    .toShort()
            samples[i] = sample
            samples[i + 1] = (-1 * sample).toShort()
            i += 2
        }
        return samples
    }

    private fun generateSilence(duration: Float): ShortArray {
        val count = getCountForDuration(duration)
        val samples = ShortArray(count)
        var i = 0
        while (i < count) {
            samples[i] = 0
            samples[i + 1] = 0
            i += 2
        }
        return samples
    }

    private fun getCountForDuration(duration: Float): Int {
        return (SAMPLE_RATE_IN_HERTZ * 2 * duration).toInt() and 1.inv()
    }

    private fun prepareAudioTrack(): AudioTrack {
        val audioAttributes = AudioAttributes.Builder().apply {
            setUsage(AudioAttributes.USAGE_MEDIA)
            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        }.build()

        val audioFormat = AudioFormat.Builder().apply {
            setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setSampleRate(SAMPLE_RATE_IN_HERTZ)
        }.build()

        val track = AudioTrack.Builder().apply {
            setAudioAttributes(audioAttributes)
            setAudioFormat(audioFormat)
            setTransferMode(AudioTrack.MODE_STREAM)
        }.build()
        return track
    }

    private fun getMinBufferSize() = AudioTrack.getMinBufferSize(
        SAMPLE_RATE_IN_HERTZ,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    )
}
