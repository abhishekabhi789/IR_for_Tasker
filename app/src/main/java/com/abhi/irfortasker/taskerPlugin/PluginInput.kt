package com.abhi.irfortasker.taskerPlugin

import android.annotation.SuppressLint
import com.abhi.irfortasker.R
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@SuppressLint("NonConstantResourceId")
@TaskerInputRoot
class PluginInput @JvmOverloads constructor(
    @field:TaskerInputField("inputCode", R.string.input_code_variable_label)
    var inputCode: String? = null,
    @field:TaskerInputField("shouldVibrate", R.string.input_vibrate_option_label)
    var shouldVibrate: Boolean = true, //default
    @field:TaskerInputField("transmissionMethod", R.string.input_transmission_method)
    var transmissionMethod: String? = null, //let the hardware decide default
)
