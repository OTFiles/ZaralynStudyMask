package com.zaralyn.study.mask.core.config

import com.zaralyn.study.mask.detector.AppType
import com.zaralyn.study.mask.strategy.UIReplacementStrategy

/**
 * 策略配置
 * 定义应用类型与UI替换策略的映射关系
 */
object StrategyConfig {
    
    /**
     * 应用类型到UI替换策略的映射
     */
    private val strategyMap = mapOf(
        AppType.STANDARD to listOf(
            UIReplacementStrategy.Type.WINDOW_MANAGER_OVERLAY,
            UIReplacementStrategy.Type.DECOR_VIEW_MODIFICATION,
            UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER
        ),
        AppType.NATIVE to listOf(
            UIReplacementStrategy.Type.WINDOW_MANAGER_OVERLAY,
            UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER
        ),
        AppType.BROWSER to listOf(
            UIReplacementStrategy.Type.WINDOW_MANAGER_OVERLAY
        ),
        AppType.GAME to listOf(
            UIReplacementStrategy.Type.WINDOW_MANAGER_OVERLAY
        ),
        AppType.WEBVIEW to listOf(
            UIReplacementStrategy.Type.WINDOW_MANAGER_OVERLAY,
            UIReplacementStrategy.Type.DECOR_VIEW_MODIFICATION
        )
    )
    
    /**
     * 获取指定应用类型的UI替换策略列表
     */
    fun getStrategiesForAppType(appType: AppType): List<UIReplacementStrategy.Type> {
        return strategyMap[appType] ?: strategyMap[AppType.STANDARD]!!
    }
    
    /**
     * 获取默认策略
     */
    fun getDefaultStrategy(): UIReplacementStrategy.Type {
        return UIReplacementStrategy.Type.WINDOW_MANAGER_OVERLAY
    }
}