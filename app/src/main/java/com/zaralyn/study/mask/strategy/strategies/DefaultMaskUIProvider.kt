package com.zaralyn.study.mask.strategy.strategies

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zaralyn.study.mask.R
import com.zaralyn.study.mask.core.ModuleClassLoaderManager
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.logger.LogLevel

/**
 * 默认的伪装UI提供者
 * 使用纯代码创建UI，避免ClassLoader隔离问题
 */
class DefaultMaskUIProvider : MaskUIProvider {

    private val logger = Logger.create("DefaultMaskUIProvider", LogLevel.DEBUG)

    override fun createMaskUI(activity: Activity): View {
        val ctx = ModuleClassLoaderManager.getModuleContext()

        // 创建根容器
        val rootLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F3F3F3"))
        }

        // 使用ContextThemeWrapper包装Context，为Material组件提供AppCompat主题
        val themedContext = ContextThemeWrapper(ctx, R.style.Theme_ZaralynStudyMask)

        // 创建 AppBarLayout（使用themedContext）
        val appBarLayout = AppBarLayout(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                288 // 72dp * 4 (density = 4)
            )
            setBackgroundColor(Color.parseColor("#1E88E5"))
            elevation = 4f
        }

        // 创建 Toolbar（使用themedContext）
        val toolbar = MaterialToolbar(themedContext).apply {
            layoutParams = AppBarLayout.LayoutParams(
                AppBarLayout.LayoutParams.MATCH_PARENT,
                AppBarLayout.LayoutParams.MATCH_PARENT
            )
            elevation = 4f
            setTitle(R.string.app_name)
            setTitleTextColor(Color.WHITE)
            setNavigationIcon(R.drawable.ic_menu_book)
            setNavigationIconTint(Color.WHITE)
        }

        appBarLayout.addView(toolbar)

        // 创建 Fragment 容器
        val fragmentContainer = FrameLayout(ctx).apply {
            id = R.id.fragmentContainer
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        // 创建底部导航栏（使用themedContext）
        val bottomNav = BottomNavigationView(themedContext).apply {
            id = R.id.bottomNavigationView
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                224 // 56dp * 4 (density = 4)
            )
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            inflateMenu(R.menu.bottom_nav_menu)
            // 使用ColorStateList设置图标和文字颜色
            val colors = ContextCompat.getColorStateList(themedContext, R.color.bottom_nav_color)
            setItemIconTintList(colors)
            setItemTextColor(colors)
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(activity, "com.zaralyn.study.mask.ui.fragments.HomeFragment")
                    true
                }
                R.id.navigation_explore -> {
                    loadFragment(activity, "com.zaralyn.study.mask.ui.fragments.ExploreFragment")
                    true
                }
                R.id.navigation_settings -> {
                    loadFragment(activity, com.zaralyn.study.mask.ui.fragments.SettingsFragment::class.java.name)
                    true
                }
                else -> false
            }
        }

        // 添加所有视图到根容器
        rootLayout.addView(appBarLayout)
        rootLayout.addView(fragmentContainer)
        rootLayout.addView(bottomNav)

        // 默认加载主页
        loadFragment(activity, "com.zaralyn.study.mask.ui.fragments.HomeFragment")

        return rootLayout
    }

    /**
     * 加载 Fragment
     * 由于我们不在 Activity 中，需要使用反射来创建 Fragment 实例
     */
    private fun loadFragment(activity: Activity, fragmentClassName: String) {
        try {
            // 检查是否是FragmentActivity
            if (activity !is androidx.fragment.app.FragmentActivity) {
                logger.warn("Activity is not FragmentActivity, cannot load fragment")
                return
            }

            // 关键修复：使用模块ClassLoader加载Fragment类
            val moduleClassLoader = ModuleClassLoaderManager.getModuleClassLoader()
            val fragmentClass = moduleClassLoader.loadClass(fragmentClassName)

            // 创建Fragment实例
            val fragment = fragmentClass.newInstance()

            // 安全的类型转换
            if (fragment !is androidx.fragment.app.Fragment) {
                logger.warn("Created object is not a Fragment")
                return
            }

            // 获取 FragmentManager
            val fragmentManager = activity.supportFragmentManager

            fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()

            logger.info("Fragment loaded successfully: $fragmentClassName")
        } catch (e: ClassNotFoundException) {
            logger.error("Fragment class not found: $fragmentClassName - ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to load fragment: ${e.message}", e)
        }
    }
}