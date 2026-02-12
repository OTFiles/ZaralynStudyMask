package com.zaralyn.study.mask.event

import android.app.Activity
import android.content.SharedPreferences
import android.view.KeyEvent
import com.zaralyn.study.mask.core.constants.Constants
import com.zaralyn.study.mask.logger.Logger
import de.robv.android.xposed.XposedHelpers
import java.lang.ref.WeakReference

/**
 * 事件管理器
 * 管理所有事件处理器
 */
class EventManager(
    private val prefs: SharedPreferences,
    private val logger: Logger = Logger.create("EventManager")
) {

    // 点击计数
    private var tapCount = 0
    private var lastTapTime = 0L

    // Overlay视图引用
    private var overlayViewRef: WeakReference<android.view.View>? = null

    /**
     * 设置overlay视图引用
     */
    fun setOverlayView(view: android.view.View?) {
        overlayViewRef = WeakReference(view)
    }

    /**
     * 处理按键事件
     */
    fun handleKeyEvent(activity: Activity, event: KeyEvent): Boolean {
        // 检测F10键或MENU键
        if (event.keyCode == KeyEvent.KEYCODE_F10 || event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                logger.info("F10/MENU key detected, launching original app")
                launchOriginalApp(activity)
                return true
            }
        }

        return false
    }

    /**
     * 处理主页键点击事件
     */
    fun handleHomeKeyPress(): Boolean {
        val currentTime = System.currentTimeMillis()

        // 检查是否在检测窗口内
        if (currentTime - lastTapTime > Constants.TAP_DETECTION_WINDOW_MS) {
            tapCount = 0
        }

        tapCount++
        lastTapTime = currentTime

        logger.debug("Home button tap count: $tapCount")

        // 检查是否达到所需点击次数
        if (tapCount >= Constants.TAP_COUNT_REQUIRED) {
            logger.info("5 home button taps detected, launching original app")
            tapCount = 0
            return true
        }

        return false
    }

    /**
     * 启动原应用
     */
    private fun launchOriginalApp(activity: Activity) {
        try {
            val intent = activity.intent ?: return

            // 设置显示原应用标志
            intent.putExtra(Constants.EXTRA_SHOW_ORIGINAL, true)

            // 保存到SharedPreferences
            prefs.edit().putBoolean(Constants.KEY_SHOW_ORIGINAL, true).apply()

            // 重新启动Activity
            activity.recreate()

            logger.info("Original app launched")
        } catch (e: Exception) {
            logger.error("Failed to launch original app: ${e.message}", e)
        }
    }

    /**
     * 重置点击计数
     */
    fun resetTapCount() {
        tapCount = 0
        lastTapTime = 0L
    }

    /**
     * 获取当前点击计数
     */
    fun getTapCount(): Int {
        return tapCount
    }
}