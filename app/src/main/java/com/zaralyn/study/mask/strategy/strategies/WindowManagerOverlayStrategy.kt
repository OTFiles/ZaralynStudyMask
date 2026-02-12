package com.zaralyn.study.mask.strategy.strategies

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.strategy.ReplacementResult
import com.zaralyn.study.mask.strategy.UIReplacementContext
import com.zaralyn.study.mask.strategy.UIReplacementStrategy
import java.lang.ref.WeakReference

/**
 * WindowManager覆盖层策略
 *
 * 通过WindowManager添加一个全屏覆盖View，在原应用UI之上显示伪装界面。
 * 这种方法不改变原有的视图层级结构，避免异步代码引用失效。
 *
 * 适用场景：
 * - NativeActivity（游戏、复杂应用）
 * - 原应用UI包含异步加载或复杂动画
 * - 需要保持原应用UI结构完整的场景
 *
 * 优先级：10（中等优先级，兼容性好但不适用于所有情况）
 */
class WindowManagerOverlayStrategy(
    private val maskUIProvider: MaskUIProvider
) : UIReplacementStrategy {

    companion object {
        private const val TAG = "WindowManagerOverlayStrategy"
        
        // 用于保存WindowManager overlay的引用，以便后续刷新和清理
        private val overlayViewRef = WeakReference<View>(null)
    }

    override fun getName(): String = "WindowManagerOverlayStrategy"

    override fun getType(): UIReplacementStrategy.Type = UIReplacementStrategy.Type.WINDOW_MANAGER_OVERLAY

    override fun getPriority(): Int = 10

    override fun canApply(context: UIReplacementContext): Boolean {
        // WindowManager覆盖层策略适用于所有Activity
        // 特别是NativeActivity和需要保持原视图结构的场景
        return context.isActivityValid()
    }

    override fun replaceUI(context: UIReplacementContext): ReplacementResult {
        val logger = context.logger
        val activity = context.activity

        logger.debug("Applying WindowManager overlay strategy for ${activity.javaClass.name}")

        try {
            // 检查Activity状态
            if (!context.isActivityValid()) {
                logger.warn("Activity is finishing or destroyed, cannot apply overlay")
                return ReplacementResult.Failed(
                    IllegalStateException("Activity is not valid"),
                    UIReplacementStrategy.Type.DECOR_VIEW_MODIFICATION
                )
            }

            // 检查是否已经有覆盖层
            if (overlayViewRef.get() != null) {
                logger.debug("Overlay already exists, skipping duplicate replacement")
                return ReplacementResult.Skipped
            }

            // 创建伪装UI
            val overlayView = maskUIProvider.createMaskUI(activity)

            // 保存overlayView的引用，以便后续刷新
            overlayViewRef = WeakReference(overlayView)

            // 获取WindowManager
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // 设置全屏参数
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.token = activity.window.decorView.windowToken

            // 添加覆盖View
            try {
                windowManager.addView(overlayView, params)
                logger.info("Overlay view added successfully")
                return ReplacementResult.Success
            } catch (e: WindowManager.BadTokenException) {
                logger.error("Failed to add overlay: BadTokenException - ${e.message}", e)
                return ReplacementResult.Failed(
                    e,
                    UIReplacementStrategy.Type.DECOR_VIEW_MODIFICATION
                )
            } catch (e: SecurityException) {
                logger.error("Failed to add overlay: SecurityException - ${e.message}", e)
                return ReplacementResult.Failed(
                    e,
                    UIReplacementStrategy.Type.DECOR_VIEW_MODIFICATION
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to replace UI via WindowManager overlay: ${e.message}", e)
            return ReplacementResult.Failed(
                e,
                UIReplacementStrategy.Type.DECOR_VIEW_MODIFICATION
            )
        }
    }

    override fun cleanup(context: UIReplacementContext): Boolean {
        val logger = context.logger
        val activity = context.activity

        logger.debug("Cleaning up WindowManager overlay")

        try {
            val overlayView = overlayViewRef.get()

            if (overlayView != null && activity != null) {
                val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                try {
                    windowManager.removeView(overlayView)
                    logger.info("Overlay view removed successfully")
                    overlayViewRef.clear()
                    return true
                } catch (e: Exception) {
                    logger.error("Failed to remove overlay: ${e.message}", e)
                    return false
                }
            } else {
                logger.debug("No overlay to remove")
                overlayViewRef.clear()
                return true
            }
        } catch (e: Exception) {
            logger.error("Failed to cleanup WindowManager overlay: ${e.message}", e)
            return false
        }
    }

    /**
     * 获取当前覆盖视图的引用
     */
    fun getOverlayView(): View? = overlayViewRef.get()

    /**
     * 刷新覆盖视图（用于年级切换等需要更新UI的场景）
     */
    fun refreshOverlay(activity: Activity, newOverlayView: View): Boolean {
        try {
            val oldOverlayView = overlayViewRef.get()
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            if (oldOverlayView != null) {
                windowManager.removeView(oldOverlayView)
            }

            overlayViewRef = WeakReference(newOverlayView)
            windowManager.addView(newOverlayView, oldOverlayView?.layoutParams as? WindowManager.LayoutParams)

            return true
        } catch (e: Exception) {
            return false
        }
    }
}

/**
 * 伪装UI提供者接口
 * 用于解耦策略和UI创建逻辑
 */
interface MaskUIProvider {
    /**
     * 创建伪装UI
     */
    fun createMaskUI(activity: Activity): View
}