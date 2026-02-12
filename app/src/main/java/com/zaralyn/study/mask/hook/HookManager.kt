package com.zaralyn.study.mask.hook

import com.zaralyn.study.mask.core.config.HookConfig
import com.zaralyn.study.mask.detector.AppTypeDetector
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.state.StateManager
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Hook管理器
 * 负责管理所有Hook策略的安装和执行
 */
class HookManager(
    val lpparam: XC_LoadPackage.LoadPackageParam,
    private val appTypeDetector: AppTypeDetector,
    private val stateManager: StateManager,
    private val logger: Logger
) {
    
    val packageName: String = lpparam.packageName
    val classLoader: ClassLoader = lpparam.classLoader
    
    private val strategies: MutableList<HookStrategy> = mutableListOf()
    
    init {
        // 初始化所有Hook策略
        initializeStrategies()
    }
    
    /**
     * 初始化Hook策略
     */
    private fun initializeStrategies() {
        val configStrategies = HookConfig.getEnabledStrategies()
        
        // 注意：具体的Hook策略实现将在后续任务中创建
        // 这里只创建占位符
        configStrategies.forEach { config ->
            logger.info("Hook strategy configured: ${config.name} (priority: ${config.priority})")
        }
        
        logger.info("Initialized ${configStrategies.size} hook strategies")
    }
    
    /**
     * 安装所有Hook
     */
    fun installHooks() {
        var successCount = 0
        var failCount = 0
        
        strategies.forEach { strategy ->
            try {
                val context = HookContext.create(
                    lpparam = lpparam,
                    appType = appTypeDetector.detectAppType(),
                    isMainActivity = false, // 将在Hook执行时确定
                    mainActivityClass = appTypeDetector.getMainActivityClass(),
                    intent = null, // 将在Hook执行时获取
                    prefs = stateManager.getSharedPreferences(),
                    logger = logger
                )
                
                if (strategy.install(context)) {
                    successCount++
                    logger.info("Installed hook: ${strategy.getName()}")
                } else {
                    failCount++
                    logger.warn("Failed to install hook: ${strategy.getName()}")
                }
            } catch (e: Exception) {
                failCount++
                logger.error("Error installing hook ${strategy.getName()}: ${e.message}", e)
            }
        }
        
        logger.info("Hook installation complete: $successCount succeeded, $failCount failed")
    }
    
    /**
     * 添加Hook策略
     */
    fun addStrategy(strategy: HookStrategy) {
        strategies.add(strategy)
    }
    
    /**
     * 获取已安装的Hook策略
     */
    fun getStrategies(): List<HookStrategy> {
        return strategies.toList()
    }
    
    /**
     * 按优先级获取Hook策略
     */
    fun getStrategiesByPriority(): List<HookStrategy> {
        return strategies.sortedBy { it.getPriority() }
    }
}