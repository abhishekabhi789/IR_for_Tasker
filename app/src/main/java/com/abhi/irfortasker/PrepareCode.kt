package com.abhi.irfortasker

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import net.dinglisch.android.tasker.TaskerPlugin.variableNameValid
import java.util.Collections.frequency


/**
 * Process the input and provides the methods [getInputType], [isValidInput], [isValidCode], [getErrorDetails]  and [transmitCode].*/
class PrepareCode(private val input: String) {
    private val TAG = javaClass.simpleName

    /**A pair of error code and error message to return to tasker, if action fails.*/
    private lateinit var errorDetails: Pair<Int, String>

    init {
        Log.d(TAG, "input: $input")
    }

    /**This function returns  the code type*/
    fun getInputType(): Enum<CodeType> {
        return getType(input)
    }

    /**Returns whether the input on config is valid IR code*/
    fun isValidInput(): Boolean {
        return when (getType(input)) {
            CodeType.HEX, CodeType.RAW -> return isValidCode()
            CodeType.EMPTY_VARIABLE -> true
            else -> false
        }
    }

    /**Returns whether the input on runner is valid IR code*/
    fun isValidCode(): Boolean {
        return when (getType(input)) {
            CodeType.HEX -> return isValidHexCode(input)
            CodeType.RAW -> return isValidRawPulseCode(input)
            else -> false
        }
    }

    /**Returns the and error message for tasker.*/
    fun getErrorDetails(): Pair<Int, String> {
        return if (::errorDetails.isInitialized) {
            errorDetails
        } else {
            val error: ErrorCodes = if (getType(input) == CodeType.EMPTY_VARIABLE) {
                ErrorCodes.EMPTY_INPUT
            } else {
                ErrorCodes.ERROR
            }
            Pair(error.code, error.message + input)
        }
    }

    /**Transmits the code previously prepared.*/
    fun transmitCode(context: Context, tryAudioPulseMethod: Boolean): Boolean {
        var frequency = 0
        var pattern: IntArray = intArrayOf()
        when (getType(input)) {
            CodeType.HEX -> {
                val code = ProcessHexCode(input)
                frequency = code.getFrequency()
                pattern = code.getPattern()
            }

            CodeType.RAW -> {
                val code = ProcessRawCode(input)
                frequency = code.getFrequency()
                pattern = code.getPattern()
            }
        }
        return transmitIr(context, tryAudioPulseMethod, frequency, pattern)
    }

    private fun getType(input: String): Enum<CodeType> {
        val regexHex = Regex("^[0-9a-fA-F]{4}(\\s[0-9a-fA-F]{4})*\$")
        val regexRaw = Regex("^\\s*\\b\\d+\\s*(?:,\\s*\\d+\\s*)*\\b\\s*")
        return when {
            regexHex.matches(input) -> CodeType.HEX
            regexRaw.matches(input) -> CodeType.RAW
            variableNameValid(input) -> CodeType.EMPTY_VARIABLE
            else -> CodeType.ERROR
        }
    }


    /**Preamble is the first four words in a Pronto HEX code which carries code type, [frequency], [length of burst pair sequence 1] and [length of burst pair sequence 2] respectively. */
    private fun getPreamble(hex: String): List<String> {
        val bytes = hex.trim().split("\\s+".toRegex())
        return bytes.subList(0, 4)
    }

    /**Hex code contains a preamble and one or two burst pair sequences(BPS).
     *  Frequency is validated by checking its range, code length is calculated from preamble([getPreamble]) and compared with actual size.*/
    private fun isValidHexCode(hexCode: String): Boolean {
        val preamble = getPreamble(hexCode)
        val frequency = (1_000_000 / (preamble[1].toLong(16) * 0.241246)).toInt()
        //frequency should lies between 30 - 60 kHz
        val isFrequencyValid = frequency in (30_000..60_000)
        val lenActual = hexCode.split(" ").size
        val lenCalculated =
            preamble.size + ((preamble[2].toInt(16) * 2) + (preamble[3].toInt(16)) * 2)
        val isLenValid = lenCalculated == lenActual //total size = preamble + lenBPS1 + lenBPS2
        if (!isFrequencyValid) {
            val error = ErrorCodes.INVALID_FREQUENCY
            errorDetails = Pair(error.code, "${error.message}: $frequency")
        }
        if (!isLenValid) {
            val error = ErrorCodes.INVALID_LENGTH
            errorDetails = Pair(
                error.code,
                "${error.message}: calculated length - $lenCalculated | actual length = $lenActual"
            )
        }
        return isFrequencyValid && isLenValid
    }

    /**Raw code is verified only by frequency. Couldn't find a good documentation that explains the code structure in this case*/
    private fun isValidRawPulseCode(raw: String): Boolean {
        val code = ProcessRawCode(raw)
        val frequency = code.getFrequency()
        val isFrequencyValid = frequency in (30_000..60_000)
        if (!isFrequencyValid) {
            val error = ErrorCodes.INVALID_FREQUENCY
            errorDetails = Pair(error.code, "${error.message}: $frequency")
        }
        //pattern = code.getPattern()
        return isFrequencyValid
    }

    /**The function will check hardware and calls appropriate method.*/
    private fun transmitIr(
        context: Context, tryAudioPulseMethod: Boolean, frequency: Int, pattern: IntArray
    ): Boolean {
        val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as ConsumerIrManager
        val hasEmitter = irManager.hasIrEmitter()
        Log.i(TAG, "transmitIr: hasEmitter $hasEmitter")
        try {
            return when {
                !hasEmitter && tryAudioPulseMethod -> {
                    val isOnWiredHeadset = AudioUtils.isOnWiredHeadset(context)
                    if (!isOnWiredHeadset) {
                        Log.e(TAG, "transmitIr: no audio ir blaster")
                        val error = ErrorCodes.NO_WIRED_HEADPHONE_CONNECTED
                        errorDetails = Pair(error.code, error.message)
                        return false
                    }
                    TransmitAsAudioPulse(frequency, pattern).transmit(context)
                    true
                }

                hasEmitter -> {
                    irManager.transmit(frequency, pattern)
                    true
                }

                else -> {
                    val error = ErrorCodes.NO_BUILTIN_IR_BLASTER
                    Log.d(TAG, "transmitIr: ${error.message}")
                    errorDetails = Pair(error.code, error.message)
                    false
                }
            }

        } catch (e: Error) {
            Log.e(TAG, "transmitIr: Exception!\n" + e.message)
        }
        val error = ErrorCodes.UNKNOWN_ERROR_DURING_TRANSMISSION
        errorDetails = Pair(error.code, error.message)
        return false
    }
}

