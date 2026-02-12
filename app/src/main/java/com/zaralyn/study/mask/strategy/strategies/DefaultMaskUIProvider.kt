package com.zaralyn.study.mask.strategy.strategies

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.zaralyn.study.mask.R
import com.zaralyn.study.mask.databinding.ActivityMainBinding

/**
 * 默认的伪装UI提供者
 * 使用 ZaralynMainActivity 的布局作为伪装界面
 */
class DefaultMaskUIProvider : MaskUIProvider {

    override fun createMaskUI(activity: Activity): View {
        // 使用 DataBinding 创建布局
        val binding = ActivityMainBinding.inflate(
            LayoutInflater.from(activity),
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
     * 加载 Fragment
     * 由于我们不在 Activity 中，需要使用反射来创建 Fragment 实例
     */
    private fun loadFragment(activity: Activity, fragmentClassName: String) {
        try {
            val fragmentClass = Class.forName(fragmentClassName)
            val fragment = fragmentClass.newInstance() as androidx.fragment.app.Fragment

            // 获取 FragmentManager
            val fragmentManager = (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager

            fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}