package com.zaralyn.study.mask.event.handlers

import android.app.Activity
import android.view.KeyEvent
import com.zaralyn.study.mask.core.constants.Constants
import com.zaralyn.study.mask.event.EventHandler
import com.zaralyn.study.mask.logger.Logger

/**
 * 5次点击处理器
 */
class FiveTapHandler(
    private val logger: Logger = Logger.create("FiveTapHandler")
) : EventHandler {

    private var tapCount = 0
    private var lastTapTime = 0L

    override fun handleKeyEvent(activity: Activity, event: KeyEvent): Boolean {
        // 检测主页键
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                return handleHomeKeyPress(activity)
            }
        }

        return false
    }

    override fun handleTapEvent(activity: Activity): Boolean {
        return handleHomeKeyPress(activity)
    }

    private fun handleHomeKeyPress(activity: Activity): Boolean {
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

            // 启动原应用
            val intent = activity.intent
            intent.putExtra("show_original_app", true)
            activity.recreate()

            return true
        }

        return false
    }

    fun resetTapCount() {
        tapCount = 0
        lastTapTime = 0L
    }
}