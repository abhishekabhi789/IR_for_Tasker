package com.abhi.irfortasker.taskerPlugin

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.abhi.irfortasker.PrepareCode
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultErrorWithOutput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class TransmitIrRunner : TaskerPluginRunnerAction<TransmitIrInput, Unit>() {
    private val TAG = javaClass.simpleName
    override fun run(
        context: Context, input: TaskerInput<TransmitIrInput>
    ): TaskerPluginResult<Unit> {
        val inputCode = input.regular.inputCode.toString()
        val shouldVibrate = input.regular.shouldVibrate
        val tryAudioPulseMethod = input.regular.tryAudioPulseMethod
        val preparedCode = PrepareCode(inputCode)
        if (preparedCode.isValidCode()) {
            return try {
                vibrate(context, 50, shouldVibrate)
                val isSuccess = preparedCode.transmitCode(context, tryAudioPulseMethod)
                if (isSuccess) {
                    TaskerPluginResultSucess()
                } else {
                    val (err, errMsg) = preparedCode.getErrorDetails()
                    vibrate(context, 500, shouldVibrate)
                    Log.e(TAG, "run: Error: $errMsg | input: $inputCode")
                    TaskerPluginResultErrorWithOutput(err, errMsg)
                }
            } catch (e: Exception) {
                TaskerPluginResultErrorWithOutput(5, e.message ?: "unknown error")
            }
        } else {
            val (err, errMsg) = preparedCode.getErrorDetails()
            vibrate(context, 500, shouldVibrate)
            Log.e(TAG, "run: Error: $errMsg")
            return TaskerPluginResultErrorWithOutput(err, errMsg)
        }
    }

    private fun vibrate(context: Context, duration: Long, shouldVibrate: Boolean) {
        if (!shouldVibrate) return
        val vibrator = getSystemService(context, Vibrator::class.java)
        if (vibrator != null && vibrator.hasVibrator() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator!!.vibrate(duration)
        }
    }
}
