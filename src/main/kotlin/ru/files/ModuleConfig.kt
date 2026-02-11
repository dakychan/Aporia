package ru.files

data class ModuleConfig(
    val enabled: Boolean,
    val settings: Map<String, String>
)
