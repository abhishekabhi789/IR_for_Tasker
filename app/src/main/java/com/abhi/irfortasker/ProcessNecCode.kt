package com.abhi.irfortasker

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
        if (necCode.length != 8) {
            return null
        }
        try {
            val necValue = necCode.toLong(16)
            val pattern = mutableListOf<Int>()

            pattern.add(NEC_HEADER_PULSE)
            pattern.add(NEC_HEADER_SPACE)

            for (i in 31 downTo 0) {
                pattern.add(NEC_PULSE)
                if ((necValue shr i) and 1L == 1L) {
                    pattern.add(NEC_SPACE_ONE)
                } else {
                    pattern.add(NEC_SPACE_ZERO)
                }
            }

            pattern.add(NEC_PULSE)

            return pattern.toIntArray()

        } catch (e: NumberFormatException) {
            return null
        }
    }
}
