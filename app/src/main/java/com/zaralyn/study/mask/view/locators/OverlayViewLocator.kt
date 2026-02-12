package com.zaralyn.study.mask.view.locators

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.zaralyn.study.mask.view.ViewLocator
import com.zaralyn.study.mask.core.constants.Constants

/**
 * Overlay视图定位器
 * 从WindowManager overlay中查找视图
 */
class OverlayViewLocator : ViewLocator {

    private var overlayView: View? = null

    fun setOverlayView(view: View?) {
        overlayView = view
    }

    override fun findView(activity: Activity, viewId: Int): View? {
        return overlayView?.findViewById(viewId)
    }

    override fun findViewByIdRecursive(root: View, viewId: Int): View? {
        if (root.id == viewId) {
            return root
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val found = findViewByIdRecursive(child, viewId)
                if (found != null) {
                    return found
                }
            }
        }

        return null
    }
}