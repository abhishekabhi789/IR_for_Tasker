package com.abhi.irfortasker

import android.content.Context
import android.util.Log
import com.abhi.irfortasker.taskerPlugin.TransmissionMethod
import net.dinglisch.android.tasker.TaskerPlugin.variableNameValid
import java.util.Collections.frequency


class IrCodeHelper(context: Context) {

    private val deviceIrHelper = DeviceIrHelper(context)

    private var inputCode: String? = null

    /** Stores error code and message for Tasker if an action fails. */
    var errorDetails: Pair<Int, String>? = null

    /** First call this method to update the input code
     * @param codeInput the input code from config UI or tasker runner*/
    fun updateInputCode(codeInput: String) {
        inputCode = codeInput
    }

    /** Returns the detected code type from the input string. */
    fun determineInputType(): CodeType = inputCode?.let { parseCodeType() } ?: CodeType.UNKNOWN

    /** Method to verify the input before saving the plugin */
    fun isInputValidToSave(): Boolean {
        return when (parseCodeType()) {
            CodeType.HEX, CodeType.RAW -> isCodeValidToTransmit()
            CodeType.EMPTY_VARIABLE -> true
            else -> false
        }
    }

    /** Method to verify the code before transmission. */
    fun isCodeValidToTransmit(): Boolean {
        return inputCode?.let {
            when (parseCodeType()) {
                CodeType.HEX -> isValidHexCode()
                CodeType.RAW -> isValidRawPulseCode()
                CodeType.EMPTY_VARIABLE -> false.also { updateError(ErrorCodes.EMPTY_INPUT) }
                else -> false
            }
        } ?: false
    }

    /** Transmits the processed IR code.
     * @param transmitAsAudioPulses if set true, in the absence of IR blaster, the code will be
     * transmitted as audio pulses through audio stream.
     */
    suspend fun transmitCode(context: Context, transmissionMethod: TransmissionMethod): Boolean {
        return inputCode?.let { input ->
            val (frequency, pattern) = when (parseCodeType()) {
                CodeType.HEX -> ProcessHexCode(input).run { getFrequency() to getPattern() }
                CodeType.RAW -> ProcessRawCode(input).run { getFrequency() to getPattern() }
                else -> return false
            }
            return performTransmission(context, transmissionMethod, frequency, pattern)
        } ?: false
    }

    private fun parseCodeType(): CodeType {
        return (inputCode?.let { input ->
            when {
                hexRegex.matches(input) -> CodeType.HEX
                rawRegex.matches(input) -> CodeType.RAW
                variableNameValid(input) -> CodeType.EMPTY_VARIABLE
                else -> CodeType.UNKNOWN
            }
        } ?: CodeType.UNKNOWN).also {
            Log.i(TAG, "parseCodeType: Codetype - $it")
        }
    }

    /**Preamble is the first four words in a Pronto HEX code which carries code type, [frequency], [length of burst pair sequence 1] and [length of burst pair sequence 2] respectively. */
    private fun extractPreamble(hex: String): List<String> =
        hex.trim().split("\\s+".toRegex()).take(4)

    /**Hex code contains a preamble and one or two burst pair sequences(BPS).
     *  Frequency is validated by checking its range, code length is calculated from preamble([getPreamble]) and compared with actual size.*/
    private fun isValidHexCode(): Boolean {
        return inputCode?.let { hexCode ->
            val preamble = extractPreamble(hexCode)
            val frequency = (1_000_000 / (preamble[1].toLong(16) * 0.241246)).toInt()
            //frequency should be within between 30 - 60 kHz
            val isFrequencyValid = isFrequencyValid(frequency)
            Log.i(TAG, "isValidHexCode: frequency $frequency")
            val actualCodeLength = hexCode.split(" ").size
            val calculatedLength = with(preamble) {
                //total size = preamble length + lenBPS1 + lenBPS2
                size + (get(2).toInt(16) * 2) + (get(3).toInt(16) * 2)
            }
            val isLengthValid = calculatedLength == actualCodeLength
            Log.i(TAG, "isValidHexCode: lengths act $actualCodeLength calc. $calculatedLength")
            when {
                !isFrequencyValid -> updateError(ErrorCodes.INVALID_FREQUENCY, ": $frequency")
                !isLengthValid -> updateError(
                    ErrorCodes.INVALID_LENGTH,
                    ": calculated length - $calculatedLength | actual length = $actualCodeLength"
                )
            }
            isFrequencyValid && isLengthValid
        } ?: false
    }

    /** Validates a RAW pulse code by checking frequency. */
    private fun isValidRawPulseCode(): Boolean {
        return inputCode?.let { rawCode ->
            val frequency = ProcessRawCode(rawCode).getFrequency()
            val isFrequencyValid = isFrequencyValid(frequency)
            Log.i(TAG, "isValidRawPulseCode: freq. $frequency, valid $isFrequencyValid")
            if (!isFrequencyValid) updateError(ErrorCodes.INVALID_FREQUENCY)
            isFrequencyValid
        } ?: false
    }

    /** check whether the frequency lies between 30kHz to 60kHz.
     * In general IR remote pulses are at 38kHz */
    private fun isFrequencyValid(frequency: Int): Boolean {
        return frequency in (30_000..60_000)
    }

    fun hasDeviceEmitter() = deviceIrHelper.hasIrEmitter()

    /**The function will check hardware and calls appropriate method.*/
    private suspend fun performTransmission(
        context: Context, transmissionMethod: TransmissionMethod, frequency: Int, pattern: IntArray
    ): Boolean {
        val hasEmitter = hasDeviceEmitter()
        Log.d(TAG, "performTransmission:  $frequency ${pattern.joinToString(separator = ",")}")
        Log.i(TAG, "performTransmission: hasEmitter $hasEmitter")
        try {
            when (transmissionMethod) {
                TransmissionMethod.DeviceIrBlaster -> {
                    if (!hasEmitter) {
                        updateError(ErrorCodes.NO_BUILTIN_IR_BLASTER)
                        return false
                    }
                    deviceIrHelper.transmit(frequency, pattern)
                    return true
                }

                TransmissionMethod.AudioPulse -> {
                    val isOnWiredHeadset = AudioUtils.isOnWiredHeadset(context)
                    if (!isOnWiredHeadset) {
                        Log.e(TAG, "performTransmission: no audio ir blaster")
                        updateError(ErrorCodes.NO_WIRED_HEADPHONE_CONNECTED)
                        return false
                    }
                    TransmitAsAudioPulse.transmit(context, frequency, pattern)
                    return true
                }
            }

        } catch (e: Error) {
            Log.e(TAG, "performTransmission: Failed to transmit!", e)
            updateError(ErrorCodes.UNKNOWN_ERROR_DURING_TRANSMISSION)
            return false
        }
    }

    private fun updateError(errorCode: ErrorCodes, message: String? = null) {
        Log.e(TAG, "updateError: error reported $errorCode $message")
        errorDetails =
            errorCode.let { error ->
                error.code to error.message + (message?.let { "\n$it" } ?: "")
            }
    }

    companion object {
        private const val TAG = "IrCodeHelper"
        private val hexRegex = Regex("^[0-9a-fA-F]{4}(\\s[0-9a-fA-F]{4})*$")
        private val rawRegex = Regex("^\\s*\\b\\d+\\s*(?:,\\s*\\d+\\s*)*\\b\\s*")
    }
}
