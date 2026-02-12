package com.zaralyn.study.mask.hook.hooks

import android.app.Activity
import com.zaralyn.study.mask.hook.HookContext
import com.zaralyn.study.mask.hook.HookStrategy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * PhoneWindow Hook 策略
 * 
 * 拦截 PhoneWindow.setContentView 方法，在窗口设置内容视图时进行 UI 替换
 * 通过 PhoneWindow 的 mActivity 字段获取 Activity 实例
 * 
 * 优先级：3
 */
class PhoneWindowHook(
    private val onPhoneWindowSetContent: (Activity, HookContext) -> Unit
) : HookStrategy {

    companion object {
        private const val TAG = "PhoneWindowHook"
    }

    override fun getName(): String = "PhoneWindowHook"

    override fun getPriority(): Int = 3

    override fun install(context: HookContext): Boolean {
        return try {
            context.logger.debug("Installing PhoneWindow.setContentView hook")
            
            XposedHelpers.findAndHookMethod(
                "com.android.internal.policy.PhoneWindow",
                context.classLoader,
                "setContentView",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        handlePhoneWindowHook(param, context)
                    }
                }
            )
            
            context.logger.info("Hooked PhoneWindow.setContentView")
            true
        } catch (e: Exception) {
            context.logger.error("Failed to hook PhoneWindow: ${e.message}", e)
            false
        }
    }

    /**
     * 处理 PhoneWindow Hook
     */
    private fun handlePhoneWindowHook(param: MethodHookParam, context: HookContext) {
        try {
            val phoneWindow = param.thisObject
            
            // 从 PhoneWindow 获取 Activity
            val activity = try {
                XposedHelpers.getObjectField(phoneWindow, "mActivity") as? Activity
            } catch (e: Exception) {
                context.logger.verbose("Failed to get mActivity from PhoneWindow: ${e.message}")
                null
            }

            if (activity == null) {
                context.logger.verbose("Activity is null in PhoneWindow hook, skipping")
                return
            }

            val activityClassName = activity.javaClass.name
            context.logger.debug("PhoneWindow.setContentView called: $activityClassName")

            // 更新 HookContext 的 isMainActivity
            val updatedContext = context.copy(isMainActivity = isMainActivity(activity, context))

            // 检查是否应该处理当前 Activity
            if (!shouldHandleActivity(updatedContext)) {
                context.logger.verbose("Not a main activity, skipping PhoneWindow hook")
                return
            }

            // 检查是否应该显示原应用
            if (updatedContext.shouldShowOriginal()) {
                context.logger.info("Should show original app, skipping PhoneWindow hook")
                return
            }

            context.logger.info("Replacing UI via PhoneWindow hook")

            // 调用回调函数
            onPhoneWindowSetContent(activity, updatedContext)

            // 阻止原始方法的执行
            param.setResult(null)

        } catch (e: Exception) {
            context.logger.error("Error in PhoneWindow hook: ${e.message}", e)
        }
    }

    /**
     * 判断是否是主 Activity
     */
    private fun isMainActivity(activity: Activity, context: HookContext): Boolean {
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

        // 方法3: PackageManager 查询
        try {
            val packageManager = activity.packageManager
            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
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