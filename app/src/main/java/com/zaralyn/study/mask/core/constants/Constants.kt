package com.zaralyn.study.mask.core.constants

/**
 * 常量定义
 */
object Constants {
    
    // SharedPreferences
    const val PREFS_NAME = "ZaralynStudyMask"
    
    // 状态键
    const val KEY_SHOW_ORIGINAL = "show_original_app"
    const val KEY_SELECTED_GRADE = "selected_grade"
    const val KEY_CURRENT_FRAGMENT = "current_fragment"
    
    // Intent Extras
    const val EXTRA_SHOW_ORIGINAL = "show_original_app"
    const val EXTRA_GRADE = "grade"
    const val EXTRA_BOOK_TITLE = "book_title"
    const val EXTRA_BOOK_FILE = "book_file"
    
    // View IDs
    const val VIEW_ID_CUSTOM = -1  // 自定义视图ID
    const val VIEW_ID_OVERLAY = -2  // Overlay视图ID
    
    // 时间常量
    const val TAP_DETECTION_WINDOW_MS = 2000L  // 点击检测窗口（2秒）
    const val TAP_COUNT_REQUIRED = 5  // 需要点击次数
    const val DELAY_UI_REPLACEMENT_MS = 50L  // UI替换延迟
    const val MAX_UI_REPLACEMENT_ATTEMPTS = 10  // 最大尝试次数
    
    // 日志标签
    const val TAG = "ZaralynStudyMask"
    
    // 包名
    const val PACKAGE_NAME = "com.zaralyn.study.mask"
}