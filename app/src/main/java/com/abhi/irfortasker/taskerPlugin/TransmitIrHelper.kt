package com.abhi.irfortasker.taskerPlugin

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class TransmitIrHelper(config: TaskerPluginConfig<TransmitIrInput>) :
    TaskerPluginConfigHelper<TransmitIrInput, Unit, TransmitIrRunner>(config) {
    override val runnerClass: Class<TransmitIrRunner> get() = TransmitIrRunner::class.java
    override val inputClass: Class<TransmitIrInput> get() = TransmitIrInput::class.java
    override val outputClass: Class<Unit> get() = Unit::class.java
}