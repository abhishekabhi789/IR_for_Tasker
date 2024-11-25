package com.abhi.irfortasker

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceIrHelper(context: Context) {
    private val irManager =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    /** Whether the device has built-in IR blaster */
    fun hasIrEmitter(): Boolean = irManager?.hasIrEmitter() ?: false

    /** Transmits the ir signal in an IO coroutine.
     * @param frequency The IR carrier frequency in Hertz.
     * @param pattern The alternating on/off pattern in microseconds to transmit.
     */
    suspend fun transmit(frequency: Int, pattern: IntArray) {
        withContext(Dispatchers.IO) { irManager?.transmit(frequency, pattern) }
    }
}
