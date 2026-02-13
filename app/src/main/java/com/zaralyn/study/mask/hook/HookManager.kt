package com.zaralyn.study.mask.hook

import android.app.Activity
import android.content.Intent
import com.zaralyn.study.mask.core.ModuleClassLoaderManager
import com.zaralyn.study.mask.core.config.HookConfig
import com.zaralyn.study.mask.detector.AppType
import com.zaralyn.study.mask.detector.AppTypeDetector
import com.zaralyn.study.mask.hook.hooks.ActivityThreadHook
import com.zaralyn.study.mask.hook.hooks.DecorViewHook
import com.zaralyn.study.mask.hook.hooks.LayoutInflaterHook
import com.zaralyn.study.mask.hook.hooks.PhoneWindowHook
import com.zaralyn.study.mask.hook.hooks.SetContentViewHook
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.state.StateManager
import com.zaralyn.study.mask.strategy.StrategySelector
import com.zaralyn.study.mask.strategy.UIReplacementContext
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
        
        strategies.clear()
        
        configStrategies.forEach { config ->
            val strategy = when (config) {
                HookConfig.HookStrategy.ACTIVITY_THREAD ->
                    ActivityThreadHook { activity, intent, hookContext ->
                        handleMainActivityDetected(activity, intent, hookContext)
                    }
                HookConfig.HookStrategy.SET_CONTENT_VIEW ->
                    SetContentViewHook { activity, hookContext ->
                        handleUIReplacement(activity, hookContext)
                    }
                HookConfig.HookStrategy.PHONE_WINDOW ->
                    PhoneWindowHook { activity, hookContext ->
                        handleUIReplacement(activity, hookContext)
                    }
                HookConfig.HookStrategy.DECOR_VIEW ->
                    DecorViewHook { activity, hookContext ->
                        handleUIReplacement(activity, hookContext)
                    }
                HookConfig.HookStrategy.LAYOUT_INFLATER ->
                    LayoutInflaterHook()
            }
            strategies.add(strategy)
        }
        
        logger.info("Initialized ${strategies.size} hook strategies: ${strategies.map { it.getName() }}")
    }
    
    /**
     * 安装所有Hook
     */
    fun installHooks() {
        var successCount = 0
        var failCount = 0

        // 先获取应用类型，避免在循环中重复获取
        val appType = try {
            appTypeDetector.detectAppType()
        } catch (e: Throwable) {
            logger.error("Failed to detect app type: ${e.message}", e)
            logger.warn("Using default app type: STANDARD")
            AppType.STANDARD  // 使用默认类型
        }

        // 获取主Activity类名
        val mainActivityClass = try {
            appTypeDetector.getMainActivityClass()
        } catch (e: Throwable) {
            logger.warn("Failed to get main activity class: ${e.message}")
            null
        }

        strategies.forEach { strategy ->
            try {
                val context = HookContext.create(
                    lpparam = lpparam,
                    appType = appType,
                    isMainActivity = false, // 将在Hook执行时确定
                    mainActivityClass = mainActivityClass,
                    intent = null, // 将在Hook执行时获取
                    prefs = null, // 注意：在Hook安装阶段无法获取SharedPreferences，实际使用时在UI替换阶段通过Activity获取
                    logger = logger
                )
                
                if (strategy.install(context)) {
                    successCount++
                    logger.info("Installed hook: ${strategy.getName()}")
                } else {
                    failCount++
                    logger.warn("Failed to install hook: ${strategy.getName()}")
                }
            } catch (e: Throwable) {
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
    
    /**
     * 处理主Activity检测
     * 在ActivityThread Hook中使用，检测到主Activity后执行UI替换
     */
    private fun handleMainActivityDetected(activity: Activity, intent: Intent?, hookContext: HookContext) {
        try {
            logger.info("Main activity detected: ${activity.javaClass.name}")
            
            // 检查是否应该显示原应用
            if (hookContext.shouldShowOriginal()) {
                logger.info("Should show original app, skipping UI replacement")
                return
            }
            
            // 执行UI替换
            handleUIReplacement(activity, hookContext)
            
        } catch (e: Exception) {
            logger.error("Error handling main activity detection: ${e.message}", e)
        }
    }
    
    /**
     * 处理UI替换
     * 使用StrategySelector执行UI替换
     */
    private fun handleUIReplacement(activity: Activity, hookContext: HookContext) {
        try {
            logger.info("Replacing UI for ${activity.javaClass.name}")

            // 初始化ModuleClassLoaderManager（只在第一次时初始化）
            if (!ModuleClassLoaderManager.isReady()) {
                try {
                    ModuleClassLoaderManager.initialize(activity)
                    logger.info("ModuleClassLoaderManager initialized")
                } catch (e: Exception) {
                    logger.error("Failed to initialize ModuleClassLoaderManager: ${e.message}", e)
                    // 继续尝试，可能使用降级方案
                }
            }

            // 通过Activity获取SharedPreferences
            val prefs = try {
                activity.getSharedPreferences("zaralyn_mask_prefs", android.content.Context.MODE_PRIVATE)
            } catch (e: Exception) {
                logger.debug("Could not get SharedPreferences: ${e.message}")
                null
            }

            // 创建UI替换上下文
            val replacementContext = UIReplacementContext(
                activity = activity,
                appType = hookContext.appType,
                isMainActivity = hookContext.isMainActivity,
                prefs = prefs,
                logger = hookContext.logger
            )

            // 使用StrategySelector执行UI替换
            val result = StrategySelector.replaceUI(replacementContext)

            when (result) {
                is com.zaralyn.study.mask.strategy.ReplacementResult.Success -> {
                    logger.info("UI replacement succeeded for ${activity.javaClass.name}")
                }
                is com.zaralyn.study.mask.strategy.ReplacementResult.Failed -> {
                    logger.error("UI replacement failed for ${activity.javaClass.name}: ${result.error.message}")
                }
                is com.zaralyn.study.mask.strategy.ReplacementResult.Skipped -> {
                    logger.info("UI replacement skipped for ${activity.javaClass.name}")
                }
            }

        } catch (e: Exception) {
            logger.error("Error in handleUIReplacement: ${e.message}", e)
        }
    }
}