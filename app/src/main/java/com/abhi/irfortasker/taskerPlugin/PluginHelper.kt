package com.abhi.irfortasker.taskerPlugin

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class PluginHelper(config: TaskerPluginConfig<PluginInput>) :
    TaskerPluginConfigHelper<PluginInput, Unit, PluginRunner>(config) {
    override val runnerClass: Class<PluginRunner> get() = PluginRunner::class.java
    override val inputClass: Class<PluginInput> get() = PluginInput::class.java
    override val outputClass: Class<Unit> get() = Unit::class.java
}
