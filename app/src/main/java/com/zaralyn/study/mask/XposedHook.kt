package com.zaralyn.study.mask

import android.app.Activity
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedHook : IXposedHookLoadPackage {

    companion object {
        private const val PREFS_NAME = "ZaralynStudyMask"
        private const val KEY_SHOW_ORIGINAL = "show_original_app"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName

        XposedBridge.log("ZaralynStudyMask: Loading hook for $packageName")

        try {
            // Hook 目标应用包名的 Activity.onCreate
            // 我们通过 hook Instrumentation.callActivityOnCreate 来拦截所有 Activity 的创建
            XposedHelpers.findAndHookMethod(
                "android.app.Instrumentation",
                lpparam.classLoader,
                "callActivityOnCreate",
                Activity::class.java,
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.args[0] as Activity
                        val activityClassName = activity.javaClass.name

                        XposedBridge.log("ZaralynStudyMask: callActivityOnCreate - $activityClassName")

                        // 只处理 MainActivity
                        if (!activityClassName.endsWith(".MainActivity")) {
                            return
                        }

                        // 检查是否应该显示原应用
                        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)

                        if (showOriginal) {
                            // 显示原应用，不做任何修改
                            return
                        }

                        XposedBridge.log("ZaralynStudyMask: Replacing MainActivity with ZaralynMainActivity")

                        // 设置标志，防止循环
                        prefs.edit().putBoolean("in_mask_mode", true).apply()

                        // 启动伪装界面
                        val intent = Intent()
                        intent.setClass(activity, Class.forName("com.zaralyn.study.mask.ZaralynMainActivity"))
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        activity.startActivity(intent)

                        // 结束原 Activity
                        activity.finish()
                        
                        // 取消原来的 onCreate 调用
                        param.setResult(null)
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("ZaralynStudyMask: Hook failed - ${e.message}")
            e.printStackTrace()
        }
    }
}