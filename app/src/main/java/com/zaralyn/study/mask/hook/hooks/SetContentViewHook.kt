package com.zaralyn.study.mask.hook.hooks

import android.app.Activity
import android.view.View
import com.zaralyn.study.mask.hook.HookContext
import com.zaralyn.study.mask.hook.HookStrategy
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers

/**
 * setContentView Hook 策略
 * 
 * 拦截 Activity.setContentView 方法，在设置内容视图时进行 UI 替换
 * 支持两个重载方法：setContentView(int) 和 setContentView(View)
 * 
 * 使用 ThreadLocal 标志防止递归调用
 * 
 * 优先级：2
 */
class SetContentViewHook(
    private val onSetContentView: (Activity, HookContext) -> Unit
) : HookStrategy {

    companion object {
        private const val TAG = "SetContentViewHook"
        
        /**
         * ThreadLocal 标志，防止递归调用
         */
        private val isReplacingUI = ThreadLocal<Boolean>()
    }

    override fun getName(): String = "SetContentViewHook"

    override fun getPriority(): Int = 2

    override fun install(context: HookContext): Boolean {
        return try {
            // Hook setContentView(int)
            installSetContentViewInt(context)
            
            // Hook setContentView(View)
            installSetContentViewView(context)
            
            context.logger.info("Successfully hooked both setContentView methods")
            true
        } catch (e: Exception) {
            context.logger.error("Failed to hook setContentView: ${e.message}", e)
            false
        }
    }

    /**
     * Hook setContentView(int) 方法
     */
    private fun installSetContentViewInt(context: HookContext) {
        context.logger.debug("Installing setContentView(int) hook")
        
        XposedHelpers.findAndHookMethod(
            Activity::class.java.name,
            context.classLoader,
            "setContentView",
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    handleSetContentViewHook(param, context, "setContentView(int)")
                }
            }
        )
        
        context.logger.debug("Hooked setContentView(int)")
    }

    /**
     * Hook setContentView(View) 方法
     */
    private fun installSetContentViewView(context: HookContext) {
        context.logger.debug("Installing setContentView(View) hook")
        
        XposedHelpers.findAndHookMethod(
            Activity::class.java.name,
            context.classLoader,
            "setContentView",
            View::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    handleSetContentViewHook(param, context, "setContentView(View)")
                }
            }
        )
        
        context.logger.debug("Hooked setContentView(View)")
    }

    /**
     * 处理 setContentView Hook
     */
    private fun handleSetContentViewHook(param: MethodHookParam, context: HookContext, methodName: String) {
        try {
            // 检查是否正在替换 UI，防止递归
            if (isReplacingUI.get() == true) {
                context.logger.verbose("Already replacing UI, skipping $methodName")
                return
            }

            val activity = param.thisObject as? Activity ?: run {
                context.logger.verbose("thisObject is not an Activity in $methodName")
                return
            }

            val activityClassName = activity.javaClass.name
            context.logger.debug("$methodName called: $activityClassName")

            // 更新 HookContext 的 isMainActivity
            val updatedContext = context.copy(isMainActivity = isMainActivity(activity, context))

            // 检查是否应该处理当前 Activity
            if (!shouldHandleActivity(updatedContext)) {
                context.logger.verbose("Not a main activity, skipping $methodName")
                return
            }

            // 检查是否应该显示原应用
            if (updatedContext.shouldShowOriginal()) {
                context.logger.info("Should show original app, skipping $methodName")
                return
            }

            context.logger.info("Replacing UI via $methodName hook")

            // 调用回调函数
            onSetContentView(activity, updatedContext)

            // 阻止原始方法的执行
            param.setResult(null)

        } catch (e: Exception) {
            context.logger.error("Error in $methodName hook: ${e.message}", e)
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

    /**
     * 设置正在替换 UI 的标志
     */
    fun setReplacingUI(replacing: Boolean) {
        isReplacingUI.set(replacing)
    }

    /**
     * 获取是否正在替换 UI
     */
    fun isReplacingUI(): Boolean {
        return isReplacingUI.get() == true
    }

    override fun getCallback(): XC_MethodHook {
        // 此方法不直接使用，实际的回调在 install 方法中创建
        return object : XC_MethodHook() {}
    }
}