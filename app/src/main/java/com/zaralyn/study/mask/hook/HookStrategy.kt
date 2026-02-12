package com.zaralyn.study.mask.hook

import de.robv.android.xposed.XC_MethodHook

/**
 * Hook策略接口
 * 所有具体的Hook实现都必须实现此接口
 */
interface HookStrategy {
    
    /**
     * 策略名称
     */
    fun getName(): String
    
    /**
     * 策略优先级（数字越小优先级越高）
     */
    fun getPriority(): Int
    
    /**
     * 安装Hook
     * @param context Hook上下文
     * @return true表示安装成功，false表示安装失败
     */
    fun install(context: HookContext): Boolean
    
    /**
     * 卸载Hook（可选）
     */
    fun uninstall(context: HookContext): Boolean {
        // 默认不支持卸载
        return false
    }
    
    /**
     * 检查是否应该处理当前Activity
     */
    fun shouldHandleActivity(context: HookContext): Boolean {
        return context.isMainActivity
    }
    
    /**
     * 获取Hook回调
     */
    fun getCallback(): XC_MethodHook
}