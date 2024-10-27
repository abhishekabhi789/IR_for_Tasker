package com.abhi.irfortasker.taskerPlugin

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.abhi.irfortasker.ErrorCodes
import com.abhi.irfortasker.IrCodeHelper
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultErrorWithOutput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class PluginRunner : TaskerPluginRunnerAction<PluginInput, Unit>() {
    private val TAG = javaClass.simpleName
    private val irCodeHelper = IrCodeHelper()
    override fun run(
        context: Context, input: TaskerInput<PluginInput>
    ): TaskerPluginResult<Unit> {
        val inputCode = input.regular.inputCode.toString()
        val shouldVibrate = input.regular.shouldVibrate
        val tryAudioPulseMethod = input.regular.tryAudioPulseMethod
        irCodeHelper.updateInputCode(inputCode)
        return if (irCodeHelper.isCodeValidToTransmit()) {
            try {
                vibrate(context, 50, shouldVibrate)
                val isSuccess = irCodeHelper.transmitCode(context, tryAudioPulseMethod)
                Log.i(TAG, "run: transmission success $isSuccess")
                if (isSuccess) {
                    TaskerPluginResultSucess()
                } else {
                    irCodeHelper.errorDetails?.let { (err, errMsg) ->
                        vibrate(context, 500, shouldVibrate)
                        Log.e(TAG, "run: Error: $errMsg | input: $inputCode")
                        return TaskerPluginResultErrorWithOutput(err, errMsg)
                    } ?: ErrorCodes.ERROR.let { error ->
                        TaskerPluginResultErrorWithOutput(error.code, error.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "run: unknown error during transmission", e)
                TaskerPluginResultErrorWithOutput(5, e.message ?: "unknown error")
            }
        } else {
            irCodeHelper.errorDetails?.let { (err, errMsg) ->
                vibrate(context, 500, shouldVibrate)
                Log.e(TAG, "run: Error: $errMsg | input: $inputCode")
                TaskerPluginResultErrorWithOutput(err, errMsg)
            } ?: ErrorCodes.ERROR.let { error ->
                TaskerPluginResultErrorWithOutput(error.code, error.message)
            }
        }
    }

    private fun vibrate(context: Context, duration: Long, shouldVibrate: Boolean) {
        if (!shouldVibrate) return
        getSystemService(context, Vibrator::class.java)?.run {
            if (hasVibrator() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrate(duration)
            }
        }
    }
}