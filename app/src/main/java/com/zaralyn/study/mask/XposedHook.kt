package com.zaralyn.study.mask

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
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
        private const val KEY_MAIN_ACTIVITY = "main_activity"
    }

    private var mainActivityClass: String? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName

        XposedBridge.log("========== ZaralynStudyMask: Loading hook for $packageName ==========")

        try {
            // 动态获取主 Activity 类名
            mainActivityClass = findMainActivityClass(lpparam.classLoader, packageName)
            XposedBridge.log("ZaralynStudyMask: Main activity class: $mainActivityClass")

            // Hook ActivityThread.performLaunchActivity 来捕获所有 Activity 的启动
            XposedBridge.log("ZaralynStudyMask: Hooking ActivityThread.performLaunchActivity")
            XposedHelpers.findAndHookMethod(
                "android.app.ActivityThread",
                lpparam.classLoader,
                "performLaunchActivity",
                "android.app.ActivityThread\$ActivityClientRecord",
                android.content.Intent::class.java,
                String::class.java,
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = XposedHelpers.getObjectField(param.result, "activity") as Activity
                            val activityClassName = activity.javaClass.name
                            val intent = param.args[1] as Intent
                            
                            XposedBridge.log("ZaralynStudyMask: Activity launched - $activityClassName")
                            XposedBridge.log("ZaralynStudyMask: Intent action: ${intent.action}, flags: ${intent.flags}")
                            XposedBridge.log("ZaralynStudyMask: Intent categories: ${intent.categories?.joinToString()}")
                            
                            // 检查是否是主 Activity
                            val isMainActivity = isMainActivity(activity, intent, packageName)
                            XposedBridge.log("ZaralynStudyMask: Is main activity: $isMainActivity")
                            
                            if (!isMainActivity) {
                                return
                            }
                            
                            XposedBridge.log("ZaralynStudyMask: === MAIN ACTIVITY DETECTED: $activityClassName ===")
                            
                            // 检查是否应该显示原应用
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            
                            if (showOriginal) {
                                XposedBridge.log("ZaralynStudyMask: Showing original app")
                                return
                            }
                            
                            XposedBridge.log("ZaralynStudyMask: Replacing UI for main activity")
                            
                            // 延迟替换 UI，确保 Activity 完全初始化
                            activity.window.decorView.post {
                                try {
                                    replaceActivityUI(activity, prefs)
                                } catch (e: Exception) {
                                    XposedBridge.log("ZaralynStudyMask: Failed to replace UI - ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("ZaralynStudyMask: Error in performLaunchActivity hook - ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )

            // 备用方案：Hook setContentView
            XposedBridge.log("ZaralynStudyMask: Hooking Activity.setContentView")
            XposedHelpers.findAndHookMethod(
                Activity::class.java.name,
                lpparam.classLoader,
                "setContentView",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = param.thisObject as Activity
                            val activityClassName = activity.javaClass.name
                            
                            XposedBridge.log("ZaralynStudyMask: setContentView(int) called - $activityClassName")
                            
                            // 检查是否是主 Activity
                            if (!isMainActivity(activity, null, packageName)) {
                                return
                            }
                            
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                            
                            if (showOriginal || uiReplaced) {
                                return
                            }
                            
                            XposedBridge.log("ZaralynStudyMask: Replacing UI via setContentView hook")
                            prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()
                            
                            val newUI = createMaskUI(activity, prefs)
                            activity.setContentView(newUI)
                            param.setResult(null)
                        } catch (e: Exception) {
                            XposedBridge.log("ZaralynStudyMask: Error in setContentView(int) hook - ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                Activity::class.java.name,
                lpparam.classLoader,
                "setContentView",
                View::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = param.thisObject as Activity
                            val activityClassName = activity.javaClass.name
                            
                            XposedBridge.log("ZaralynStudyMask: setContentView(View) called - $activityClassName")
                            
                            // 检查是否是主 Activity
                            if (!isMainActivity(activity, null, packageName)) {
                                return
                            }
                            
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                            
                            if (showOriginal || uiReplaced) {
                                return
                            }
                            
                            XposedBridge.log("ZaralynStudyMask: Replacing UI via setContentView(View) hook")
                            prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()
                            
                            val newUI = createMaskUI(activity, prefs)
                            activity.setContentView(newUI)
                            param.setResult(null)
                        } catch (e: Exception) {
                            XposedBridge.log("ZaralynStudyMask: Error in setContentView(View) hook - ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )

        } catch (e: Exception) {
            XposedBridge.log("ZaralynStudyMask: Hook failed - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun findMainActivityClass(classLoader: ClassLoader, packageName: String): String? {
        try {
            // 获取 PackageManager
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            val context = XposedHelpers.getObjectField(currentActivityThread, "mSystemContext") as Context
            
            // 获取启动 Intent
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            
            if (intent != null) {
                val component = intent.component
                if (component != null) {
                    val className = component.className
                    XposedBridge.log("ZaralynStudyMask: Found main activity via PackageManager: $className")
                    return className
                }
            }
            
            // 备用方法：查询所有启动 Activity
            val mainIntent = Intent(Intent.ACTION_MAIN)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            mainIntent.`package` = packageName
            
            val resolveInfos = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfos.isNotEmpty()) {
                val resolveInfo = resolveInfos[0]
                val className = resolveInfo.activityInfo.name
                XposedBridge.log("ZaralynStudyMask: Found main activity via queryIntentActivities: $className")
                return className
            }
            
        } catch (e: Exception) {
            XposedBridge.log("ZaralynStudyMask: Error finding main activity - ${e.message}")
            e.printStackTrace()
        }
        
        return null
    }

    private fun isMainActivity(activity: Activity, intent: Intent?, packageName: String): Boolean {
        // 方法1：检查缓存的类名
        if (mainActivityClass != null && activity.javaClass.name == mainActivityClass) {
            return true
        }
        
        // 方法2：检查类名是否以 MainActivity 结尾
        if (activity.javaClass.name.endsWith(".MainActivity")) {
            return true
        }
        
        // 方法3：检查 Intent
        if (intent != null) {
            val isMainAction = intent.action == Intent.ACTION_MAIN
            val isLauncherCategory = intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
            val isFromLauncher = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
            
            XposedBridge.log("ZaralynStudyMask: Intent check - action: $isMainAction, launcher: $isLauncherCategory, fromLauncher: $isFromLauncher")
            
            if (isMainAction && isLauncherCategory) {
                return true
            }
        }
        
        // 方法4：检查是否是包中的第一个 Activity
        try {
            val packageManager = activity.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            mainIntent.`package` = packageName
            
            val resolveInfos = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfos.isNotEmpty()) {
                val mainClassName = resolveInfos[0].activityInfo.name
                if (activity.javaClass.name == mainClassName) {
                    return true
                }
            }
        } catch (e: Exception) {
            // 忽略错误
        }
        
        return false
    }

    private fun replaceActivityUI(activity: Activity, prefs: android.content.SharedPreferences) {
        XposedBridge.log("ZaralynStudyMask: Replacing activity UI")
        
        try {
            val newUI = createMaskUI(activity, prefs)
            activity.setContentView(newUI)
            prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()
            
            setupKeyListener(activity, prefs)
            
            XposedBridge.log("ZaralynStudyMask: UI replaced successfully")
        } catch (e: Exception) {
            XposedBridge.log("ZaralynStudyMask: Failed to replace UI - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createMaskUI(activity: Activity, prefs: android.content.SharedPreferences): View {
        XposedBridge.log("ZaralynStudyMask: Creating mask UI")
        
        val rootLayout = LinearLayout(activity)
        rootLayout.orientation = LinearLayout.VERTICAL
        
        val toolbar = createToolbar(activity)
        rootLayout.addView(toolbar)
        
        val scrollView = ScrollView(activity)
        val contentLayout = createContentLayout(activity)
        scrollView.addView(contentLayout)
        
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ))
        
        val bottomNav = createBottomNavigation(activity)
        rootLayout.addView(bottomNav)
        
        return rootLayout
    }
    
    private fun createToolbar(activity: Activity): LinearLayout {
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
    
    private fun createContentLayout(activity: Activity): LinearLayout {
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.WHITE)
        layout.setPadding(16, 16, 16, 16)
        
        layout.addView(createSectionTitle(activity, "选择年级"))
        layout.addView(createGradeRow(activity))
        
        layout.addView(createSectionTitle(activity, "课本列表"))
        layout.addView(createBookList(activity))
        
        return layout
    }
    
    private fun createSectionTitle(activity: Activity, text: String): TextView {
        val title = TextView(activity)
        title.text = text
        title.textSize = 18f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setPadding(0, 0, 0, 8)
        return title
    }
    
    private fun createGradeRow(activity: Activity): LinearLayout {
        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        
        val grades = listOf("高一", "高二", "高三")
        for (grade in grades) {
            row.addView(createGradeCard(activity, grade))
        }
        
        return row
    }
    
    private fun createGradeCard(activity: Activity, gradeName: String): LinearLayout {
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
    
    private fun createBookList(activity: Activity): LinearLayout {
        val list = LinearLayout(activity)
        list.orientation = LinearLayout.VERTICAL
        
        val books = listOf("必修一", "必修二", "英语语法", "词汇手册")
        for (book in books) {
            list.addView(createBookCard(activity, book))
        }
        
        return list
    }
    
    private fun createBookCard(activity: Activity, bookName: String): LinearLayout {
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
    
    private fun createBottomNavigation(activity: Activity): LinearLayout {
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
    
    private fun setupKeyListener(activity: Activity, prefs: android.content.SharedPreferences) {
        val decorView = activity.window.decorView
        
        decorView.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                if (keyCode == android.view.KeyEvent.KEYCODE_F10 || 
                    keyCode == android.view.KeyEvent.KEYCODE_MENU) {
                    XposedBridge.log("ZaralynStudyMask: F10/MENU key pressed")
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
    
    private fun launchOriginalApp(activity: Activity, prefs: android.content.SharedPreferences) {
        XposedBridge.log("ZaralynStudyMask: Launching original app")
        
        prefs.edit()
            .putBoolean(KEY_SHOW_ORIGINAL, true)
            .putBoolean(KEY_UI_REPLACED, false)
            .apply()
        
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
    }
}