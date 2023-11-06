package com.abhi.irfortasker

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import com.abhi.irfortasker.taskerPlugin.TransmitIrHelper

/**show the text as toast*/
fun String.toToast(context: Context) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, this, Toast.LENGTH_LONG).show()
    }
}

fun Activity.showAlertDialog(
    title: String,
    items: List<String>,
    callback: (String?) -> Unit
) {
    AlertDialog.Builder(this).apply {
        setIcon(R.mipmap.ic_launcher)
        setTitle(title)
        val arrayAdapter = ArrayAdapter<String>(
            this@showAlertDialog,
            android.R.layout.select_dialog_singlechoice
        ).apply {
            addAll(items)
        }
        setAdapter(arrayAdapter) { _, which -> callback(arrayAdapter.getItem(which)) }
        setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
            dialog.dismiss(); callback(
            null
        )
        }
    }.show()
}

fun isValidInputConfigs(context: Context, codeInput: String): Boolean {
    val TAG = "util.isValidInputConfig"
    if (codeInput.isEmpty()) {
        context.getString(R.string.error_empty_input).toToast(context)
        Log.d(TAG, "empty input")
        return false
    } else {
        val prepareCode = PrepareCode(codeInput)
        when (val inputType = prepareCode.getInputType()) {
            CodeType.EMPTY_VARIABLE -> {
                context.getString(R.string.input_type_saved, inputType.name).toToast(context)
                Log.d(TAG, "empty variable")
                return true
            }

            CodeType.HEX, CodeType.RAW -> {
                return if (prepareCode.isValidInput()) {
                    context.getString(R.string.input_type_saved, inputType.name).toToast(context)
                    Log.d(TAG, "valid ${inputType.name}")
                    true
                } else {
                    context.getString(R.string.error_input_is_invalid).toToast(context)
                    Log.d(TAG, "blocking invalid input: $codeInput")
                    false
                }
            }

            else -> {
                context.getString(R.string.invalid_input_ask_to_choose_valid_code).toToast(context)
                Log.d(TAG, "invalid input: $codeInput")
                return false
            }
        }
    }
}

/**List the variable from tasker and ask user to choose one.*/
fun showVariableDialog(activity: Activity, taskerHelper: TransmitIrHelper) {
    val relevantVariables = taskerHelper.relevantVariables.toList()
    if (relevantVariables.isEmpty()) {
        activity.getString(R.string.no_variable_to_show).toToast(
            activity
        )
        return
    }
    activity.showAlertDialog(
        activity.getString(R.string.variable_dialog_title),
        relevantVariables
    ) {
        activity.findViewById<EditText>(R.id.codeInputField).setText(it)
    }
}

/**Validate the input and save.*/
fun saveInput(activity: Activity, taskerHelper: TransmitIrHelper) {
    val codeInput = activity.findViewById<EditText>(R.id.codeInputField).text.toString()
    val isValid = isValidInputConfigs(activity, codeInput)
    if (isValid && taskerHelper.onBackPressed().success) {
        taskerHelper.finishForTasker()
    }
}