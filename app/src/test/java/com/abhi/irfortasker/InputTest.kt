package com.abhi.irfortasker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IrCodeHelperTest {
    private val irCodeHelper = IrCodeHelper()

    @Test
    fun testProcessHexCode() {
        val input =
            "0000 006D 0000 0008 0060 0040 0040 0020 0020 0040 0020 0040 0020 0040 0020 0040 0020 0020 0020 0D7A"
        irCodeHelper.updateInputCode(input)
        assertTrue(irCodeHelper.isCodeValidToTransmit())
        assertEquals(CodeType.HEX, irCodeHelper.determineInputType())
    }

    @Test
    fun testValidRawCode() {
        val input = "38028,2526,1684,1684,842,842,1684,842,1684,842,1684,842,1684,842,842,842,90789"
        irCodeHelper.updateInputCode(input)
        assertTrue(irCodeHelper.isCodeValidToTransmit())
        assertEquals(CodeType.RAW, irCodeHelper.determineInputType())
    }

    @Test
    fun testInvalidHexCode() {
        val irCodeHelper = IrCodeHelper()
        val input =
            "0000 000D 0000 0008 0060 0040 0040 0020 0020 0040 0020 0040 0020 0040 0020 0040 0020 0020 0020 0D7A"
        irCodeHelper.updateInputCode(input)
        assertFalse(irCodeHelper.isCodeValidToTransmit())
    }

    @Test
    fun testEmptyVariable() {
        val input = "%input"
        irCodeHelper.updateInputCode(input)
        assertFalse(irCodeHelper.isCodeValidToTransmit())
        assertEquals(CodeType.EMPTY_VARIABLE, irCodeHelper.determineInputType())
    }

    @Test
    fun testGetErrorDetails() {
        val input =
            "0000 006D 0000 0005 0060 0040 0040 0020 0020 0040 0020 0040 0020 0040 0020 0040 0020 0020 0020 0D7A"
        irCodeHelper.updateInputCode(input)
        assertFalse(irCodeHelper.isCodeValidToTransmit())
        assertEquals(ErrorCodes.INVALID_LENGTH.code, irCodeHelper.errorDetails?.first)
    }
}
