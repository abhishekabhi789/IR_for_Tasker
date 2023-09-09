package com.abhi.irfortasker

/**Helps to get frequency and pattern from raw input*/
class ProcessRawCode(inputCode: String) {
    private val frequency: Int
    private val pattern: IntArray

    init {
        val dataArr = inputCode.replace(" ", "").split(",").toMutableList()
        frequency = dataArr[0].toInt()
        pattern =
            dataArr.subList(1, dataArr.size).map { ((it.toLong() * frequency) / 1_000_000).toInt() }
                .toIntArray()
    }

    fun getFrequency(): Int {
        return frequency
    }

    fun getPattern(): IntArray {
        return pattern
    }
}