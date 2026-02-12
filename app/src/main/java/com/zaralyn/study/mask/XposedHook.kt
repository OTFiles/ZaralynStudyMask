package com.zaralyn.study.mask

import com.zaralyn.study.mask.core.constants.Constants
import com.zaralyn.study.mask.detector.AppTypeDetector
import com.zaralyn.study.mask.hook.HookManager
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.logger.LogLevel
import com.zaralyn.study.mask.state.StateManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_LoadPackage.LoadPackageParam

/**
 * ZaralynStudyMask Xposed模块入口
 *
 * 该模块为Android应用添加"高中英语学习"的伪装界面
 * 用户必须通过特定操作（连击5次主页键或按F10键）才能进入原应用
 */
class XposedHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = Constants.TAG
        private val logger = Logger.create(TAG, LogLevel.DEBUG)

        // 模块实例缓存
        private val hookManagers = mutableMapOf<String, HookManager>()
        private val stateManagers = mutableMapOf<String, StateManager>()
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        try {
            val packageName = lpparam.packageName

            // 过滤系统进程
            if (isSystemProcess(packageName)) {
                logger.debug("Skipping system process: $packageName")
                return
            }

            // 过滤模块自身
            if (packageName == Constants.PACKAGE_NAME) {
                logger.debug("Skipping self: $packageName")
                return
            }

            logger.info("========== ZaralynStudyMask: Loading hook for $packageName ==========")

            // 初始化状态管理器
            val stateManager = StateManager.getInstance(packageName, logger)
            stateManagers[packageName] = stateManager

            // 初始化应用类型检测器
            val appTypeDetector = AppTypeDetector(lpparam.classLoader, packageName, logger)

            // 初始化Hook管理器
            val hookManager = HookManager(lpparam, appTypeDetector, stateManager, logger)
            hookManagers[packageName] = hookManager

            // 安装所有Hook
            hookManager.installHooks()

            logger.info("All hooks installed successfully for $packageName")

        } catch (e: Exception) {
            logger.error("Failed to load hook for ${lpparam.packageName}: ${e.message}", e)
        }
    }

    /**
     * 检查是否是系统进程
     */
    private fun isSystemProcess(packageName: String): Boolean {
        // 过滤android.*开头的包名
        if (packageName.startsWith("android.")) {
            return true
        }

        // 过滤系统应用包名
        val systemPackages = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.mms",
            "de.robv.android.xposed.installer",
            "org.lsposed.manager",
            "org.lsposed.lspd"
        )

        return packageName in systemPackages
    }
}