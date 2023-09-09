package com.abhi.irfortasker.taskerPlugin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import com.abhi.irfortasker.R
import com.abhi.irfortasker.saveInput
import com.abhi.irfortasker.showVariableDialog
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

/**This class handles event plugin configuration activity in tasker.*/
class ActivityConfigTransmitIr : Activity(), TaskerPluginConfig<TransmitIrInput> {
    override val context: Context get() = applicationContext
    override val inputForTasker
        get() = TaskerInput(
            TransmitIrInput(
                findViewById<EditText>(R.id.codeInputField).text?.toString(),
                findViewById<Switch>(R.id.shouldVibrate).isChecked,
                findViewById<Switch>(R.id.tryAudioPulseMethod).isChecked
            )
        )

    override fun assignFromInput(input: TaskerInput<TransmitIrInput>) = input.regular.run {
        findViewById<EditText>(R.id.codeInputField).setText(inputCode)
        findViewById<Switch>(R.id.shouldVibrate).isChecked = input.regular.shouldVibrate
        findViewById<Switch>(R.id.tryAudioPulseMethod).isChecked = input.regular.tryAudioPulseMethod
    }

    private val taskerHelper by lazy { TransmitIrHelper(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pluginconfiglayout)
        taskerHelper.onCreate()
        findViewById<ImageButton>(R.id.variableButton).setOnClickListener {
            showVariableDialog(this, taskerHelper)
        }
        findViewById<Button>(R.id.saveConfigButton).setOnClickListener {
            saveInput(this, taskerHelper)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            saveInput(this, taskerHelper)
        }
        return super.onKeyDown(keyCode, event)
    }
}