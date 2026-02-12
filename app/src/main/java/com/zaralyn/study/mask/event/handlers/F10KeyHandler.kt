package com.zaralyn.study.mask.event.handlers

import android.app.Activity
import android.view.KeyEvent
import com.zaralyn.study.mask.event.EventHandler
import com.zaralyn.study.mask.logger.Logger

/**
 * F10键处理器
 */
class F10KeyHandler(
    private val logger: Logger = Logger.create("F10KeyHandler")
) : EventHandler {

    override fun handleKeyEvent(activity: Activity, event: KeyEvent): Boolean {
        // 检测F10键
        if (event.keyCode == KeyEvent.KEYCODE_F10) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                logger.info("F10 key detected, launching original app")

                // 启动原应用
                val intent = activity.intent
                intent.putExtra("show_original_app", true)
                activity.recreate()

                return true
            }
        }

        return false
    }

    override fun handleTapEvent(activity: Activity): Boolean {
        return false
    }
}