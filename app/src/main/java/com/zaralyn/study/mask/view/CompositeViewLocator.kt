package com.zaralyn.study.mask.view

import android.app.Activity
import android.view.View

/**
 * 组合视图定位器
 * 支持多种查找方式
 */
class CompositeViewLocator(
    private val locators: List<ViewLocator>
) : ViewLocator {

    override fun findView(activity: Activity, viewId: Int): View? {
        return locators.firstNotNullOfOrNull { it.findView(activity, viewId) }
    }

    override fun findViewByIdRecursive(root: View, viewId: Int): View? {
        return locators.firstNotNullOfOrNull { it.findViewByIdRecursive(root, viewId) }
    }
}