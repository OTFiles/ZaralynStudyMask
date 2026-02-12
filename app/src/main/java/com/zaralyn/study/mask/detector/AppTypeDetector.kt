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
     * 使用多种兼容性检测方法，避免因字段不存在导致崩溃
     */
    private fun isNativeActivity(): Boolean {
        try {
            // 方法1: 检查主Activity类名
            val mainActivityClass = getMainActivityClass()
            if (mainActivityClass != null) {
                if (mainActivityClass == "android.app.NativeActivity" ||
                    mainActivityClass.contains("NativeActivity")) {
                    logger.debug("Detected NativeActivity via class name: $mainActivityClass")
                    return true
                }
            }

            // 方法2: 检查NativeActivity的子类
            try {
                val nativeActivityClass = XposedHelpers.findClass("android.app.NativeActivity", classLoader)
                if (nativeActivityClass != null && mainActivityClass != null) {
                    val mainActivityClassObj = try {
                        XposedHelpers.findClass(mainActivityClass, classLoader)
                    } catch (e: Exception) {
                        null
                    }
                    if (mainActivityClassObj != null && nativeActivityClass.isAssignableFrom(mainActivityClassObj)) {
                        logger.debug("Detected NativeActivity subclass: $mainActivityClass")
                        return true
                    }
                }
            } catch (e: Exception) {
                logger.debug("NativeActivity class not found: ${e.message}")
            }

            // 方法3: 检查是否有Native库加载（兼容性改进）
            try {
                val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
                val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
                val packages = XposedHelpers.getObjectField(currentActivityThread, "mPackages") as? Map<*, *>

                packages?.values?.forEach { pkg ->
                    try {
                        // 尝试多种字段名，提高兼容性
                        var loadedLibraries: Set<*>? = null

                        // 尝试字段名1: mLoadedLibraries
                        try {
                            loadedLibraries = XposedHelpers.getObjectField(pkg, "mLoadedLibraries") as? Set<*>
                        } catch (e: NoSuchFieldError) {
                            logger.debug("Field mLoadedLibraries not found, trying alternatives")
                        }

                        // 尝试字段名2: mLibraryPath
                        if (loadedLibraries == null) {
                            try {
                                val libraryPath = XposedHelpers.getObjectField(pkg, "mLibraryPath") as? String
                                if (!libraryPath.isNullOrEmpty()) {
                                    logger.debug("Found library path: $libraryPath")
                                    return true
                                }
                            } catch (e: NoSuchFieldError) {
                                logger.debug("Field mLibraryPath not found")
                            }
                        }

                        // 尝试字段名3: mNativeLibraryDir
                        if (loadedLibraries == null) {
                            try {
                                val nativeLibDir = XposedHelpers.getObjectField(pkg, "mNativeLibraryDir") as? String
                                if (!nativeLibDir.isNullOrEmpty()) {
                                    logger.debug("Found native library dir: $nativeLibDir")
                                    return true
                                }
                            } catch (e: NoSuchFieldError) {
                                logger.debug("Field mNativeLibraryDir not found")
                            }
                        }

                        if (!loadedLibraries.isNullOrEmpty()) {
                            logger.debug("Found native libraries: $loadedLibraries")
                            return true
                        }
                    } catch (e: Throwable) {
                        // 捕获所有异常，包括 Error
                        logger.debug("Error checking native libraries: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                logger.debug("Error checking ActivityThread: ${e.message}")
            }

        } catch (e: Throwable) {
            // 捕获所有异常，包括 Error
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