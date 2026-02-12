package com.zaralyn.study.mask.event

import android.app.Activity
import android.view.KeyEvent

/**
 * 事件处理器接口
 */
interface EventHandler {
    /**
     * 处理按键事件
     */
    fun handleKeyEvent(activity: Activity, event: KeyEvent): Boolean
    
    /**
     * 处理点击事件
     */
    fun handleTapEvent(activity: Activity): Boolean
}