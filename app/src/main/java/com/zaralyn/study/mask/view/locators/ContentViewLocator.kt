package com.zaralyn.study.mask.view.locators

import android.app.Activity
import android.view.View
import com.zaralyn.study.mask.view.ViewLocator

/**
 * ContentView视图定位器
 * 从Activity的ContentView中查找视图
 */
class ContentViewLocator : ViewLocator {

    override fun findView(activity: Activity, viewId: Int): View? {
        val contentView = activity.findViewById<View>(android.R.id.content)
        return contentView?.findViewById(viewId)
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