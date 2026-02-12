package com.zaralyn.study.mask.core.config

/**
 * Hook配置
 * 定义哪些Hook策略应该被启用
 */
object HookConfig {
    
    /**
     * Hook策略配置
     */
    enum class HookStrategy(
        val enabled: Boolean,
        val priority: Int,
        val description: String
    ) {
        ACTIVITY_THREAD(true, 1, "ActivityThread.performLaunchActivity Hook"),
        SET_CONTENT_VIEW(true, 2, "Activity.setContentView Hook"),
        PHONE_WINDOW(true, 3, "PhoneWindow.setContentView Hook"),
        DECOR_VIEW(true, 4, "DecorView.dispatchAttachedToWindow Hook"),
        LAYOUT_INFLATER(false, 5, "LayoutInflater.inflate Hook (仅日志)")
    }
    
    /**
     * 获取已启用的Hook策略列表（按优先级排序）
     */
    fun getEnabledStrategies(): List<HookStrategy> {
        return HookStrategy.values()
            .filter { it.enabled }
            .sortedBy { it.priority }
    }
    
    /**
     * 检查指定策略是否启用
     */
    fun isStrategyEnabled(strategy: HookStrategy): Boolean {
        return strategy.enabled
    }
}