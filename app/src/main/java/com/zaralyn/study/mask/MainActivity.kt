package com.zaralyn.study.mask

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zaralyn.study.mask.databinding.ActivityMainBinding
import com.zaralyn.study.mask.ui.fragments.HomeFragment
import com.zaralyn.study.mask.ui.fragments.ExploreFragment
import com.zaralyn.study.mask.ui.fragments.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var homeClickCount = 0
    private var lastClickTime = 0L
    private val CLICK_THRESHOLD = 2000 // 2秒内点击有效
    private val REQUIRED_CLICKS = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_explore -> {
                    loadFragment(ExploreFragment())
                    true
                }
                R.id.navigation_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        // 默认加载主页
        loadFragment(HomeFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                androidx.appcompat.R.anim.abc_fade_in,
                androidx.appcompat.R.anim.abc_fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 处理 F10 键
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_F10) {
            launchOriginalApp()
            return true
        }

        // 处理主页键连击
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < CLICK_THRESHOLD) {
                homeClickCount++
                if (homeClickCount >= REQUIRED_CLICKS) {
                    launchOriginalApp()
                    homeClickCount = 0
                }
            } else {
                homeClickCount = 1
            }
            lastClickTime = currentTime
            return false
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun launchOriginalApp() {
        try {
            // 获取原应用的包名和主Activity类名
            // 这里需要根据实际情况配置
            val packageName = packageName.removeSuffix(".mask")
            val className = "$packageName.MainActivity"

            val intent = Intent().setClassName(packageName, className)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果找不到原应用，显示提示
            android.widget.Toast.makeText(this, "未找到原应用", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}