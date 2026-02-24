package aporia.cc.files

data class ModuleConfig(
    val enabled: Boolean,
    val settings: Map<String, String>
)
