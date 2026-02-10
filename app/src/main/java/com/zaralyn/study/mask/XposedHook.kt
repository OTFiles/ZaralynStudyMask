package com.zaralyn.study.mask

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
        private const val KEY_UI_REPLACED = "ui_replaced"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName

        XposedBridge.log("ZaralynStudyMask: Loading hook for $packageName")

        try {
            // Hook setContentView 方法，阻止原 Activity 设置 UI
            XposedHelpers.findAndHookMethod(
                android.app.Activity::class.java.name,
                lpparam.classLoader,
                "setContentView",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as android.app.Activity
                        val activityClassName = activity.javaClass.name

                        XposedBridge.log("ZaralynStudyMask: setContentView called for $activityClassName")

                        // 只处理 MainActivity
                        if (!activityClassName.endsWith(".MainActivity")) {
                            return
                        }

                        // 检查是否应该显示原应用
                        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                        val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)

                        if (showOriginal) {
                            XposedBridge.log("ZaralynStudyMask: Showing original app")
                            return
                        }

                        if (uiReplaced) {
                            // UI 已经被替换过了，不再重复
                            return
                        }

                        XposedBridge.log("ZaralynStudyMask: Replacing UI for MainActivity")

                        // 标记 UI 已被替换
                        prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()

                        // 创建并设置伪装界面的 UI
                        val newUI = createMaskUI(activity, prefs)
                        activity.setContentView(newUI)

                        // 取消原来的 setContentView 调用
                        param.setResult(null)
                    }
                }
            )

            // Hook setContentView 方法（View 参数版本）
            XposedHelpers.findAndHookMethod(
                android.app.Activity::class.java.name,
                lpparam.classLoader,
                "setContentView",
                android.view.View::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as android.app.Activity
                        val activityClassName = activity.javaClass.name

                        XposedBridge.log("ZaralynStudyMask: setContentView(View) called for $activityClassName")

                        // 只处理 MainActivity
                        if (!activityClassName.endsWith(".MainActivity")) {
                            return
                        }

                        // 检查是否应该显示原应用
                        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                        val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)

                        if (showOriginal || uiReplaced) {
                            return
                        }

                        XposedBridge.log("ZaralynStudyMask: Replacing UI for MainActivity (View version)")

                        // 标记 UI 已被替换
                        prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()

                        // 创建并设置伪装界面的 UI
                        val newUI = createMaskUI(activity, prefs)
                        activity.setContentView(newUI)

                        // 取消原来的 setContentView 调用
                        param.setResult(null)
                    }
                }
            )

            // Hook Activity.onWindowFocusChanged 来处理按键事件
            XposedHelpers.findAndHookMethod(
                android.app.Activity::class.java.name,
                lpparam.classLoader,
                "onWindowFocusChanged",
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as android.app.Activity
                        val activityClassName = activity.javaClass.name

                        // 只处理 MainActivity
                        if (!activityClassName.endsWith(".MainActivity")) {
                            return
                        }

                        // 检查是否应该显示原应用
                        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                        val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)

                        if (showOriginal || !uiReplaced) {
                            return
                        }

                        // 设置按键监听
                        setupKeyListener(activity, prefs)
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("ZaralynStudyMask: Hook failed - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createMaskUI(activity: android.app.Activity, prefs: android.content.SharedPreferences): View {
        // 创建根布局
        val rootLayout = LinearLayout(activity)
        rootLayout.orientation = LinearLayout.VERTICAL
        
        // 创建工具栏
        val toolbar = createToolbar(activity)
        rootLayout.addView(toolbar)
        
        // 创建内容区域
        val scrollView = ScrollView(activity)
        val contentLayout = createContentLayout(activity)
        scrollView.addView(contentLayout)
        
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ))
        
        // 添加底部导航
        val bottomNav = createBottomNavigation(activity)
        rootLayout.addView(bottomNav)
        
        return rootLayout
    }
    
    private fun createToolbar(activity: android.app.Activity): LinearLayout {
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
        
        val hint = TextView(activity)
        hint.text = "按 F10 或连击主页键 5 次"
        hint.setTextColor(Color.parseColor("#BBDEFB"))
        hint.textSize = 12f
        
        toolbar.addView(hint)
        
        return toolbar
    }
    
    private fun createContentLayout(activity: android.app.Activity): LinearLayout {
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.WHITE)
        layout.setPadding(16, 16, 16, 16)
        
        // 年级选择
        layout.addView(createSectionTitle(activity, "选择年级"))
        layout.addView(createGradeRow(activity))
        
        // 课本列表
        layout.addView(createSectionTitle(activity, "课本列表"))
        layout.addView(createBookList(activity))
        
        return layout
    }
    
    private fun createSectionTitle(activity: android.app.Activity, text: String): TextView {
        val title = TextView(activity)
        title.text = text
        title.textSize = 18f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setPadding(0, 0, 0, 8)
        return title
    }
    
    private fun createGradeRow(activity: android.app.Activity): LinearLayout {
        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        
        val grades = listOf("高一", "高二", "高三")
        for (grade in grades) {
            row.addView(createGradeCard(activity, grade))
        }
        
        return row
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
    
    private fun createBookList(activity: android.app.Activity): LinearLayout {
        val list = LinearLayout(activity)
        list.orientation = LinearLayout.VERTICAL
        
        val books = listOf("必修一", "必修二", "英语语法", "词汇手册")
        for (book in books) {
            list.addView(createBookCard(activity, book))
        }
        
        return list
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
    
    private fun setupKeyListener(activity: android.app.Activity, prefs: android.content.SharedPreferences) {
        val decorView = activity.window.decorView
        
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
        prefs.edit()
            .putBoolean(KEY_SHOW_ORIGINAL, true)
            .putBoolean(KEY_UI_REPLACED, false)
            .apply()
        
        // 重新启动 Activity
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
    }
}