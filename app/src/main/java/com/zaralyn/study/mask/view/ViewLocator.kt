package com.zaralyn.study.mask.view

import android.app.Activity
import android.view.View

/**
 * 视图定位器接口
 */
interface ViewLocator {
    /**
     * 查找视图
     */
    fun findView(activity: Activity, viewId: Int): View?

    /**
     * 递归查找视图
     */
    fun findViewByIdRecursive(root: View, viewId: Int): View?
}