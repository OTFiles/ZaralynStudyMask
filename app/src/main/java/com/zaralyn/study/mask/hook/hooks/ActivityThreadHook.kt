package com.zaralyn.study.mask.hook.hooks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zaralyn.study.mask.hook.HookContext
import com.zaralyn.study.mask.hook.HookStrategy
import com.zaralyn.study.mask.logger.LogLevel
import com.zaralyn.study.mask.logger.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * ActivityThread Hook 策略
 * 
 * 拦截 ActivityThread.performLaunchActivity 方法，在 Activity 启动时进行 UI 替换
 * 支持 Android 10+ 和 Android 9 的双签名方法
 * 
 * 优先级：1（最高）
 */
class ActivityThreadHook(
    private val onMainActivityDetected: (Activity, Intent?, HookContext) -> Unit
) : HookStrategy {

    companion object {
        private const val TAG = "ActivityThreadHook"
    }

    override fun getName(): String = "ActivityThreadHook"

    override fun getPriority(): Int = 1

    override fun install(context: HookContext): Boolean {
        return try {
            // 首先尝试 Android 10+ 签名（2个参数）
            installForAndroid10Plus(context)
        } catch (e: NoSuchMethodError) {
            context.logger.warn("Android 10+ signature not found, trying legacy signature")
            // 回退到 Android 9 签名（4个参数）
            try {
                installForLegacy(context)
            } catch (e2: Exception) {
                context.logger.error("Failed to hook ActivityThread with legacy signature: ${e2.message}", e2)
                false
            }
        } catch (e: Exception) {
            context.logger.error("Failed to hook ActivityThread: ${e.message}", e)
            false
        }
    }

    /**
     * 安装 Android 10+ 签名的 Hook
     * performLaunchActivity(ActivityClientRecord, Intent)
     */
    private fun installForAndroid10Plus(context: HookContext): Boolean {
        context.logger.info("Installing ActivityThread hook for Android 10+ (2 params)")
        
        XposedHelpers.findAndHookMethod(
            "android.app.ActivityThread",
            context.classLoader,
            "performLaunchActivity",
            "android.app.ActivityThread\$ActivityClientRecord",
            Intent::class.java,
            createHookCallback(context)
        )
        
        context.logger.info("Hooked performLaunchActivity (Android 10+ signature)")
        return true
    }

    /**
     * 安装 Android 9 签名的 Hook
     * performLaunchActivity(ActivityClientRecord, Intent, String, Bundle)
     */
    private fun installForLegacy(context: HookContext): Boolean {
        context.logger.info("Installing ActivityThread hook for Android 9 (4 params)")
        
        XposedHelpers.findAndHookMethod(
            "android.app.ActivityThread",
            context.classLoader,
            "performLaunchActivity",
            "android.app.ActivityThread\$ActivityClientRecord",
            Intent::class.java,
            String::class.java,
            Bundle::class.java,
            createHookCallback(context)
        )
        
        context.logger.info("Hooked performLaunchActivity (legacy signature)")
        return true
    }

    /**
     * 创建 Hook 回调
     */
    private fun createHookCallback(context: HookContext): XC_MethodHook {
        return object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    // 检查返回值是否为有效的 Activity
                    if (param.result == null) {
                        context.logger.verbose("Activity result is null, skipping")
                        return
                    }

                    val activity = param.result as? Activity ?: run {
                        context.logger.verbose("Result is not an Activity, skipping")
                        return
                    }

                    val activityClassName = activity.javaClass.name
                    context.logger.debug("Activity launched: $activityClassName")

                    // 安全获取 Intent
                    val intent: Intent? = try {
                        if (param.args.size > 1) {
                            param.args[1] as? Intent
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        context.logger.warn("Failed to get intent from param.args: ${e.message}")
                        null
                    } ?: try {
                        // 如果 param.args 中的 Intent 为 null，尝试从 Activity 中获取
                        activity.intent
                    } catch (e: Exception) {
                        context.logger.warn("Failed to get intent from activity: ${e.message}")
                        null
                    }

                    // 记录 Intent 信息
                    if (intent != null) {
                        context.logger.debug("Intent action: ${intent.action}, flags: ${intent.flags}")
                        context.logger.debug("Intent categories: ${intent.categories?.joinToString()}")
                    } else {
                        context.logger.debug("Intent is null")
                    }

                    // 检查是否应该显示原应用
                    if (shouldShowOriginalApp(intent, context)) {
                        context.logger.info("Detected request to show original app, skipping UI replacement")
                        return
                    }

                    // 更新 HookContext 的 Intent
                    val updatedContext = context.copy(intent = intent)

                    // 检查是否是主 Activity
                    if (!isMainActivity(activity, intent, updatedContext)) {
                        context.logger.verbose("Not a main activity, skipping")
                        return
                    }

                    context.logger.info("=== MAIN ACTIVITY DETECTED: $activityClassName ===")

                    // 调用回调函数，通知检测到主 Activity
                    activity.window.decorView.post {
                        try {
                            onMainActivityDetected(activity, intent, updatedContext)
                        } catch (e: Exception) {
                            context.logger.error("Error in onMainActivityDetected callback: ${e.message}", e)
                        }
                    }

                } catch (e: Exception) {
                    context.logger.error("Error in performLaunchActivity hook: ${e.message}", e)
                }
            }
        }
    }

    /**
     * 检查是否应该显示原应用
     */
    private fun shouldShowOriginalApp(intent: Intent?, context: HookContext): Boolean {
        // 检查 Intent extras 中的 show_original_app 标志
        if (intent != null) {
            context.logger.verbose("Checking intent extras...")
            val showOriginalFromIntent = intent.getBooleanExtra("show_original_app", false)
            context.logger.verbose("Intent show_original_app flag: $showOriginalFromIntent")
            
            if (showOriginalFromIntent) {
                return true
            }

            // 检查 Intent flags
            val flags = intent.flags
            val isClearTask = (flags and Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0
            context.logger.verbose("Intent flags: $flags, isClearTask: $isClearTask")
        }

        return false
    }

    /**
     * 判断是否是主 Activity
     */
    private fun isMainActivity(activity: Activity, intent: Intent?, context: HookContext): Boolean {
        val activityClassName = activity.javaClass.name

        // 方法1: 缓存的类名
        if (context.mainActivityClass != null && activityClassName == context.mainActivityClass) {
            context.logger.debug("Main activity detected via cache: $activityClassName")
            return true
        }

        // 方法2: 类名以 MainActivity 结尾
        if (activityClassName.endsWith(".MainActivity")) {
            context.logger.debug("Main activity detected via class name: $activityClassName")
            return true
        }

        // 方法3: Intent 检查
        if (intent != null) {
            val isMainAction = intent.action == Intent.ACTION_MAIN
            val isLauncherCategory = intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true

            if (isMainAction && isLauncherCategory) {
                context.logger.debug("Main activity detected via Intent: $activityClassName")
                return true
            }
        }

        // 方法4: PackageManager 查询
        try {
            val packageManager = activity.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            mainIntent.`package` = context.packageName

            val resolveInfos = packageManager.queryIntentActivities(mainIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfos.isNotEmpty()) {
                val mainClassName = resolveInfos[0].activityInfo.name
                if (activityClassName == mainClassName) {
                    context.logger.debug("Main activity detected via PackageManager query: $activityClassName")
                    return true
                }
            }
        } catch (e: Exception) {
            context.logger.verbose("Error querying PackageManager: ${e.message}")
        }

        return false
    }

    override fun getCallback(): XC_MethodHook {
        // 此方法不直接使用，实际的回调在 install 方法中创建
        return object : XC_MethodHook() {}
    }
}