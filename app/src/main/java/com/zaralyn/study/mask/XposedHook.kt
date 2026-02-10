package com.zaralyn.study.mask

import android.content.Intent
import android.content.SharedPreferences
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
            // Hook Activity 的 onCreate 方法，拦截所有 Activity 的启动
            XposedHelpers.findAndHookMethod(
                android.app.Activity::class.java.name,
                lpparam.classLoader,
                "onCreate",
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as android.app.Activity
                        val activityClassName = activity.javaClass.name

                        XposedBridge.log("ZaralynStudyMask: Activity onCreate - $activityClassName")

                        // 检查是否应该显示原应用
                        val prefs = activity.getSharedPreferences(PREFS_NAME, 0)
                        val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)

                        if (showOriginal) {
                            // 显示原应用，不做任何修改
                            return
                        }

                        // 如果是目标应用的 MainActivity，启动我们的伪装界面
                        if (activityClassName.endsWith(".MainActivity")) {
                            XposedBridge.log("ZaralynStudyMask: Launching ZaralynMainActivity")

                            val intent = Intent(activity, ZaralynMainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            activity.startActivity(intent)
                            activity.finish()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("ZaralynStudyMask: Hook failed - ${e.message}")
            e.printStackTrace()
        }
    }
}