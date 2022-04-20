package com.xh.flutter.assets.bean

data class ConfigBean(
    val targetDir: String,
    val scanDirs: List<String>,
    val isCreateMappingFile: Boolean,
    val createMappingDir: String,
    val createMappingFileName: String,
    val createMappingClassName: String,
    val isClearOldData: Boolean
)