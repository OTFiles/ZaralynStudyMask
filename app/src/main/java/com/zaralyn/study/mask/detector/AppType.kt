package com.zaralyn.study.mask.detector

/**
 * 应用类型枚举
 */
enum class AppType(
    val displayName: String,
    val description: String
) {
    STANDARD("标准应用", "普通的Android应用"),
    NATIVE("Native应用", "使用NativeActivity的应用，如游戏"),
    BROWSER("浏览器", "浏览器类应用"),
    GAME("游戏", "游戏类应用"),
    WEBVIEW("WebView应用", "主要使用WebView的应用"),
    UNKNOWN("未知", "无法识别的应用类型")
}