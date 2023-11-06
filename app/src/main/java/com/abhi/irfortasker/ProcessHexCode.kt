package com.abhi.irfortasker

/**This class converts Pronto HEX input into [frequency] and [pattern].*/
class ProcessHexCode(hexCode: String) {
    private val frequency: Int
    private val pattern: IntArray

    init {
        val hexArray = hexCode.split(" ").toMutableList()
        frequency = (1_000_000 / (hexArray[1].toLong(16) * 0.241246)).toInt()
        pattern = hexArray.subList(4, hexArray.size).map { it.toInt(16) }.toIntArray()
    }

    fun getFrequency(): Int {
        return frequency
    }

    fun getPattern(): IntArray {
        return pattern
    }
}