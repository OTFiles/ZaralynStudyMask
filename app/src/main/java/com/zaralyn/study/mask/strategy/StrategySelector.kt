package com.zaralyn.study.mask.strategy

import android.app.Activity
import android.content.SharedPreferences
import com.zaralyn.study.mask.core.config.StrategyConfig
import com.zaralyn.study.mask.detector.AppType
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.strategy.strategies.DecorViewModificationStrategy
import com.zaralyn.study.mask.strategy.strategies.FrameLayoutWrapperStrategy
import com.zaralyn.study.mask.strategy.strategies.WindowManagerOverlayStrategy
import de.robv.android.xposed.XposedBridge

/**
 * 策略选择器
 * 负责根据应用类型选择合适的UI替换策略
 */
object StrategySelector {
    
    // 策略实例缓存
    private val strategyInstances = mutableMapOf<UIReplacementStrategy.Type, UIReplacementStrategy>()
    
    /**
     * 替换UI
     * 根据应用类型选择策略并执行UI替换
     */
    fun replaceUI(context: UIReplacementContext): ReplacementResult {
        val appType = context.appType
        
        // 获取应用类型对应的策略列表
        val strategyTypes = StrategyConfig.getStrategiesForAppType(appType)
        
        context.logger.info("Attempting UI replacement for ${appType.displayName} with ${strategyTypes.size} strategies")
        
        // 按优先级尝试每个策略
        for (strategyType in strategyTypes) {
            val strategy = getStrategy(strategyType)
            
            context.logger.debug("Trying strategy: ${strategy.getName()} (priority: ${strategy.getPriority()})")
            
            // 检查是否可以应用此策略
            if (!strategy.canApply(context)) {
                context.logger.warn("Strategy ${strategy.getName()} cannot be applied, skipping")
                continue
            }
            
            // 执行UI替换
            val result = strategy.replaceUI(context)
            
            when (result) {
                is ReplacementResult.Success -> {
                    context.logger.info("UI replacement succeeded with strategy: ${strategy.getName()}")
                    return result
                }
                is ReplacementResult.Failed -> {
                    context.logger.error("UI replacement failed with strategy ${strategy.getName()}: ${result.error.message}")
                    
                    // 如果有fallback策略，继续尝试
                    if (result.fallbackStrategy != null) {
                        context.logger.info("Falling back to strategy: ${result.fallbackStrategy}")
                        val fallback = getStrategy(result.fallbackStrategy)
                        val fallbackResult = fallback.replaceUI(context)
                        if (fallbackResult is ReplacementResult.Success) {
                            return fallbackResult
                        }
                    }
                }
                is ReplacementResult.Skipped -> {
                    context.logger.debug("Strategy ${strategy.getName()} skipped")
                }
            }
        }
        
        // 所有策略都失败了
        context.logger.error("All UI replacement strategies failed for ${appType.displayName}")
        return ReplacementResult.Failed(
            error = Exception("All strategies failed"),
            fallbackStrategy = null
        )
    }
    
    /**
     * 获取策略实例
     */
    private fun getStrategy(type: UIReplacementStrategy.Type): UIReplacementStrategy {
        return strategyInstances.getOrPut(type) {
            createStrategy(type)
        }
    }
    
    /**
     * 创建策略实例
     */
    private fun createStrategy(type: UIReplacementStrategy.Type): UIReplacementStrategy {
        val maskUIProvider = com.zaralyn.study.mask.strategy.strategies.DefaultMaskUIProvider()

        return when (type) {
            UIReplacementStrategy.Type.WINDOW_MANAGER_OVERLAY ->
                WindowManagerOverlayStrategy(maskUIProvider)
            UIReplacementStrategy.Type.DECOR_VIEW_MODIFICATION ->
                DecorViewModificationStrategy(maskUIProvider)
            UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER ->
                FrameLayoutWrapperStrategy(maskUIProvider)
        }
    }
    
    /**
     * 清除策略实例缓存
     */
    fun clearCache() {
        strategyInstances.clear()
    }
}