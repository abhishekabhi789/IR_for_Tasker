package com.abhi.irfortasker.taskerPlugin

import com.abhi.irfortasker.R
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class TransmitIrInput @JvmOverloads constructor(
    @field:TaskerInputField("inputCode", R.string.io_input_code_variable_label)
    var inputCode: String? = null,
    @field:TaskerInputField("shouldVibrate", R.string.io_input_vibrate_option_label)
    var shouldVibrate: Boolean = true, //default
    @field:TaskerInputField("tryAudioPulseMethod", R.string.io_audio_pulse_method_label)
    var tryAudioPulseMethod: Boolean = false //default
)
