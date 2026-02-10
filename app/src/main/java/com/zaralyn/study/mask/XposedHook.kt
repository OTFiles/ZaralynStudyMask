package com.zaralyn.study.mask

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedHook : IXposedHookLoadPackage {

    companion object {
        private const val PREFS_NAME = "ZaralynStudyMask"
        private const val KEY_SHOW_ORIGINAL = "show_original_app"
        private const val KEY_CLICK_COUNT = "home_click_count"
        private const val KEY_LAST_CLICK_TIME = "last_click_time"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName

        XposedBridge.log("ZaralynStudyMask: Loading hook for $packageName")

        try {
            // Hook 目标应用包名的 Activity.onCreate
            XposedHelpers.findAndHookMethod(
                android.app.Activity::class.java.name,
                lpparam.classLoader,
                "onCreate",
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as android.app.Activity
                        val activityClassName = activity.javaClass.name

                        XposedBridge.log("ZaralynStudyMask: Activity onCreate - $activityClassName")

                        // 只处理 MainActivity
                        if (!activityClassName.endsWith(".MainActivity")) {
                            return
                        }

                        // 检查是否应该显示原应用
                        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)

                        if (showOriginal) {
                            // 显示原应用，不做任何修改
                            XposedBridge.log("ZaralynStudyMask: Showing original app")
                            return
                        }

                        XposedBridge.log("ZaralynStudyMask: Replacing MainActivity UI with study mask")

                        // 替换 Activity 的 UI
                        replaceActivityUI(activity, prefs)
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("ZaralynStudyMask: Hook failed - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun replaceActivityUI(activity: android.app.Activity, prefs: android.content.SharedPreferences) {
        // 创建伪装界面的根布局
        val rootLayout = LinearLayout(activity)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setBackgroundColor(Color.parseColor("#1E88E5"))
        
        // 创建工具栏
        val toolbar = createToolbar(activity, prefs)
        rootLayout.addView(toolbar)
        
        // 创建内容区域
        val contentLayout = LinearLayout(activity)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.setBackgroundColor(Color.parseColor("#FFFFFF"))
        
        val scrollView = ScrollView(activity)
        scrollView.addView(contentLayout)
        
        // 添加内容
        contentLayout.addView(createGradeSection(activity))
        contentLayout.addView(createBookSection(activity))
        
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ))
        
        // 添加底部导航
        val bottomNav = createBottomNavigation(activity)
        rootLayout.addView(bottomNav)
        
        // 设置为 Activity 的内容视图
        activity.setContentView(rootLayout)
        
        // 处理按键事件
        handleKeyPresses(activity, prefs)
    }
    
    private fun createToolbar(activity: android.app.Activity, prefs: android.content.SharedPreferences): LinearLayout {
        val toolbar = LinearLayout(activity)
        toolbar.orientation = LinearLayout.HORIZONTAL
        toolbar.setBackgroundColor(Color.parseColor("#1E88E5"))
        toolbar.setPadding(16, 16, 16, 16)
        
        val title = TextView(activity)
        title.text = "高中英语学习"
        title.setTextColor(Color.WHITE)
        title.textSize = 20f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        
        toolbar.addView(title, LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ))
        
        // 添加隐藏入口提示
        val hint = TextView(activity)
        hint.text = "按 F10 或连击主页键 5 次进入原应用"
        hint.setTextColor(Color.parseColor("#BBDEFB"))
        hint.textSize = 12f
        
        toolbar.addView(hint)
        
        return toolbar
    }
    
    private fun createGradeSection(activity: android.app.Activity): LinearLayout {
        val section = LinearLayout(activity)
        section.orientation = LinearLayout.VERTICAL
        section.setPadding(16, 16, 16, 16)
        
        val title = TextView(activity)
        title.text = "选择年级"
        title.textSize = 18f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setPadding(0, 0, 0, 8)
        
        section.addView(title)
        
        val gradesLayout = LinearLayout(activity)
        gradesLayout.orientation = LinearLayout.HORIZONTAL
        
        val grades = listOf("高一", "高二", "高三")
        for (grade in grades) {
            val gradeCard = createGradeCard(activity, grade)
            gradesLayout.addView(gradeCard)
        }
        
        section.addView(gradesLayout)
        
        return section
    }
    
    private fun createGradeCard(activity: android.app.Activity, gradeName: String): LinearLayout {
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.HORIZONTAL
        card.setBackgroundColor(Color.parseColor("#D1E4FF"))
        card.setPadding(16, 16, 16, 16)
        card.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        
        val icon = TextView(activity)
        icon.text = "📚"
        icon.textSize = 24f
        
        val name = TextView(activity)
        name.text = gradeName
        name.textSize = 16f
        name.setTypeface(null, android.graphics.Typeface.BOLD)
        name.setPadding(16, 0, 0, 0)
        
        card.addView(icon)
        card.addView(name)
        
        return card
    }
    
    private fun createBookSection(activity: android.app.Activity): LinearLayout {
        val section = LinearLayout(activity)
        section.orientation = LinearLayout.VERTICAL
        section.setPadding(16, 16, 16, 16)
        
        val title = TextView(activity)
        title.text = "课本列表"
        title.textSize = 18f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setPadding(0, 0, 0, 8)
        
        section.addView(title)
        
        val books = listOf("必修一", "必修二", "英语语法", "词汇手册")
        for (book in books) {
            section.addView(createBookCard(activity, book))
        }
        
        return section
    }
    
    private fun createBookCard(activity: android.app.Activity, bookName: String): LinearLayout {
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundColor(Color.parseColor("#F3F3F3"))
        card.setPadding(16, 16, 16, 16)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        val name = TextView(activity)
        name.text = bookName
        name.textSize = 16f
        name.setTypeface(null, android.graphics.Typeface.BOLD)
        
        val filename = TextView(activity)
        filename.text = "compulsory_1.md"
        filename.setTextColor(Color.parseColor("#74777F"))
        filename.textSize = 12f
        filename.setPadding(0, 8, 0, 0)
        
        card.addView(name)
        card.addView(filename)
        
        return card
    }
    
    private fun createBottomNavigation(activity: android.app.Activity): LinearLayout {
        val nav = LinearLayout(activity)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.setBackgroundColor(Color.parseColor("#D1E4FF"))
        
        val items = listOf("主页", "课外", "设置")
        for (item in items) {
            val navItem = TextView(activity)
            navItem.text = item
            navItem.gravity = android.view.Gravity.CENTER
            navItem.setPadding(0, 16, 0, 16)
            navItem.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            
            nav.addView(navItem)
        }
        
        return nav
    }
    
    private fun handleKeyPresses(activity: android.app.Activity, prefs: android.content.SharedPreferences) {
        val window = activity.window
        val decorView = window.decorView
        
        // 设置按键监听
        decorView.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                if (keyCode == android.view.KeyEvent.KEYCODE_F10 || 
                    keyCode == android.view.KeyEvent.KEYCODE_MENU) {
                    launchOriginalApp(activity, prefs)
                    return@setOnKeyListener true
                }
                
                if (keyCode == android.view.KeyEvent.KEYCODE_HOME) {
                    val currentTime = System.currentTimeMillis()
                    val lastClickTime = prefs.getLong(KEY_LAST_CLICK_TIME, 0)
                    val clickCount = prefs.getInt(KEY_CLICK_COUNT, 0)
                    
                    if (currentTime - lastClickTime < 2000) {
                        val newClickCount = clickCount + 1
                        prefs.edit()
                            .putInt(KEY_CLICK_COUNT, newClickCount)
                            .putLong(KEY_LAST_CLICK_TIME, currentTime)
                            .apply()
                        
                        XposedBridge.log("ZaralynStudyMask: Home click count: $newClickCount")
                        
                        if (newClickCount >= 5) {
                            launchOriginalApp(activity, prefs)
                            prefs.edit().putInt(KEY_CLICK_COUNT, 0).apply()
                        }
                    } else {
                        prefs.edit()
                            .putInt(KEY_CLICK_COUNT, 1)
                            .putLong(KEY_LAST_CLICK_TIME, currentTime)
                            .apply()
                    }
                }
            }
            false
        }
    }
    
    private fun launchOriginalApp(activity: android.app.Activity, prefs: android.content.SharedPreferences) {
        XposedBridge.log("ZaralynStudyMask: Launching original app")
        
        // 设置标志，让 XposedHook 知道应该显示原应用
        prefs.edit().putBoolean(KEY_SHOW_ORIGINAL, true).apply()
        
        // 重新启动 Activity
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
    }
}