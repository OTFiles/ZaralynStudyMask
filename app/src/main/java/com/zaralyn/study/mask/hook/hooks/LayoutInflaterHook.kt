package com.zaralyn.study.mask.hook.hooks

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.zaralyn.study.mask.hook.HookContext
import com.zaralyn.study.mask.hook.HookStrategy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.XC_MethodHookParam
import de.robv.android.xposed.XposedHelpers

/**
 * LayoutInflater Hook 策略
 * 
 * 拦截 LayoutInflater.inflate 方法，仅用于日志记录
 * 不执行 UI 替换操作
 * 
 * 优先级：5（最低）
 */
class LayoutInflaterHook : HookStrategy {

    companion object {
        private const val TAG = "LayoutInflaterHook"
    }

    override fun getName(): String = "LayoutInflaterHook"

    override fun getPriority(): Int = 5

    override fun install(context: HookContext): Boolean {
        return try {
            context.logger.debug("Installing LayoutInflater.inflate hook")
            
            XposedHelpers.findAndHookMethod(
                "android.view.LayoutInflater",
                context.classLoader,
                "inflate",
                Int::class.javaPrimitiveType,
                ViewGroup::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        handleLayoutInflaterHook(param, context)
                    }
                }
            )
            
            context.logger.info("Hooked LayoutInflater.inflate")
            true
        } catch (e: Exception) {
            context.logger.error("Failed to hook LayoutInflater: ${e.message}", e)
            false
        }
    }

    /**
     * 处理 LayoutInflater Hook
     * 仅记录日志，不执行 UI 替换
     */
    private fun handleLayoutInflaterHook(param: XC_MethodHookParam, context: HookContext) {
        try {
            // 获取 LayoutInflater 的 Context
            val inflaterContext = try {
                XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
            } catch (e: Exception) {
                context.logger.verbose("Failed to get mContext from LayoutInflater: ${e.message}")
                null
            }

            if (inflaterContext !is Activity) {
                return
            }

            val activity = inflaterContext
            val activityClassName = activity.javaClass.name
            val resource = param.args[0]

            // 只记录日志，不替换 UI
            context.logger.debug("LayoutInflater.inflate called: $activityClassName, resource: $resource")

            // 更新 HookContext 的 isMainActivity
            val updatedContext = context.copy(isMainActivity = isMainActivity(activity, context))

            // 记录是否是主 Activity
            if (shouldHandleActivity(updatedContext)) {
                context.logger.debug("LayoutInflater.inflate for main activity: $activityClassName")
            }

        } catch (e: Exception) {
            context.logger.error("Error in LayoutInflater hook: ${e.message}", e)
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