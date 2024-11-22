package com.abhi.irfortasker

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeConversionTest {

    @Test
    fun testProcessHexCode() {
        val hexCode =
            "0000 006D 0000 0008 0060 0040 0040 0020 0020 0040 0020 0040 0020 0040 0020 0040 0020 0020 0020 0D7A"
        val code = ProcessHexCode(hexCode)
        assertEquals(38028, code.getFrequency())
        //comparing as string for easy visual check
        assertEquals(
            "2496,1664,1664,832,832,1664,832,1664,832,1664,832,1664,832,832,832,89700",
            code.getPattern().joinToString(",")
        )

    }

    @Test
    fun testProcessRawCode() {
        val rawCode =
            "38028,2526,1684,1684,842,842,1684,842,1684,842,1684,842,1684,842,842,842,90789"
        val code = ProcessRawCode(rawCode)
        assertEquals(38028, code.getFrequency())
        assertEquals(
            "2526,1684,1684,842,842,1684,842,1684,842,1684,842,1684,842,842,842,90789",
            code.getPattern().joinToString(",")
        )
    }
}
