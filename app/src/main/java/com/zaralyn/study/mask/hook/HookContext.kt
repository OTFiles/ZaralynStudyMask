package com.zaralyn.study.mask.hook

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import com.zaralyn.study.mask.detector.AppType
import com.zaralyn.study.mask.logger.Logger
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Hook上下文
 * 封装Hook执行所需的所有上下文信息
 */
data class HookContext(
    val packageName: String,
    val classLoader: ClassLoader,
    val appType: AppType,
    val isMainActivity: Boolean,
    val mainActivityClass: String?,
    val intent: Intent?,
    val prefs: SharedPreferences?,  // 改为可选
    val logger: Logger
) {

    /**
     * 获取Activity实例（如果在afterHooked中可用）
     */
    fun getActivity(): Activity? {
        return null // 由具体的Hook实现填充
    }

    /**
     * 检查是否应该显示原应用
     */
    fun shouldShowOriginal(): Boolean {
        // 检查Intent extras
        if (intent != null) {
            if (intent.getBooleanExtra("show_original_app", false)) {
                return true
            }
        }

        // 检查SharedPreferences（如果可用）
        if (prefs != null && prefs.getBoolean("show_original_app", false)) {
            return true
        }

        return false
    }

    companion object {
        fun create(
            lpparam: XC_LoadPackage.LoadPackageParam,
            appType: AppType,
            isMainActivity: Boolean,
            mainActivityClass: String?,
            intent: Intent?,
            prefs: SharedPreferences?,  // 改为可选
            logger: Logger
        ): HookContext {
            return HookContext(
                packageName = lpparam.packageName,
                classLoader = lpparam.classLoader,
                appType = appType,
                isMainActivity = isMainActivity,
                mainActivityClass = mainActivityClass,
                intent = intent,
                prefs = prefs,
                logger = logger
            )
        }
    }
}