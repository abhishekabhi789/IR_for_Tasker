package com.abhi.irfortasker

import android.util.Log

class ProcessNecCode(private val necCode: String) {

    private val NEC_PULSE = 563
    private val NEC_SPACE_ONE = 1688
    private val NEC_SPACE_ZERO = 563
    private val NEC_HEADER_PULSE = 9000
    private val NEC_HEADER_SPACE = 4500
    private val CARRIER_FREQUENCY = 38000

    fun getFrequency(): Int {
        return CARRIER_FREQUENCY
    }

    fun getPattern(): IntArray? {
        //nec - [Inverse Command][Command][Inverse Address][Address]
        //nec extended - [Inverse Command][Command][16-bit Address]
        return try {
            val necValue = necCode.removePrefix("0x").toLong(16)
            val pattern = mutableListOf<Int>()

            pattern.add(NEC_HEADER_PULSE)
            pattern.add(NEC_HEADER_SPACE)

            for (byteIndex in 0 until 4) {
                val byte = (necValue shr (byteIndex * 8)) and 0xFF

                for (bit in 0 until 8) {
                    pattern.add(NEC_PULSE)

                    if ((byte shr bit) and 1L == 1L) {
                        pattern.add(NEC_SPACE_ONE)
                    } else {
                        pattern.add(NEC_SPACE_ZERO)
                    }
                }
            }

            pattern.add(NEC_PULSE)

            pattern.toIntArray()

        } catch (e: NumberFormatException) {
            Log.e(TAG, "getPattern: failed to parse the code", e)
            null
        }
    }

    companion object {
        private const val TAG = "ProcessNecCode"
    }
}
