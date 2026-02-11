package com.abhi.irfortasker

import android.content.ContentValues.TAG
import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Arrays

class DeviceIrHelper(context: Context) {
    private val irManager =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    /** Whether the device has built-in IR blaster */
    fun hasIrEmitter(): Boolean = irManager?.hasIrEmitter() ?: false

    /** Transmits the ir signal.
     * @param frequency The IR carrier frequency in Hertz.
     * @param pattern The alternating on/off pattern in microseconds to transmit.
     * @return true if transmission was successful, false otherwise.
     */
    fun transmit(frequency: Int, pattern: IntArray): Boolean {
        return try {
            irManager?.transmit(frequency, pattern)
            true
        } catch (e: Exception) {
            false
        }
    }
}
