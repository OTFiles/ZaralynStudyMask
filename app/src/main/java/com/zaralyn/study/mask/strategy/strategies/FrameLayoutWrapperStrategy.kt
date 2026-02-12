package com.zaralyn.study.mask.strategy.strategies

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.strategy.ReplacementResult
import com.zaralyn.study.mask.strategy.UIReplacementContext
import com.zaralyn.study.mask.strategy.UIReplacementStrategy
import java.lang.ref.WeakReference

/**
 * FrameLayout包装策略
 *
 * 创建FrameLayout作为新的根容器，保存并重新添加原有子视图，在顶层添加覆盖视图。
 * 这种方法保留了原应用的所有视图，只是在最上层添加覆盖层。
 *
 * 适用场景：
 * - DecorView修改策略失败后的fallback方案
 * - 需要保留原应用视图但隐藏其显示的场景
 * - 标准Activity（非NativeActivity）
 *
 * 优先级：30（最低优先级，作为最后的fallback方案）
 *
 * 工作原理：
 * 1. 创建FrameLayout作为新的容器
 * 2. 保存ContentView中所有原有子视图
 * 3. 移除ContentView中的所有子视图
 * 4. 将FrameLayout添加到ContentView
 * 5. 将原有子视图添加到FrameLayout（在底层）
 * 6. 在FrameLayout顶层添加覆盖视图
 *
 * 注意事项：
 * - 保留了原应用视图，但被覆盖视图遮挡
 * - 原应用的所有引用仍然有效，不会崩溃
 * - 可能会有原应用UI闪烁的问题（覆盖前短暂显示）
 * - 不适用于NativeActivity
 */
class FrameLayoutWrapperStrategy(
    private val maskUIProvider: MaskUIProvider
) : UIReplacementStrategy {

    companion object {
        private const val TAG = "FrameLayoutWrapperStrategy"
        
        // 用于保存FrameLayout的引用，以便后续刷新和清理
        private var frameLayoutRef: WeakReference<FrameLayout>? = null
    }

    override fun getName(): String = "FrameLayoutWrapperStrategy"

    override fun getType(): UIReplacementStrategy.Type = UIReplacementStrategy.Type.FRAME_LAYOUT_WRAPPER

    override fun getPriority(): Int = 30

    override fun canApply(context: UIReplacementContext): Boolean {
        val activity = context.activity

        // 检查Activity状态
        if (!context.isActivityValid()) {
            return false
        }

        // 检查是否是NativeActivity（不适用于此策略）
        val isNativeActivity = isNativeActivity(activity)
        if (isNativeActivity) {
            context.logger.debug("Activity is NativeActivity, FrameLayout wrapper not recommended")
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

        logger.debug("Applying FrameLayout wrapper strategy for ${activity.javaClass.name}")

        try {
            // 检查Activity状态
            if (!context.isActivityValid()) {
                logger.warn("Activity is finishing or destroyed, cannot apply FrameLayout wrapper")
                return ReplacementResult.Failed(
                    IllegalStateException("Activity is not valid"),
                    null
                )
            }

            // 获取DecorView和ContentView
            val decorView = activity.window.decorView
            val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)

            if (contentView == null) {
                logger.error("ContentView not found, FrameLayout wrapper failed")
                return ReplacementResult.Failed(
                    NullPointerException("ContentView not found"),
                    null
                )
            }

            logger.debug("ContentView found with ${contentView.childCount} children")

            // 创建FrameLayout作为新的容器
            val frameLayout = FrameLayout(activity)
            
            // 保存原有的所有子视图
            val children = ArrayList<View>()
            for (i in 0 until contentView.childCount) {
                children.add(contentView.getChildAt(i))
            }
            
            logger.debug("Saved ${children.size} original views from ContentView")

            // 移除ContentView中的所有子视图
            try {
                contentView.removeAllViews()
                logger.debug("All original views removed from ContentView")
            } catch (e: Exception) {
                logger.error("Failed to remove views from ContentView: ${e.message}", e)
                return ReplacementResult.Failed(
                    e,
                    null
                )
            }

            // 将FrameLayout添加到ContentView
            try {
                contentView.addView(frameLayout, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))
                logger.debug("FrameLayout added to ContentView")
                
                // 保存FrameLayout引用
                frameLayoutRef = WeakReference(frameLayout)
            } catch (e: Exception) {
                logger.error("Failed to add FrameLayout to ContentView: ${e.message}", e)
                return ReplacementResult.Failed(
                    e,
                    null
                )
            }

            // 将原有的子视图添加到FrameLayout（在底层）
            try {
                for (child in children) {
                    frameLayout.addView(child)
                }
                logger.debug("All original views added back to FrameLayout (bottom layer)")
            } catch (e: Exception) {
                logger.error("Failed to add original views to FrameLayout: ${e.message}", e)
                return ReplacementResult.Failed(
                    e,
                    null
                )
            }

            // 创建并添加覆盖视图（在顶层）
            val overlayView = maskUIProvider.createMaskUI(activity)
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            try {
                frameLayout.addView(overlayView, layoutParams)
                logger.info("FrameLayout wrapper created successfully, overlay view added on top")
                return ReplacementResult.Success
            } catch (e: Exception) {
                logger.error("Failed to add overlay view to FrameLayout: ${e.message}", e)
                return ReplacementResult.Failed(
                    e,
                    null
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to replace UI via FrameLayout wrapper: ${e.message}", e)
            return ReplacementResult.Failed(
                e,
                null
            )
        }
    }

    override fun cleanup(context: UIReplacementContext): Boolean {
        val logger = context.logger
        val activity = context.activity

        logger.debug("Cleaning up FrameLayout wrapper")

        try {
            // 获取DecorView和ContentView
            val decorView = activity.window.decorView
            val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)

            if (contentView != null && contentView.childCount > 0) {
                // 检查第一个子视图是否是FrameLayout
                val firstChild = contentView.getChildAt(0)
                if (firstChild is FrameLayout) {
                    // 将FrameLayout中的所有子视图移回ContentView
                    val children = ArrayList<View>()
                    for (i in 0 until firstChild.childCount) {
                        children.add(firstChild.getChildAt(i))
                    }
                    
                    contentView.removeAllViews()
                    for (child in children) {
                        contentView.addView(child)
                    }
                    
                    logger.info("FrameLayout wrapper removed, original views restored")
                } else {
                    logger.debug("First child is not FrameLayout, no cleanup needed")
                }
            } else {
                logger.debug("ContentView is empty or not found")
            }
            
            // 清除引用
            frameLayoutRef?.clear()
            return true
        } catch (e: Exception) {
            logger.error("Failed to cleanup FrameLayout wrapper: ${e.message}", e)
            return false
        }
    }

    /**
     * 获取当前FrameLayout的引用
     */
    fun getFrameLayout(): FrameLayout? = frameLayoutRef?.get()

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