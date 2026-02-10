package com.zaralyn.study.mask

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class XposedHook : IXposedHookLoadPackage {

    companion object {
        private const val PREFS_NAME = "ZaralynStudyMask"
        private const val KEY_SHOW_ORIGINAL = "show_original_app"
        private const val KEY_CLICK_COUNT = "home_click_count"
        private const val KEY_LAST_CLICK_TIME = "last_click_time"
        private const val KEY_UI_REPLACED = "ui_replaced"
        private const val KEY_MAIN_ACTIVITY = "main_activity"
        private const val LOG_FILE = "log.log"
    }

    private var mainActivityClass: String? = null
    private var logFile: File? = null
    private var logFileAlt: File? = null
    private var packageName: String = ""

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        packageName = lpparam.packageName

        logToAll("========== ZaralynStudyMask: Loading hook for $packageName ==========")

        try {
            // 初始化日志文件
            initLogFile(lpparam.classLoader)

            // 动态获取主 Activity 类名
            mainActivityClass = findMainActivityClass(lpparam.classLoader, packageName)
            logToAll("Main activity class: $mainActivityClass")

            // 方案1: Hook ActivityThread.performLaunchActivity - Activity启动Hook法
            logToAll("Setting up Hook 1: ActivityThread.performLaunchActivity")
            hookActivityThread(lpparam.classLoader)

            // 方案2: Hook Activity.setContentView - setContentView拦截法
            logToAll("Setting up Hook 2: Activity.setContentView")
            hookSetContentView(lpparam.classLoader)

            // 方案3: Hook PhoneWindow.setContentView - PhoneWindow底层修改法
            logToAll("Setting up Hook 3: PhoneWindow.setContentView")
            hookPhoneWindow(lpparam.classLoader)

            // 方案4: Hook DecorView - DecorView挂载替换法
            logToAll("Setting up Hook 4: DecorView.dispatchAttachedToWindow")
            hookDecorView(lpparam.classLoader)

            // 方案5: Hook LayoutInflater - LayoutInflater资源劫持法
            logToAll("Setting up Hook 5: LayoutInflater.inflate")
            hookLayoutInflater(lpparam.classLoader)

            logToAll("All hooks installed successfully")

        } catch (e: Exception) {
            logToAll("Hook failed - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initLogFile(classLoader: ClassLoader) {
        try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            val context = XposedHelpers.getObjectField(currentActivityThread, "mSystemContext") as Context
            
            // 获取标准文件目录
            val appDir = context.filesDir
            logFile = File(appDir, LOG_FILE)
            
            // 清空旧日志
            if (logFile?.exists() == true) {
                logFile?.delete()
            }
            
            // 同时尝试创建备用路径 /data/user/0/
            val altDir = File("/data/user/0/$packageName/files")
            if (altDir.exists() || altDir.mkdirs()) {
                logFileAlt = File(altDir, LOG_FILE)
                if (logFileAlt?.exists() == true) {
                    logFileAlt?.delete()
                }
            }
            
            // 记录路径信息
            XposedBridge.log("ZaralynStudyMask: App files dir: $appDir")
            XposedBridge.log("ZaralynStudyMask: Log file path: ${logFile?.absolutePath}")
            if (logFileAlt != null) {
                XposedBridge.log("ZaralynStudyMask: Alt log file path: ${logFileAlt?.absolutePath}")
            }
            
            logToFile("Log file initialized: ${logFile?.absolutePath}")
            if (logFileAlt != null) {
                logToFile("Alt log file initialized: ${logFileAlt?.absolutePath}")
            }
        } catch (e: Exception) {
            XposedBridge.log("Failed to init log file: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun logToAll(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $message"
        
        XposedBridge.log("ZaralynStudyMask: $message")
        logToFile(logMessage)
    }

    private fun logToFile(message: String) {
        try {
            // 写入主日志文件
            logFile?.let { file ->
                try {
                    FileWriter(file, true).use { writer ->
                        writer.write("$message\n")
                        writer.flush()
                    }
                } catch (e: Exception) {
                    // 忽略主文件写入错误
                }
            }
            
            // 写入备用日志文件
            logFileAlt?.let { file ->
                try {
                    FileWriter(file, true).use { writer ->
                        writer.write("$message\n")
                        writer.flush()
                    }
                } catch (e: Exception) {
                    // 忽略备用文件写入错误
                }
            }
        } catch (e: Exception) {
            // 忽略日志写入错误
        }
    }

    // 方案1: ActivityThread Hook 法
    private fun hookActivityThread(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ActivityThread",
                classLoader,
                "performLaunchActivity",
                "android.app.ActivityThread\$ActivityClientRecord",
                Intent::class.java,
                String::class.java,
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = XposedHelpers.getObjectField(param.result, "activity") as Activity
                            val activityClassName = activity.javaClass.name
                            val intent = param.args[1] as Intent
                            
                            logToAll("Activity launched: $activityClassName")
                            logToAll("Intent action: ${intent.action}, flags: ${intent.flags}")
                            logToAll("Intent categories: ${intent.categories?.joinToString()}")
                            
                            if (!isMainActivity(activity, intent)) {
                                logToAll("Not a main activity, skipping")
                                return
                            }
                            
                            logToAll("=== MAIN ACTIVITY DETECTED: $activityClassName ===")
                            
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            
                            if (showOriginal) {
                                logToAll("Showing original app")
                                return
                            }
                            
                            logToAll("Replacing UI via ActivityThread hook")
                            
                            activity.window.decorView.post {
                                try {
                                    replaceActivityUI(activity, prefs)
                                } catch (e: Exception) {
                                    logToAll("Failed to replace UI: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            logToAll("Error in performLaunchActivity hook: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            logToAll("Failed to hook ActivityThread: ${e.message}")
            e.printStackTrace()
        }
    }

    // 方案2: setContentView Hook 法
    private fun hookSetContentView(classLoader: ClassLoader) {
        try {
            // Hook setContentView(int)
            XposedHelpers.findAndHookMethod(
                Activity::class.java.name,
                classLoader,
                "setContentView",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = param.thisObject as Activity
                            val activityClassName = activity.javaClass.name
                            
                            logToAll("setContentView(int) called: $activityClassName")
                            
                            if (!isMainActivity(activity, null)) {
                                return
                            }
                            
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                            
                            if (showOriginal || uiReplaced) {
                                return
                            }
                            
                            logToAll("Replacing UI via setContentView(int) hook")
                            prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()
                            
                            val newUI = createMaskUI(activity, prefs)
                            activity.setContentView(newUI)
                            param.setResult(null)
                        } catch (e: Exception) {
                            logToAll("Error in setContentView(int) hook: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )

            // Hook setContentView(View)
            XposedHelpers.findAndHookMethod(
                Activity::class.java.name,
                classLoader,
                "setContentView",
                View::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = param.thisObject as Activity
                            val activityClassName = activity.javaClass.name
                            
                            logToAll("setContentView(View) called: $activityClassName")
                            
                            if (!isMainActivity(activity, null)) {
                                return
                            }
                            
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                            
                            if (showOriginal || uiReplaced) {
                                return
                            }
                            
                            logToAll("Replacing UI via setContentView(View) hook")
                            prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()
                            
                            val newUI = createMaskUI(activity, prefs)
                            activity.setContentView(newUI)
                            param.setResult(null)
                        } catch (e: Exception) {
                            logToAll("Error in setContentView(View) hook: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            logToAll("Failed to hook setContentView: ${e.message}")
            e.printStackTrace()
        }
    }

    // 方案3: PhoneWindow Hook 法
    private fun hookPhoneWindow(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.internal.policy.PhoneWindow",
                classLoader,
                "setContentView",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val phoneWindow = param.thisObject
                            val activity = XposedHelpers.getObjectField(phoneWindow, "mActivity") as? Activity
                            
                            if (activity == null) {
                                return
                            }
                            
                            val activityClassName = activity.javaClass.name
                            logToAll("PhoneWindow.setContentView called: $activityClassName")
                            
                            if (!isMainActivity(activity, null)) {
                                return
                            }
                            
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                            
                            if (showOriginal || uiReplaced) {
                                return
                            }
                            
                            logToAll("Replacing UI via PhoneWindow hook")
                            prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()
                            
                            val newUI = createMaskUI(activity, prefs)
                            activity.setContentView(newUI)
                            param.setResult(null)
                        } catch (e: Exception) {
                            logToAll("Error in PhoneWindow hook: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            logToAll("Failed to hook PhoneWindow: ${e.message}")
            e.printStackTrace()
        }
    }

    // 方案4: DecorView Hook 法
    private fun hookDecorView(classLoader: ClassLoader) {
        try {
            val attachInfoClass = XposedHelpers.findClass("android.view.View\$AttachInfo", classLoader)
            
            XposedHelpers.findAndHookMethod(
                "android.view.View",
                classLoader,
                "dispatchAttachedToWindow",
                attachInfoClass,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val view = param.thisObject
                            
                            // 检查是否是 DecorView
                            if (view.javaClass.name != "com.android.internal.policy.DecorView") {
                                return
                            }
                            
                            // 获取 Activity
                            val viewContext = XposedHelpers.callMethod(view, "getContext") as Context
                            if (viewContext !is Activity) {
                                return
                            }
                            
                            val activity = viewContext
                            val activityClassName = activity.javaClass.name
                            
                            logToAll("DecorView attached: $activityClassName")
                            
                            if (!isMainActivity(activity, null)) {
                                return
                            }
                            
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                            
                            if (showOriginal || uiReplaced) {
                                return
                            }
                            
                            logToAll("Replacing UI via DecorView hook")
                            prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()
                            
                            activity.window.decorView.post {
                                try {
                                    val newUI = createMaskUI(activity, prefs)
                                    activity.setContentView(newUI)
                                } catch (e: Exception) {
                                    logToAll("Failed to replace UI via DecorView: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            logToAll("Error in DecorView hook: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            logToAll("Failed to hook DecorView: ${e.message}")
            e.printStackTrace()
        }
    }

    // 方案5: LayoutInflater Hook 法
    private fun hookLayoutInflater(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.view.LayoutInflater",
                classLoader,
                "inflate",
                Int::class.javaPrimitiveType,
                ViewGroup::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val inflaterContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                            
                            if (inflaterContext !is Activity) {
                                return
                            }
                            
                            val activity = inflaterContext
                            val activityClassName = activity.javaClass.name
                            
                            // 只记录日志，不替换
                            logToAll("LayoutInflater.inflate called: $activityClassName, resource: ${param.args[0]}")
                        } catch (e: Exception) {
                            logToAll("Error in LayoutInflater hook: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            logToAll("Failed to hook LayoutInflater: ${e.message}")
            e.printStackTrace()
        }
    }

    // 动态获取主 Activity
    private fun findMainActivityClass(classLoader: ClassLoader, packageName: String): String? {
        try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            val context = XposedHelpers.getObjectField(currentActivityThread, "mSystemContext") as Context
            
            // 方法1: PackageManager.getLaunchIntentForPackage()
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            
            if (intent != null) {
                val component = intent.component
                if (component != null) {
                    val className = component.className
                    logToAll("Found main activity via PackageManager: $className")
                    return className
                }
            }
            
            // 方法2: queryIntentActivities()
            val mainIntent = Intent(Intent.ACTION_MAIN)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            mainIntent.`package` = packageName
            
            val resolveInfos = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfos.isNotEmpty()) {
                val resolveInfo = resolveInfos[0]
                val className = resolveInfo.activityInfo.name
                logToAll("Found main activity via queryIntentActivities: $className")
                return className
            }
            
        } catch (e: Exception) {
            logToAll("Error finding main activity: ${e.message}")
            e.printStackTrace()
        }
        
        return null
    }

    // 判断是否是主 Activity
    private fun isMainActivity(activity: Activity, intent: Intent?): Boolean {
        val activityClassName = activity.javaClass.name
        
        // 方法1: 缓存的类名
        if (mainActivityClass != null && activityClassName == mainActivityClass) {
            logToAll("Main activity detected via cache: $activityClassName")
            return true
        }
        
        // 方法2: 类名以 MainActivity 结尾
        if (activityClassName.endsWith(".MainActivity")) {
            logToAll("Main activity detected via class name: $activityClassName")
            return true
        }
        
        // 方法3: Intent 检查
        if (intent != null) {
            val isMainAction = intent.action == Intent.ACTION_MAIN
            val isLauncherCategory = intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
            
            if (isMainAction && isLauncherCategory) {
                logToAll("Main activity detected via Intent: $activityClassName")
                return true
            }
        }
        
        // 方法4: PackageManager 查询
        try {
            val packageManager = activity.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            mainIntent.`package` = packageName
            
            val resolveInfos = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfos.isNotEmpty()) {
                val mainClassName = resolveInfos[0].activityInfo.name
                if (activityClassName == mainClassName) {
                    logToAll("Main activity detected via PackageManager query: $activityClassName")
                    return true
                }
            }
        } catch (e: Exception) {
            // 忽略错误
        }
        
        return false
    }

    // 替换 Activity UI
    private fun replaceActivityUI(activity: Activity, prefs: android.content.SharedPreferences) {
        logToAll("Replacing activity UI")
        
        try {
            // 保存原视图
            val decorView = activity.window.decorView
            val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)
            
            if (contentView != null && contentView.childCount > 0) {
                val originalView = contentView.getChildAt(0)
                logToAll("Original view: ${originalView.javaClass.name}")
            }
            
            // 创建并设置新 UI
            val newUI = createMaskUI(activity, prefs)
            activity.setContentView(newUI)
            prefs.edit().putBoolean(KEY_UI_REPLACED, true).apply()
            
            setupKeyListener(activity, prefs)
            
            logToAll("UI replaced successfully")
        } catch (e: Exception) {
            logToAll("Failed to replace UI: ${e.message}")
            e.printStackTrace()
        }
    }

    // 创建伪装界面
    private fun createMaskUI(activity: Activity, prefs: android.content.SharedPreferences): View {
        logToAll("Creating mask UI")
        
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
    
    // 设置按键监听
    private fun setupKeyListener(activity: Activity, prefs: android.content.SharedPreferences) {
        val decorView = activity.window.decorView
        
        decorView.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                if (keyCode == android.view.KeyEvent.KEYCODE_F10 || 
                    keyCode == android.view.KeyEvent.KEYCODE_MENU) {
                    logToAll("F10/MENU key pressed")
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
                        
                        logToAll("Home click count: $newClickCount")
                        
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
    
    // 启动原应用
    private fun launchOriginalApp(activity: Activity, prefs: android.content.SharedPreferences) {
        logToAll("Launching original app")
        
        prefs.edit()
            .putBoolean(KEY_SHOW_ORIGINAL, true)
            .putBoolean(KEY_UI_REPLACED, false)
            .apply()
        
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
    }
}