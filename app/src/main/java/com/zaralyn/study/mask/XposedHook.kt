package com.zaralyn.study.mask

import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 只 hook 目标应用（这里需要根据实际情况配置包名）
        val targetPackage = "com.target.app"
        if (lpparam.packageName != targetPackage) {
            return
        }

        XposedBridge.log("ZaralynStudyMask: Loading hook for $targetPackage")

        try {
            // Hook 目标应用的 MainActivity.onCreate 方法
            XposedHelpers.findAndHookMethod(
                "com.target.app.MainActivity",
                lpparam.classLoader,
                "onCreate",
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as android.app.Activity

                        XposedBridge.log("ZaralynStudyMask: MainActivity onCreate hooked")

                        // 启动伪装界面
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        activity.startActivity(intent)
                        activity.finish()
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("ZaralynStudyMask: Hook failed - ${e.message}")
            e.printStackTrace()
        }
    }
}