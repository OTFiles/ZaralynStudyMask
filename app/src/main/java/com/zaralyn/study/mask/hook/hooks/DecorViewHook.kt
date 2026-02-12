package com.zaralyn.study.mask.hook.hooks

import android.app.Activity
import android.content.Context
import com.zaralyn.study.mask.hook.HookContext
import com.zaralyn.study.mask.hook.HookStrategy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers

/**
 * DecorView Hook 策略
 * 
 * 拦截 View.dispatchAttachedToWindow 方法，在 DecorView 附加到窗口时进行 UI 替换
 * 使用延迟执行确保视图已完全附加
 * 
 * 优先级：4
 */
class DecorViewHook(
    private val onDecorViewAttached: (Activity, HookContext) -> Unit
) : HookStrategy {

    companion object {
        private const val TAG = "DecorViewHook"
    }

    override fun getName(): String = "DecorViewHook"

    override fun getPriority(): Int = 4

    override fun install(context: HookContext): Boolean {
        return try {
            context.logger.debug("Installing DecorView.dispatchAttachedToWindow hook")
            
            // 查找 AttachInfo 类
            val attachInfoClass = XposedHelpers.findClass("android.view.View\$AttachInfo", context.classLoader)
            
            // 查找 DecorView 类
            val decorViewClass = XposedHelpers.findClass("com.android.internal.policy.DecorView", context.classLoader)
            
            XposedHelpers.findAndHookMethod(
                "android.view.View",
                context.classLoader,
                "dispatchAttachedToWindow",
                attachInfoClass,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        handleDecorViewHook(param, context, decorViewClass)
                    }
                }
            )
            
            context.logger.info("Hooked DecorView.dispatchAttachedToWindow")
            true
        } catch (e: Exception) {
            context.logger.error("Failed to hook DecorView: ${e.message}", e)
            false
        }
    }

    /**
     * 处理 DecorView Hook
     */
    private fun handleDecorViewHook(param: MethodHookParam, context: HookContext, decorViewClass: Class<*>) {
        try {
            val view = param.thisObject

            // 检查是否是 DecorView，使用类对象的 isInstance 方法
            if (!decorViewClass.isInstance(view)) {
                return
            }

            // 获取 Activity
            val viewContext = try {
                XposedHelpers.callMethod(view, "getContext") as? Context
            } catch (e: Exception) {
                context.logger.verbose("Failed to get context from view: ${e.message}")
                null
            }

            if (viewContext !is Activity) {
                context.logger.verbose("Context is not an Activity, skipping DecorView hook")
                return
            }

            val activity = viewContext
            val activityClassName = activity.javaClass.name

            context.logger.debug("DecorView attached: $activityClassName")

            // 更新 HookContext 的 isMainActivity
            val updatedContext = context.copy(isMainActivity = isMainActivity(activity, context))

            // 检查是否应该处理当前 Activity
            if (!shouldHandleActivity(updatedContext)) {
                context.logger.verbose("Not a main activity, skipping DecorView hook")
                return
            }

            // 检查是否应该显示原应用
            if (updatedContext.shouldShowOriginal()) {
                context.logger.info("Should show original app, skipping DecorView hook")
                return
            }

            context.logger.info("Replacing UI via DecorView hook")

            // 延迟执行，确保视图已完全附加
            activity.window.decorView.post {
                try {
                    onDecorViewAttached(activity, updatedContext)
                } catch (e: Exception) {
                    context.logger.error("Error in onDecorViewAttached callback: ${e.message}", e)
                }
            }

        } catch (e: Exception) {
            context.logger.error("Error in DecorView hook: ${e.message}", e)
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