package com.abhi.irfortasker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrepareCodeTest {

    @Test
    fun testProcessHexCode() {
        val input =
            "0000 006D 0000 0008 0060 0040 0040 0020 0020 0040 0020 0040 0020 0040 0020 0040 0020 0020 0020 0D7A"
        val code = PrepareCode(input)
        assertTrue(code.isValidCode())
        assertEquals(CodeType.HEX, code.getInputType())
    }

    @Test
    fun testValidRawCode() {
        val input = "38028,2526,1684,1684,842,842,1684,842,1684,842,1684,842,1684,842,842,842,90789"
        val code = PrepareCode(input)
        assertTrue(code.isValidCode())
        assertEquals(CodeType.RAW, code.getInputType())
    }

    @Test
    fun testInvalidHexCode() {
        val input =
            "0000 000D 0000 0008 0060 0040 0040 0020 0020 0040 0020 0040 0020 0040 0020 0040 0020 0020 0020 0D7A"
        val code = PrepareCode(input)
        assertFalse(code.isValidCode())
    }

    @Test
    fun testEmptyVariable() {
        val input = "%input"
        val prepareCode = PrepareCode(input)
        assertFalse(prepareCode.isValidCode())
        assertEquals(CodeType.EMPTY_VARIABLE, prepareCode.getInputType())
    }

    @Test
    fun testGetErrorDetails() {
        val input =
            "0000 006D 0000 0005 0060 0040 0040 0020 0020 0040 0020 0040 0020 0040 0020 0040 0020 0020 0020 0D7A"
        val prepareCode = PrepareCode(input)
        assertFalse(prepareCode.isValidCode())
        assertEquals(
            Pair(
                4,
                "invalid length, calculated length - 14 | actual length = 20"
            ), prepareCode.getErrorDetails()
        )
    }
}
