package com.zaralyn.study.mask.strategy.strategies

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.strategy.ReplacementResult
import com.zaralyn.study.mask.strategy.UIReplacementContext
import com.zaralyn.study.mask.strategy.UIReplacementStrategy

/**
 * DecorView修改策略
 *
 * 直接修改DecorView的ContentView，移除所有原有子视图并添加新的覆盖视图。
 * 这种方法直接操作视图层级，能够完全替换原应用UI。
 *
 * 适用场景：
 * - WindowManager覆盖层策略失败后的fallback方案
 * - 标准Activity（非NativeActivity）
 * - 需要完全隐藏原应用UI的场景
 *
 * 优先级：20（较低优先级，作为fallback方案）
 *
 * 注意事项：
 * - 会移除所有原应用的子视图，可能导致原应用的异步代码引用失效
 * - 不适用于NativeActivity
 * - 某些应用可能有自己的ContentView管理逻辑，导致替换失败
 */
class DecorViewModificationStrategy(
    private val maskUIProvider: MaskUIProvider
) : UIReplacementStrategy {

    companion object {
        private const val TAG = "DecorViewModificationStrategy"
    }

    override fun getName(): String = "DecorViewModificationStrategy"

    override fun getType(): UIReplacementStrategy.Type = UIReplacementStrategy.Type.DECOR_VIEW_MODIFICATION

    override fun getPriority(): Int = 20

    override fun canApply(context: UIReplacementContext): Boolean {
        val activity = context.activity

        // 检查Activity状态
        if (!context.isActivityValid()) {
            return false
        }

        // 检查是否是NativeActivity（不适用于此策略）
        val isNativeActivity = isNativeActivity(activity)
        if (isNativeActivity) {
            context.logger.debug("Activity is NativeActivity, DecorView modification not recommended")
            return false
        }

        // 检查ContentView是否存在
        try {
            val decorView = activity.window.decorView
            val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)
            return contentView != null
        } catch (e: Exception) {
            context.logger.warn("Failed to check ContentView availability: ${e.message}")
            return false
        }
    }

    override fun replaceUI(context: UIReplacementContext): ReplacementResult {
        val logger = context.logger
        val activity = context.activity

        logger.debug("Applying DecorView modification strategy for ${activity.javaClass.name}")

        try {
            // 检查Activity状态
            if (!context.isActivityValid()) {
                logger.warn("Activity is finishing or destroyed, cannot modify DecorView")
                return ReplacementResult.Failed(
                    IllegalStateException("Activity is not valid"),
                    UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER
                )
            }

            // 获取DecorView和ContentView
            val decorView = activity.window.decorView
            val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)

            if (contentView == null) {
                logger.error("ContentView not found, DecorView modification failed")
                return ReplacementResult.Failed(
                    NullPointerException("ContentView not found"),
                    UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER
                )
            }

            logger.debug("ContentView found with ${contentView.childCount} children")

            // 移除所有子视图
            try {
                contentView.removeAllViews()
                logger.debug("All original views removed from ContentView")
            } catch (e: Exception) {
                logger.error("Failed to remove views from ContentView: ${e.message}", e)
                return ReplacementResult.Failed(
                    e,
                    UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER
                )
            }

            // 添加伪装UI
            val overlayView = maskUIProvider.createMaskUI(activity)

            try {
                contentView.addView(overlayView, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))
                logger.info("DecorView modified successfully, overlay view added")
                return ReplacementResult.Success
            } catch (e: Exception) {
                logger.error("Failed to add overlay view to ContentView: ${e.message}", e)
                return ReplacementResult.Failed(
                    e,
                    UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to replace UI via DecorView modification: ${e.message}", e)
            return ReplacementResult.Failed(
                e,
                UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER
            )
        }
    }

    override fun cleanup(context: UIReplacementContext): Boolean {
        val logger = context.logger
        val activity = context.activity

        logger.debug("Cleaning up DecorView modification")

        try {
            // 获取DecorView和ContentView
            val decorView = activity.window.decorView
            val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)

            if (contentView != null && contentView.childCount > 0) {
                // 移除覆盖视图
                contentView.removeAllViews()
                logger.info("All views removed from ContentView")
                return true
            } else {
                logger.debug("No views to remove from ContentView")
                return true
            }
        } catch (e: Exception) {
            logger.error("Failed to cleanup DecorView modification: ${e.message}", e)
            return false
        }
    }

    /**
     * 检查Activity是否是NativeActivity
     */
    private fun isNativeActivity(activity: Activity): Boolean {
        val className = activity.javaClass.name
        val superClassName = activity.javaClass.superclass?.name

        return className == "android.app.NativeActivity" ||
               superClassName == "android.app.NativeActivity" ||
               className.contains("Native") ||
               hasNativeContentView(activity)
    }

    /**
     * 检查Activity是否包含NativeContentView
     */
    private fun hasNativeContentView(activity: Activity): Boolean {
        return try {
            val decorView = activity.window.decorView
            val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)

            if (contentView != null && contentView.childCount > 0) {
                val originalView = contentView.getChildAt(0)
                originalView != null && originalView.javaClass.name.contains("NativeContentView")
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}