package com.zaralyn.study.mask.detector

import android.app.Activity
import android.content.pm.PackageManager
import com.zaralyn.study.mask.logger.Logger
import de.robv.android.xposed.XposedHelpers

/**
 * 应用类型检测器
 * 根据应用特征检测应用类型
 */
class AppTypeDetector(
    private val classLoader: ClassLoader,
    private val packageName: String,
    private val logger: Logger = Logger.create("AppTypeDetector")
) {

    private var cachedAppType: AppType? = null
    private var cachedMainActivityClass: String? = null

    /**
     * 检测应用类型
     */
    fun detectAppType(): AppType {
        cachedAppType?.let { return it }

        val appType = when {
            // 检测NativeActivity
            isNativeActivity() -> AppType.NATIVE

            // 检测浏览器
            isBrowserApp() -> AppType.BROWSER

            // 检测游戏
            isGameApp() -> AppType.GAME

            // 检测WebView应用
            isWebViewApp() -> AppType.WEBVIEW

            // 默认为标准应用
            else -> AppType.STANDARD
        }

        cachedAppType = appType
        logger.info("Detected app type: ${appType.displayName} for $packageName")

        return appType
    }

    /**
     * 检测是否是NativeActivity
     */
    private fun isNativeActivity(): Boolean {
        try {
            // 检查主Activity类名
            val mainActivityClass = getMainActivityClass()
            if (mainActivityClass != null) {
                if (mainActivityClass == "android.app.NativeActivity" ||
                    mainActivityClass.contains("NativeActivity")) {
                    return true
                }
            }

            // 检查是否有NativeContentView
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            val packages = XposedHelpers.getObjectField(currentActivityThread, "mPackages") as? Map<*, *>

            packages?.values?.forEach { pkg ->
                try {
                    val loadedLibraries = XposedHelpers.getObjectField(pkg, "mLoadedLibraries") as? Set<*>
                    if (!loadedLibraries.isNullOrEmpty()) {
                        logger.debug("Found native libraries: $loadedLibraries")
                        return true
                    }
                } catch (e: Exception) {
                    // 忽略错误
                }
            }

        } catch (e: Exception) {
            logger.debug("Error checking NativeActivity: ${e.message}")
        }

        return false
    }

    /**
     * 检测是否是浏览器应用
     */
    private fun isBrowserApp(): Boolean {
        val browserKeywords = listOf(
            "browser", "chrome", "firefox", "opera",
            "safari", "edge", "brave", "ucbrowser", "yujian"
        )

        return browserKeywords.any { keyword ->
            packageName.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 检测是否是游戏应用
     */
    private fun isGameApp(): Boolean {
        val gameKeywords = listOf(
            "game", "play", "minecraft", "gta", "pubg",
            "clash", "candy", "angry", "temple", "subway"
        )

        return gameKeywords.any { keyword ->
            packageName.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 检测是否是WebView应用
     */
    private fun isWebViewApp(): Boolean {
        try {
            val webViewClass = XposedHelpers.findClass("android.webkit.WebView", classLoader)
            return webViewClass != null
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 获取主Activity类名
     * 使用 PackageManager API 查询，兼容所有 Android 版本
     */
    fun getMainActivityClass(): String? {
        cachedMainActivityClass?.let { return it }

        try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")

            // 获取 Context
            var context: android.content.Context? = null

            // 尝试获取 mSystemContext
            try {
                val systemContext = XposedHelpers.getObjectField(currentActivityThread, "mSystemContext")
                if (systemContext is android.content.Context) {
                    context = systemContext
                }
            } catch (e: Exception) {
                logger.debug("Could not get mSystemContext: ${e.message}")
            }

            // 如果获取失败，尝试获取 mInitialApplication
            if (context == null) {
                try {
                    val initialApp = XposedHelpers.getObjectField(currentActivityThread, "mInitialApplication")
                    if (initialApp is android.content.Context) {
                        context = initialApp
                    }
                } catch (e: Exception) {
                    logger.debug("Could not get mInitialApplication: ${e.message}")
                }
            }

            if (context == null) {
                logger.debug("Could not get Context from ActivityThread")
                return null
            }

            // 方法1: PackageManager.getLaunchIntentForPackage()
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                val component = intent.component
                if (component != null) {
                    val className = component.className
                    logger.debug("Found main activity via PackageManager: $className")
                    cachedMainActivityClass = className
                    return className
                }
            }

            // 方法2: queryIntentActivities()
            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            mainIntent.`package` = packageName

            val resolveInfos = packageManager.queryIntentActivities(mainIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfos.isNotEmpty()) {
                val resolveInfo = resolveInfos[0]
                val className = resolveInfo.activityInfo.name
                logger.debug("Found main activity via queryIntentActivities: $className")
                cachedMainActivityClass = className
                return className
            }
        } catch (e: Exception) {
            logger.debug("Error getting main activity class: ${e.message}")
        }

        return null
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cachedAppType = null
        cachedMainActivityClass = null
    }
}