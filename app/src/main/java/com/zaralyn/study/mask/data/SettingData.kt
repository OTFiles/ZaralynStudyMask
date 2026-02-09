package com.zaralyn.study.mask.data

import com.zaralyn.study.mask.model.SettingItem
import com.zaralyn.study.mask.model.SettingType

object SettingData {
    fun getSettingItems(): List<SettingItem> {
        return listOf(
            SettingItem("s1", "夜间模式", "切换深色主题", SettingType.SWITCH),
            SettingItem("s2", "自动播放音频", "自动播放课文音频", SettingType.SWITCH),
            SettingItem("s3", "隐私政策", "查看隐私政策", SettingType.NAVIGATION),
            SettingItem("s4", "用户协议", "查看用户协议", SettingType.NAVIGATION),
            SettingItem("s5", "版本信息", "版本 1.0.0", SettingType.INFO)
        )
    }
}