package com.abhi.irfortasker.taskerPlugin

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.widget.ArrayAdapter
import android.widget.Toast
import com.abhi.irfortasker.CodeType
import com.abhi.irfortasker.IrCodeHelper
import com.abhi.irfortasker.R
import com.abhi.irfortasker.databinding.PluginconfiglayoutBinding
import com.abhi.irfortasker.taskerPlugin.TransmissionMethod.AudioPulse
import com.abhi.irfortasker.taskerPlugin.TransmissionMethod.DeviceIrBlaster
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

/**
 * Activity class that handles plugin configuration from tasker.
 * */
class PluginConfigActivity : Activity(), TaskerPluginConfig<PluginInput> {
    private lateinit var binding: PluginconfiglayoutBinding
    private lateinit var irCodeHelper: IrCodeHelper
    override val context: Context get() = applicationContext
    override val inputForTasker: TaskerInput<PluginInput>
        get() {
            val selectedMethod =
                when (binding.transmissionMethodGroup.checkedRadioButtonId) {
                    R.id.deviceIrBlasterMethod -> DeviceIrBlaster.name
                    R.id.audioPulseMethod -> AudioPulse.name
                    else -> getSuggestedTransmissionMode().name
                }
            return TaskerInput(
                PluginInput(
                    binding.codeInputField.text?.toString(),
                    binding.shouldVibrate.isChecked,
                    selectedMethod
                )
            )
        }

    override fun assignFromInput(input: TaskerInput<PluginInput>) = input.regular.run {
        binding.codeInputField.setText(inputCode)
        binding.shouldVibrate.isChecked = input.regular.shouldVibrate
        binding.transmissionMethodGroup.let { methodGroup ->
            when (transmissionMethod) {
                DeviceIrBlaster.name -> methodGroup.check(R.id.deviceIrBlasterMethod)
                AudioPulse.name -> methodGroup.check(R.id.audioPulseMethod)
                else -> {
                    when (getSuggestedTransmissionMode()) {
                        DeviceIrBlaster -> binding.deviceIrBlasterMethod.isChecked = true
                        AudioPulse -> binding.audioPulseMethod.isChecked = true
                    }
                }
            }
        }
    }

    private val taskerHelper by lazy { PluginHelper(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        irCodeHelper = IrCodeHelper(this@PluginConfigActivity)
        binding = PluginconfiglayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        taskerHelper.onCreate()
        binding.variableButton.setOnClickListener {
            showVariableChooseDialog(taskerHelper) { chosenVariable ->
                Log.i(TAG, "onCreate: chosen variable - $chosenVariable")
                binding.codeInputField.setText(chosenVariable)
            }
        }
        binding.saveConfigButton.setOnClickListener {
            saveInput(taskerHelper)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            saveInput(taskerHelper)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun saveInput(taskerHelper: PluginHelper) {
        val codeInput = binding.codeInputField.text.toString()
        val isValid = isValidInputConfigs(codeInput)
        if (isValid) taskerHelper.finishForTasker()
    }

    private fun isValidInputConfigs(codeInput: String): Boolean {
        if (codeInput.isEmpty()) {
            getString(R.string.error_empty_input).toToast()
            Log.i(TAG, "isValidInputConfigs: false - input is empty")
            return false
        } else {
            irCodeHelper.updateInputCode(codeInput)
            when (val inputType = irCodeHelper.determineInputType()) {
                CodeType.EMPTY_VARIABLE -> {
                    getString(R.string.input_type_saved, inputType.name).toToast()
                    Log.i(TAG, "isValidInputConfigs: true - empty variable : $codeInput")
                    return true
                }

                CodeType.HEX, CodeType.RAW, CodeType.NEC -> {
                    return if (irCodeHelper.isInputValidToSave()) {
                        getString(R.string.input_type_saved, inputType.name).toToast()
                        Log.i(TAG, "isValidInputConfigs: true - valid code ${inputType.name}")
                        true
                    } else {
                        getString(R.string.error_input_is_invalid).toToast()
                        Log.i(TAG, "isValidInputConfigs: false - invalid- $codeInput")
                        false
                    }
                }

                else -> {
                    getString(R.string.invalid_input_ask_to_choose_valid_code).toToast()
                    Log.i(TAG, "isValidInputConfigs: false unknown input $codeInput")
                    return false
                }
            }
        }
    }

    private fun showVariableChooseDialog(
        taskerHelper: PluginHelper,
        onSelected: (String?) -> Unit
    ) {
        val relevantVariables = taskerHelper.relevantVariables.toList()
        if (relevantVariables.isEmpty()) {
            getString(R.string.no_variable_to_show).toToast()
            return
        }
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(relevantVariables)
        AlertDialog.Builder(this).apply {
            setIcon(R.drawable.ic_tasker_variable)
            setTitle(R.string.input_variable_import_dialog_title)
            setAdapter(arrayAdapter) { _, itemPosition ->
                onSelected(arrayAdapter.getItem(itemPosition))
            }
            setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                onSelected(null)
                dialog.dismiss()
            }
        }.show()
    }

    private fun String.toToast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, this, Toast.LENGTH_LONG).show()
        }
    }

    /** Suggestion based on hardware capability*/
    private fun getSuggestedTransmissionMode(): TransmissionMethod {
        return if (irCodeHelper.hasDeviceEmitter()) DeviceIrBlaster else AudioPulse
    }

    companion object {
        private const val TAG = "PluginConfigActivity"
    }
}
