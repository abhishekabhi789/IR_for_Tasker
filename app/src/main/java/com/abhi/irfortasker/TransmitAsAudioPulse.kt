package com.abhi.irfortasker

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.abhi.irfortasker.TransmitAsAudioPulse.transmit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin


/**
 * This object generates audio tones for IR pulses and [transmit] them as IR pulses through a 3.5mm audio jack.
 */
object TransmitAsAudioPulse {
    private const val TAG = "TransmitAsAudioPulse"
    private const val SAMPLE_RATE = 44100
    private const val MAX_AMPLITUDE = 0x7FFF

    /**
     * Transmit the infrared pattern as audio pulses
     *
     * @param frequency The IR carrier frequency in Hertz.
     * @param pattern The alternating on/off pattern in microseconds to transmit.
     */
    suspend fun transmit(context: Context, frequency: Int, pattern: IntArray) {
        //based on: https://github.com/g-r-a-v-i-t-y-w-a-v-e/ProntoDroid/blob/master/app/src/main/java/paronomasia/prontodroid/Pronto.java
        Log.i(TAG, "frequency: $frequency,  pattern:${pattern.joinToString(",")}")
        withContext(Dispatchers.IO) {

            val patternInSeconds = pattern.map { it.toFloat() / 1_000_000 }.toFloatArray()
            Log.d(TAG, "transmit: patternInSeconds ${patternInSeconds.joinToString(",")}")

            val duration = patternInSeconds.sum()
            if (duration > 2) {
                Log.e(TAG, "transmit: duration is greater than 2s = $duration")
                return@withContext //duration higher than 2 seconds is invalid
            }

            var sampleCount: Int = getCountForDuration(duration)
            Log.d(TAG, "transmit: duration: $duration sample count: $sampleCount")

            val minBufferSize = getMinBufferSize()
            val bufferSizeInBytes = sampleCount * Short.SIZE_BYTES
            if (bufferSizeInBytes < minBufferSize) {
                //if buffer size is below minimum value, then duration of last pulse increased to compensate
                Log.i(TAG, "transmit: bufferSize : $bufferSizeInBytes < $minBufferSize")
                val increaseInCount = (minBufferSize - bufferSizeInBytes) / Short.SIZE_BYTES
                sampleCount += increaseInCount
                val increaseInPulse: Float = increaseInCount / (SAMPLE_RATE * 2F)
                //increasing the duration of the last pulse (lead out off pulse, safe to increase)
                patternInSeconds[patternInSeconds.size - 1] += increaseInPulse
                Log.i(
                    TAG,
                    "Buffer size adjusted: New count = $sampleCount," +
                            " new pattern = ${patternInSeconds.joinToString(",")}" +
                            "new duration ${patternInSeconds.sum()}" +
                            "new count = "
                )
            }

            val samples = ShortArray(sampleCount).apply {
                var offset = 0
                for ((index, currentVal) in patternInSeconds.withIndex()) {
                    val generated = if (index % 2 == 0)
                        generateTones(currentVal, frequency)
                    else generateSilence(currentVal)
                    generated.copyInto(destination = this, destinationOffset = offset)
                    offset += generated.size
                }
            }

            val track = prepareAudioTrack()
            if (track.state == AudioTrack.STATE_UNINITIALIZED) {
                Log.e(TAG, "AudioTrack initialization failed")
                return@withContext
            }

            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val originalVolumeLevel = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            try {
                am.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
                )
            } catch (e: Exception) {
                Log.e(TAG, "transmit: audioManager setStreamVolume failed", e)
            }
            am.mode = AudioManager.MODE_NORMAL
            am.isWiredHeadsetOn = true

            try {
                if (track.state != AudioTrack.STATE_UNINITIALIZED) {
                    track.write(samples, 0, sampleCount)
                    track.play()
                } else {
                    Log.e(TAG, "transmit: failed to play, track not initialized")
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException. Track isn't properly initialized", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unknown exception", e)
            } finally {
                Log.i(TAG, "transmit: finished transmission")
                try {
                    Log.i(TAG, "transmit: restoring volume level to $originalVolumeLevel")
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolumeLevel, 0)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore volume: ${e.message}", e)
                } finally {
                    track.flush()
                    track.release()
                }
            }
        }
    }

    //based on: https://gist.github.com/slightfoot/6330866

    private val tonesCache = mutableMapOf<Float, ShortArray>()

    /** Generates tones at for the provided duration*/
    @Synchronized
    private fun generateTones(duration: Float, frequency: Int): ShortArray {
        return tonesCache.getOrPut(duration) {
            val count = getCountForDuration(duration)
            ShortArray(count).apply {
                for (i in indices step 2) {
                    val short =
                        (MAX_AMPLITUDE * sin((i * Math.PI * frequency) / SAMPLE_RATE)).toInt()
                            .toShort()
                    this[i] = short
                    this[i + 1] = short.unaryMinus().toShort()
                }
            }
        }
    }

    private val silenceCache = mutableMapOf<Float, ShortArray>()
    private fun generateSilence(duration: Float): ShortArray {
        return silenceCache.getOrPut(duration) {
            val count = getCountForDuration(duration)
            ShortArray(count).apply {
                for (i in indices step 2) {
                    this[i] = 0
                    this[i + 1] = 0
                }
            }
        }
    }

    /** returns the sample count for the duration based on [SAMPLE_RATE].
     * @param duration duration of tone*/
    private fun getCountForDuration(duration: Float): Int {
        return (SAMPLE_RATE * 2 * duration).toInt() and 1.inv()
    }

    private fun prepareAudioTrack(): AudioTrack {
        val audioAttributes = AudioAttributes.Builder().apply {
            setUsage(AudioAttributes.USAGE_MEDIA)
            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        }.build()

        val audioFormat = AudioFormat.Builder().apply {
            setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setSampleRate(SAMPLE_RATE)
        }.build()

        val track = AudioTrack.Builder().apply {
            setAudioAttributes(audioAttributes)
            setAudioFormat(audioFormat)
            setTransferMode(AudioTrack.MODE_STREAM)
        }.build()
        return track
    }

    /** Calculates the [AudioTrack.getMinBufferSize] for the current settings.
     * @return minimum buffer size in bytes*/
    private fun getMinBufferSize() = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    )
}
