package com.zaralyn.study.mask.strategy

import android.app.Activity
import android.content.SharedPreferences

/**
 * UI替换策略接口
 * 定义UI替换的统一接口
 */
interface UIReplacementStrategy {
    
    /**
     * 策略类型
     */
    enum class Type {
        WINDOW_MANAGER_OVERLAY,      // WindowManager覆盖层方案
        DECOR_VIEW_MODIFICATION,     // DecorView修改方案
        FRAME_LAYOUT_WRAPPER         // FrameLayout包装方案
    }
    
    /**
     * 策略名称
     */
    fun getName(): String
    
    /**
     * 策略类型
     */
    fun getType(): Type
    
    /**
     * 检查是否可以应用此策略
     */
    fun canApply(context: UIReplacementContext): Boolean
    
    /**
     * 执行UI替换
     */
    fun replaceUI(context: UIReplacementContext): ReplacementResult
    
    /**
     * 清理UI替换
     */
    fun cleanup(context: UIReplacementContext): Boolean
    
    /**
     * 获取策略优先级（数字越小优先级越高）
     */
    fun getPriority(): Int
}

/**
 * UI替换结果
 */
sealed class ReplacementResult {
    /**
     * 成功
     */
    object Success : ReplacementResult()
    
    /**
     * 失败
     */
    data class Failed(
        val error: Throwable,
        val fallbackStrategy: UIReplacementStrategy.Type? = null
    ) : ReplacementResult()
    
    /**
     * 跳过
     */
    object Skipped : ReplacementResult()
}