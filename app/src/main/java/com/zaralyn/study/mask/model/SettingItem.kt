package com.zaralyn.study.mask.model

data class SettingItem(
    val id: String,
    val title: String,
    val description: String,
    val type: SettingType
)

enum class SettingType {
    SWITCH,
    NAVIGATION,
    INFO
}