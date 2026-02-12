package com.zaralyn.study.mask.strategy

import android.app.Activity
import android.content.SharedPreferences
import com.zaralyn.study.mask.detector.AppType
import com.zaralyn.study.mask.logger.Logger

/**
 * UI替换上下文
 * 封装UI替换所需的所有上下文信息
 */
data class UIReplacementContext(
    val activity: Activity,
    val appType: AppType,
    val prefs: SharedPreferences,
    val logger: Logger,
    val attempt: Int = 0,
    val maxAttempts: Int = 10
) {
    /**
     * 检查Activity是否有效
     */
    fun isActivityValid(): Boolean {
        return !activity.isFinishing && !activity.isDestroyed
    }
    
    /**
     * 检查是否应该显示原应用
     */
    fun shouldShowOriginal(): Boolean {
        // 检查Intent extras
        if (activity.intent != null) {
            if (activity.intent.getBooleanExtra("show_original_app", false)) {
                return true
            }
        }
        
        // 检查SharedPreferences
        if (prefs.getBoolean("show_original_app", false)) {
            return true
        }
        
        return false
    }
    
    /**
     * 创建下一个尝试的上下文
     */
    fun nextAttempt(): UIReplacementContext {
        return copy(attempt = attempt + 1)
    }
    
    /**
     * 检查是否达到最大尝试次数
     */
    fun isMaxAttemptsReached(): Boolean {
        return attempt >= maxAttempts
    }
}