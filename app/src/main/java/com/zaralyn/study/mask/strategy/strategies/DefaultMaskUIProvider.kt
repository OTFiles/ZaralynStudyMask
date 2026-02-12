package com.zaralyn.study.mask.strategy.strategies

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.zaralyn.study.mask.R
import com.zaralyn.study.mask.core.constants.Constants
import com.zaralyn.study.mask.databinding.ActivityMainBinding
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.logger.LogLevel

/**
 * 默认的伪装UI提供者
 * 使用 ZaralynMainActivity 的布局作为伪装界面
 */
class DefaultMaskUIProvider : MaskUIProvider {

    // 缓存模块Context，避免重复创建
    private var moduleContext: Context? = null

    private val logger = Logger.create("DefaultMaskUIProvider", LogLevel.DEBUG)

    override fun createMaskUI(activity: Activity): View {
        // 获取模块的Context
        val ctx = getModuleContext(activity)

        // 使用模块的Context创建LayoutInflater
        val inflater = LayoutInflater.from(ctx)

        // 使用模块的Context创建DataBinding
        val binding = ActivityMainBinding.inflate(
            inflater,
            null,
            false
        )

        // 设置 Toolbar
        binding.toolbar.setTitle(R.string.app_name)

        // 设置底部导航
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
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
                    loadFragment(activity, "com.zaralyn.study.mask.ui.fragments.SettingsFragment")
                    true
                }
                else -> false
            }
        }

        // 默认加载主页
        loadFragment(activity, "com.zaralyn.study.mask.ui.fragments.HomeFragment")

        return binding.root
    }

    /**
     * 获取模块的Context
     * 使用 createPackageContext 创建模块的Context，用于访问模块资源
     */
    private fun getModuleContext(activity: Activity): Context {
        // 缓存模块Context
        if (moduleContext == null) {
            try {
                // 创建模块的Context，忽略安全检查
                moduleContext = activity.createPackageContext(
                    Constants.PACKAGE_NAME,
                    Context.CONTEXT_IGNORE_SECURITY
                )
            } catch (e: Exception) {
                logger.error("Failed to create module context: ${e.message}", e)
                throw RuntimeException("Failed to create module context: ${e.message}", e)
            }
        }
        return moduleContext!!
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

            val fragmentClass = Class.forName(fragmentClassName)
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
        } catch (e: Exception) {
            logger.error("Failed to load fragment: ${e.message}", e)
        }
    }
}