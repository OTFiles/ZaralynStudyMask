package com.zaralyn.study.mask

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class XposedHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "ZaralynStudyMask"
        private const val PREFS_NAME = "ZaralynStudyMask"
        private const val KEY_SHOW_ORIGINAL = "show_original_app"
        private const val KEY_CLICK_COUNT = "home_click_count"
        private const val KEY_LAST_CLICK_TIME = "last_click_time"
        private const val KEY_UI_REPLACED = "ui_replaced"
        private const val KEY_MAIN_ACTIVITY = "main_activity"
        private const val KEY_SELECTED_GRADE = "selected_grade"
        
        // 颜色系统
        private const val COLOR_PRIMARY = "#1E88E5"      // 天蓝色
        private const val COLOR_PRIMARY_CONTAINER = "#D1E4FF"  // 浅蓝色
        private const val COLOR_SURFACE = "#FFFFFF"        // 白色
        private const val COLOR_SURFACE_CONTAINER = "#F3F3F3"  // 浅灰
        private const val COLOR_OUTLINE = "#74777F"        // 灰色
        private const val COLOR_ON_SURFACE = "#1D1B20"     // 深灰
        
        // 课本数据
        private val GRADE_10_BOOKS = listOf("必修一", "必修二", "英语语法", "词汇手册")
        private val GRADE_11_BOOKS = listOf("必修三", "必修四", "阅读训练", "写作指南")
        private val GRADE_12_BOOKS = listOf("必修五", "选修六", "高考真题", "冲刺复习")
    }

    private var mainActivityClass: String? = null
    private var packageName: String = ""
    
    // 当前选中的年级（10=高一, 11=高二, 12=高三）
    private var currentGrade = 10

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val loadedPackageName = lpparam.packageName
        
        // 过滤系统关键进程，避免影响系统稳定性
        if (isSystemProcess(loadedPackageName)) {
            return
        }
        
        packageName = loadedPackageName

        logToAll("========== ZaralynStudyMask: Loading hook for $packageName ==========")

        try {
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

    private fun logToAll(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $message"
        
        // 输出到 logcat
        Log.d(TAG, message)
        
        // 输出到 XposedBridge.log
        XposedBridge.log("ZaralynStudyMask: $message")
    }

    // 方案1: ActivityThread Hook 法
    private fun hookActivityThread(classLoader: ClassLoader) {
        try {
            // Android 10+ 使用新的方法签名（2个参数）
            XposedHelpers.findAndHookMethod(
                "android.app.ActivityThread",
                classLoader,
                "performLaunchActivity",
                "android.app.ActivityThread\$ActivityClientRecord",
                Intent::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            // 检查返回值是否为有效的 Activity
                            if (param.result == null) {
                                return
                            }
                            
                            val activity = param.result as Activity
                            val activityClassName = activity.javaClass.name
                            
                            // 安全获取 Intent，处理 null 情况
                            val intent = if (param.args.size > 1) {
                                param.args[1] as? Intent
                            } else {
                                null
                            }
                            
                            logToAll("Activity launched: $activityClassName")
                            
                            if (intent != null) {
                                logToAll("Intent action: ${intent.action}, flags: ${intent.flags}")
                                logToAll("Intent categories: ${intent.categories?.joinToString()}")
                            } else {
                                logToAll("Intent is null, trying to get from activity")
                            }
                            
                            if (!isMainActivity(activity, intent)) {
                                logToAll("Not a main activity, skipping")
                                return
                            }
                            
                            logToAll("=== MAIN ACTIVITY DETECTED: $activityClassName ===")
                            
                            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            
                            // 重置所有标志，确保每次启动都显示伪装界面
                            val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                            val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                            
                            if (showOriginal) {
                                logToAll("Resetting KEY_SHOW_ORIGINAL from true to false")
                                prefs.edit().putBoolean(KEY_SHOW_ORIGINAL, false).apply()
                            }
                            
                            if (uiReplaced) {
                                logToAll("Resetting KEY_UI_REPLACED from true to false")
                                prefs.edit().putBoolean(KEY_UI_REPLACED, false).apply()
                            }
                            
                            logToAll("Replacing UI via ActivityThread hook")
                            
                            // 检查是否已经替换过，避免重复替换
                            val currentUiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                            if (!currentUiReplaced) {
                                activity.window.decorView.post {
                                    try {
                                        replaceActivityUI(activity, prefs)
                                    } catch (e: Exception) {
                                        logToAll("Failed to replace UI: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                            } else {
                                logToAll("UI already replaced, skipping")
                            }
                        } catch (e: Exception) {
                            logToAll("Error in performLaunchActivity hook: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )
            logToAll("Hooked performLaunchActivity (Android 10+ signature)")
        } catch (e: NoSuchMethodError) {
            logToAll("Android 10+ signature not found, trying legacy signature...")
            // 尝试旧版本签名（4个参数），用于 Android 9 及以下
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
                                // 检查返回值是否为有效的 Activity
                                if (param.result == null) {
                                    return
                                }
                                
                                val activity = param.result as Activity
                                val activityClassName = activity.javaClass.name
                                
                                // 安全获取 Intent，处理 null 情况
                                val intent = if (param.args.size > 1) {
                                    param.args[1] as? Intent
                                } else {
                                    null
                                }
                                
                                logToAll("Activity launched: $activityClassName")
                                
                                if (intent != null) {
                                    logToAll("Intent action: ${intent.action}, flags: ${intent.flags}")
                                    logToAll("Intent categories: ${intent.categories?.joinToString()}")
                                } else {
                                    logToAll("Intent is null, trying to get from activity")
                                }
                                
                                if (!isMainActivity(activity, intent)) {
                                    logToAll("Not a main activity, skipping")
                                    return
                                }
                                
                                logToAll("=== MAIN ACTIVITY DETECTED: $activityClassName ===")
                                
                                val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                
                                // 重置所有标志，确保每次启动都显示伪装界面
                                val showOriginal = prefs.getBoolean(KEY_SHOW_ORIGINAL, false)
                                val uiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                                
                                if (showOriginal) {
                                    logToAll("Resetting KEY_SHOW_ORIGINAL from true to false")
                                    prefs.edit().putBoolean(KEY_SHOW_ORIGINAL, false).apply()
                                }
                                
                                if (uiReplaced) {
                                    logToAll("Resetting KEY_UI_REPLACED from true to false")
                                    prefs.edit().putBoolean(KEY_UI_REPLACED, false).apply()
                                }
                                
                                logToAll("Replacing UI via ActivityThread hook")
                                
                                // 检查是否已经替换过，避免重复替换
                                val currentUiReplaced = prefs.getBoolean(KEY_UI_REPLACED, false)
                                if (!currentUiReplaced) {
                                    activity.window.decorView.post {
                                        try {
                                            replaceActivityUI(activity, prefs)
                                        } catch (e: Exception) {
                                            logToAll("Failed to replace UI: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }
                                } else {
                                    logToAll("UI already replaced, skipping")
                                }
                            } catch (e: Exception) {
                                logToAll("Error in performLaunchActivity hook: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                )
                logToAll("Hooked performLaunchActivity (legacy signature)")
            } catch (e2: Exception) {
                logToAll("Failed to hook ActivityThread with legacy signature: ${e2.message}")
                e2.printStackTrace()
            }
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
            // 提前查找 DecorView 类，使用类对象检查而不是字符串比较
            val decorViewClass = XposedHelpers.findClass("com.android.internal.policy.DecorView", classLoader)
            
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
                            
                            // 检查是否是 DecorView，使用类对象的 isInstance 方法
                            if (!decorViewClass.isInstance(view)) {
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
            
            // 使用安全转换，避免 NullPointerException
            var context: Context? = null
            
            // 尝试获取 mSystemContext
            val systemContext = XposedHelpers.getObjectField(currentActivityThread, "mSystemContext")
            if (systemContext is Context) {
                context = systemContext
            }
            
            // 如果获取失败，尝试获取 mInitialApplication
            if (context == null) {
                val initialApp = XposedHelpers.getObjectField(currentActivityThread, "mInitialApplication")
                if (initialApp is Context) {
                    context = initialApp
                }
            }
            
            // 如果仍然获取失败，直接返回 null，避免崩溃
            if (context == null) {
                logToAll("Warning: Could not get Context from ActivityThread")
                return null
            }
            
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
            // 重置显示原应用的标志，确保每次启动都显示伪装界面
            prefs.edit().putBoolean(KEY_SHOW_ORIGINAL, false).apply()
            logToAll("Reset KEY_SHOW_ORIGINAL to false")
            
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
        
        // 从SharedPreferences加载当前选中的年级
        currentGrade = prefs.getInt(KEY_SELECTED_GRADE, 10)
        logToAll("Loaded current grade: $currentGrade")
        
        val rootLayout = LinearLayout(activity)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setBackgroundColor(Color.parseColor(COLOR_SURFACE))
        
        // 顶部应用栏
        val toolbar = createToolbar(activity)
        rootLayout.addView(toolbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (72 * activity.resources.displayMetrics.density).toInt()
        ))
        
        // 可滚动内容区
        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(Color.parseColor(COLOR_SURFACE))
        
        val contentLayout = createContentLayout(activity, prefs)
        scrollView.addView(contentLayout)
        
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ))
        
        // 底部导航栏
        val bottomNav = createBottomNavigation(activity, prefs)
        rootLayout.addView(bottomNav, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (56 * activity.resources.displayMetrics.density).toInt()
        ))
        
        return rootLayout
    }
    
    private fun createToolbar(activity: Activity): LinearLayout {
        val toolbar = LinearLayout(activity)
        toolbar.orientation = LinearLayout.HORIZONTAL
        toolbar.setBackgroundColor(Color.parseColor(COLOR_PRIMARY))
        toolbar.gravity = android.view.Gravity.CENTER_VERTICAL
        toolbar.setPadding(
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt()
        )
        
        // 书本图标（使用文字替代）
        val icon = TextView(activity)
        icon.text = "[书]"
        icon.setTextColor(Color.WHITE)
        icon.textSize = 20f
        icon.setPadding(0, 0, (12 * activity.resources.displayMetrics.density).toInt(), 0)
        
        // 标题
        val title = TextView(activity)
        title.text = "高中英语学习"
        title.setTextColor(Color.WHITE)
        title.textSize = 20f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        
        toolbar.addView(icon)
        toolbar.addView(title)
        
        return toolbar
    }
    
    private fun createContentLayout(activity: Activity, prefs: android.content.SharedPreferences): LinearLayout {
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.parseColor(COLOR_SURFACE))
        layout.setPadding(
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt()
        )
        
        // 年级选择区域
        layout.addView(createSectionTitle(activity, "选择年级"))
        layout.addView(createGradeRow(activity, prefs))
        
        // 课本列表区域
        layout.addView(createSectionTitle(activity, "课本列表"))
        layout.addView(createBookList(activity, prefs))
        
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
    
    private fun createGradeRow(activity: Activity, prefs: android.content.SharedPreferences): LinearLayout {
        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = android.view.Gravity.CENTER_VERTICAL
        
        val grades = listOf(Pair(10, "高一"), Pair(11, "高二"), Pair(12, "高三"))
        for ((gradeLevel, gradeName) in grades) {
            row.addView(createGradeCard(activity, gradeName, gradeLevel, prefs))
        }
        
        return row
    }
    
    private fun createGradeCard(activity: Activity, gradeName: String, gradeLevel: Int, prefs: android.content.SharedPreferences): LinearLayout {
        val dp = activity.resources.displayMetrics.density
        val isSelected = currentGrade == gradeLevel
        
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = android.view.Gravity.CENTER_VERTICAL
        
        // 设置背景色和边框
        if (isSelected) {
            card.setBackgroundColor(Color.parseColor(COLOR_PRIMARY_CONTAINER))
            card.background = createBorderDrawable(activity, Color.parseColor(COLOR_PRIMARY), 2)
        } else {
            card.setBackgroundColor(Color.parseColor(COLOR_SURFACE_CONTAINER))
            card.background = createBorderDrawable(activity, Color.parseColor(COLOR_OUTLINE), 1)
        }
        
        card.setPadding(
            (16 * dp).toInt(),
            (16 * dp).toInt(),
            (16 * dp).toInt(),
            (16 * dp).toInt()
        )
        
        card.layoutParams = LinearLayout.LayoutParams(
            0,
            (60 * dp).toInt(),
            1.0f
        )
        
        card.setPadding(
            (8 * dp).toInt(),
            (8 * dp).toInt(),
            (8 * dp).toInt(),
            (8 * dp).toInt()
        )
        
        // 学校图标（使用文字替代）
        val icon = TextView(activity)
        icon.text = "[学]"
        icon.textSize = 18f
        if (isSelected) {
            icon.setTextColor(Color.parseColor(COLOR_PRIMARY))
        } else {
            icon.setTextColor(Color.parseColor(COLOR_OUTLINE))
        }
        icon.setPadding(0, 0, (8 * dp).toInt(), 0)
        
        // 年级名称
        val name = TextView(activity)
        name.text = gradeName
        name.textSize = 16f
        name.setTypeface(null, android.graphics.Typeface.BOLD)
        if (isSelected) {
            name.setTextColor(Color.parseColor(COLOR_PRIMARY))
        } else {
            name.setTextColor(Color.parseColor(COLOR_ON_SURFACE))
        }
        
        card.addView(icon)
        card.addView(name)
        
        // 添加点击事件
        card.setOnClickListener {
            logToAll("Grade clicked: $gradeName (Level: $gradeLevel)")
            currentGrade = gradeLevel
            prefs.edit().putInt(KEY_SELECTED_GRADE, gradeLevel).apply()
            
            try {
                // 刷新课本列表 - 使用安全的类型转换
                val contentView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                if (contentView == null || contentView.childCount == 0) {
                    logToAll("Content view not found or empty")
                    return@setOnClickListener
                }
                
                // 安全地遍历视图层级查找 rootLayout
                var rootLayout: LinearLayout? = null
                
                // 尝试第一层
                val firstChild = contentView.getChildAt(0)
                if (firstChild is LinearLayout) {
                    rootLayout = firstChild
                } else if (firstChild is ScrollView) {
                    // 如果是ScrollView，获取其子元素
                    val scrollViewChild = firstChild.getChildAt(0)
                    if (scrollViewChild is LinearLayout) {
                        rootLayout = scrollViewChild
                    }
                } else if (firstChild is android.view.ViewGroup) {
                    // 递归查找LinearLayout
                    for (i in 0 until firstChild.childCount) {
                        val grandChild = firstChild.getChildAt(i)
                        if (grandChild is ScrollView) {
                            val scrollViewChild = grandChild.getChildAt(0)
                            if (scrollViewChild is LinearLayout) {
                                rootLayout = scrollViewChild
                                break
                            }
                        } else if (grandChild is LinearLayout) {
                            rootLayout = grandChild
                            break
                        }
                    }
                }
                
                if (rootLayout == null) {
                    logToAll("Root layout not found")
                    return@setOnClickListener
                }
                
                // 找到课本列表并更新
                for (i in 0 until rootLayout.childCount) {
                    val child = rootLayout.getChildAt(i)
                    if (child is TextView && child.text.toString() == "课本列表") {
                        // 找到了课本列表标题，替换下一个子元素
                        if (i + 1 < rootLayout.childCount) {
                            val oldBookList = rootLayout.getChildAt(i + 1)
                            rootLayout.removeView(oldBookList)
                            rootLayout.addView(createBookList(activity, prefs), i + 1)
                        }
                        break
                    }
                }
                
                // 刷新年级卡片状态
                refreshGradeCards(activity, rootLayout, prefs)
            } catch (e: Exception) {
                logToAll("Error refreshing grade selection: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // 添加触摸反馈
        card.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    card.alpha = 0.8f
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    card.alpha = 1.0f
                }
            }
            false
        }
        
        return card
    }
    
    // 创建带边框的Drawable
    private fun createBorderDrawable(activity: Activity, borderColor: Int, borderWidth: Int): android.graphics.drawable.GradientDrawable {
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.setStroke(borderWidth, borderColor)
        drawable.cornerRadius = (16 * activity.resources.displayMetrics.density).toFloat()
        return drawable
    }
    
    // 刷新年级卡片状态
    private fun refreshGradeCards(activity: Activity, rootLayout: LinearLayout, prefs: android.content.SharedPreferences) {
        // 找到年级选择区域并刷新
        for (i in 0 until rootLayout.childCount) {
            val child = rootLayout.getChildAt(i)
            if (child is TextView && child.text.toString() == "选择年级") {
                if (i + 1 < rootLayout.childCount) {
                    val oldGradeRow = rootLayout.getChildAt(i + 1)
                    rootLayout.removeView(oldGradeRow)
                    rootLayout.addView(createGradeRow(activity, prefs), i + 1)
                }
                break
            }
        }
    }
    
    private fun createBookList(activity: Activity, prefs: android.content.SharedPreferences): LinearLayout {
        val list = LinearLayout(activity)
        list.orientation = LinearLayout.VERTICAL
        
        // 根据当前选中的年级获取课本列表
        val books = when (currentGrade) {
            11 -> GRADE_11_BOOKS
            12 -> GRADE_12_BOOKS
            else -> GRADE_10_BOOKS
        }
        
        for ((index, bookName) in books.withIndex()) {
            list.addView(createBookCard(activity, bookName, index))
        }
        
        return list
    }
    
    private fun createBookCard(activity: Activity, bookName: String, index: Int): LinearLayout {
        val dp = activity.resources.displayMetrics.density
        
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundColor(Color.parseColor(COLOR_SURFACE_CONTAINER))
        
        val drawable = createBorderDrawable(activity, Color.TRANSPARENT, 0)
        card.background = drawable
        card.setPadding(
            (16 * dp).toInt(),
            (16 * dp).toInt(),
            (16 * dp).toInt(),
            (16 * dp).toInt()
        )
        
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (120 * dp).toInt()
        )
        
        // 添加边距
        val cardParams = card.layoutParams as LinearLayout.LayoutParams
        cardParams.setMargins(0, 0, 0, (8 * dp).toInt())
        card.layoutParams = cardParams
        
        // 顶部内容行
        val topRow = LinearLayout(activity)
        topRow.orientation = LinearLayout.HORIZONTAL
        topRow.gravity = android.view.Gravity.CENTER_VERTICAL
        
        // 书本图标背景
        val iconBg = TextView(activity)
        iconBg.text = "[书]"
        iconBg.textSize = 20f
        iconBg.setTextColor(Color.WHITE)
        iconBg.setBackgroundColor(Color.parseColor(COLOR_PRIMARY))
        iconBg.gravity = android.view.Gravity.CENTER
        iconBg.setPadding(
            (12 * dp).toInt(),
            (12 * dp).toInt(),
            (12 * dp).toInt(),
            (12 * dp).toInt()
        )
        
        val iconParams = LinearLayout.LayoutParams(
            (48 * dp).toInt(),
            (48 * dp).toInt()
        )
        iconBg.layoutParams = iconParams
        
        val iconDrawable = android.graphics.drawable.GradientDrawable()
        iconDrawable.cornerRadius = (12 * dp).toFloat()
        iconDrawable.setColor(Color.parseColor(COLOR_PRIMARY))
        iconBg.background = iconDrawable
        
        // 课本标题和描述
        val textLayout = LinearLayout(activity)
        textLayout.orientation = LinearLayout.VERTICAL
        textLayout.setPadding((12 * dp).toInt(), 0, 0, 0)
        textLayout.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        
        val title = TextView(activity)
        title.text = bookName
        title.textSize = 16f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setTextColor(Color.parseColor(COLOR_ON_SURFACE))
        
        val subtitle = TextView(activity)
        subtitle.text = "Grade $currentGrade"
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.parseColor(COLOR_OUTLINE))
        subtitle.setPadding(0, (4 * dp).toInt(), 0, 0)
        
        textLayout.addView(title)
        textLayout.addView(subtitle)
        
        topRow.addView(iconBg)
        topRow.addView(textLayout)
        
        // 文件名
        val filename = TextView(activity)
        filename.text = "compulsory_${index + 1}.md"
        filename.textSize = 12f
        filename.setTextColor(Color.parseColor(COLOR_OUTLINE))
        filename.setTypeface(android.graphics.Typeface.MONOSPACE)
        filename.setPadding(0, (8 * dp).toInt(), 0, 0)
        
        card.addView(topRow)
        card.addView(filename)
        
        // 添加点击事件 - 打开ReaderActivity
        card.setOnClickListener {
            logToAll("Book clicked: $bookName")
            openReaderActivity(activity, bookName, index)
        }
        
        // 添加触摸反馈
        card.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    card.alpha = 0.8f
                    card.translationY = (-2 * dp).toFloat()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    card.alpha = 1.0f
                    card.translationY = 0f
                }
            }
            false
        }
        
        return card
    }
    
    // 打开ReaderActivity
    private fun openReaderActivity(activity: Activity, bookName: String, index: Int) {
        logToAll("Opening ReaderActivity for: $bookName")
        
        try {
            val intent = Intent()
            intent.setClassName(activity, "com.zaralyn.study.mask.ReaderActivity")
            intent.putExtra("extra_book_title", bookName)
            intent.putExtra("extra_book_file_name", "compulsory_${index + 1}.md")
            activity.startActivity(intent)
        } catch (e: Exception) {
            logToAll("Failed to open ReaderActivity: ${e.message}")
            e.printStackTrace()
            
            // 如果ReaderActivity不存在，显示Toast提示
            try {
                android.widget.Toast.makeText(activity, "阅读功能开发中", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e2: Exception) {
                // 忽略Toast错误
            }
        }
    }
    
    private fun createBottomNavigation(activity: Activity, prefs: android.content.SharedPreferences): LinearLayout {
        val nav = LinearLayout(activity)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.setBackgroundColor(Color.parseColor(COLOR_SURFACE))
        
        val items = listOf(
            Triple("主页", "[主]", true),
            Triple("课外", "[外]", false),
            Triple("设置", "[设]", false)
        )
        
        for ((index, item) in items.withIndex()) {
            val (title, icon, isSelected) = item
            nav.addView(createNavItem(activity, title, icon, isSelected))
        }
        
        return nav
    }
    
    private fun createNavItem(activity: Activity, title: String, icon: String, isSelected: Boolean): LinearLayout {
        val dp = activity.resources.displayMetrics.density
        
        val navItem = LinearLayout(activity)
        navItem.orientation = LinearLayout.VERTICAL
        navItem.gravity = android.view.Gravity.CENTER
        
        // 选中状态背景
        if (isSelected) {
            navItem.setBackgroundColor(Color.parseColor(COLOR_PRIMARY_CONTAINER))
        } else {
            navItem.setBackgroundColor(Color.TRANSPARENT)
        }
        
        navItem.setPadding(
            (16 * dp).toInt(),
            (8 * dp).toInt(),
            (16 * dp).toInt(),
            (8 * dp).toInt()
        )
        
        navItem.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1.0f
        )
        
        // 图标
        val iconText = TextView(activity)
        iconText.text = icon
        iconText.textSize = 20f
        if (isSelected) {
            iconText.setTextColor(Color.parseColor(COLOR_PRIMARY))
        } else {
            iconText.setTextColor(Color.parseColor(COLOR_OUTLINE))
        }
        iconText.gravity = android.view.Gravity.CENTER
        
        // 标题
        val titleText = TextView(activity)
        titleText.text = title
        titleText.textSize = 12f
        if (isSelected) {
            titleText.setTextColor(Color.parseColor(COLOR_PRIMARY))
        } else {
            titleText.setTextColor(Color.parseColor(COLOR_OUTLINE))
        }
        titleText.gravity = android.view.Gravity.CENTER
        titleText.setPadding(0, (4 * dp).toInt(), 0, 0)
        
        navItem.addView(iconText)
        navItem.addView(titleText)
        
        // 添加点击事件
        navItem.setOnClickListener {
            logToAll("Navigation clicked: $title")
            
            try {
                // 找到ScrollView并替换内容
                val contentView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                if (contentView != null && contentView.childCount > 0) {
                    val firstChild = contentView.getChildAt(0)
                    if (firstChild is LinearLayout) {
                        // 找到ScrollView（第二个子元素）
                        for (i in 0 until firstChild.childCount) {
                            val child = firstChild.getChildAt(i)
                            if (child is ScrollView) {
                                // 替换ScrollView的内容
                                child.removeAllViews()
                                val newContent = when (title) {
                                    "课外" -> createExploreContent(activity)
                                    "设置" -> createSettingsContent(activity, prefs)
                                    else -> createContentLayout(activity, prefs)
                                }
                                child.addView(newContent)
                                break
                            }
                        }
                        
                        // 更新导航栏选中状态
                        updateNavigationSelection(activity, title)
                    }
                }
            } catch (e: Exception) {
                logToAll("Error switching page: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // 添加触摸反馈
        navItem.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    navItem.alpha = 0.7f
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    navItem.alpha = 1.0f
                }
            }
            false
        }
        
        return navItem
    }
    
    // 更新导航栏选中状态
    private fun updateNavigationSelection(activity: Activity, selectedTitle: String) {
        val contentView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        if (contentView != null && contentView.childCount > 0) {
            val firstChild = contentView.getChildAt(0)
            if (firstChild is LinearLayout) {
                // 找到底部导航栏（最后一个子元素）
                val navBar = firstChild.getChildAt(firstChild.childCount - 1)
                if (navBar is LinearLayout) {
                    for (i in 0 until navBar.childCount) {
                        val navItem = navBar.getChildAt(i)
                        if (navItem is LinearLayout && navItem.childCount >= 2) {
                            val titleText = navItem.getChildAt(1) as? TextView
                            if (titleText?.text == selectedTitle) {
                                // 选中状态
                                navItem.setBackgroundColor(Color.parseColor(COLOR_PRIMARY_CONTAINER))
                                (navItem.getChildAt(0) as? TextView)?.setTextColor(Color.parseColor(COLOR_PRIMARY))
                                titleText.setTextColor(Color.parseColor(COLOR_PRIMARY))
                            } else {
                                // 未选中状态
                                navItem.setBackgroundColor(Color.TRANSPARENT)
                                (navItem.getChildAt(0) as? TextView)?.setTextColor(Color.parseColor(COLOR_OUTLINE))
                                titleText.setTextColor(Color.parseColor(COLOR_OUTLINE))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 设置按键监听
    private fun setupKeyListener(activity: Activity, prefs: android.content.SharedPreferences) {
        try {
            // 使用反射获取Activity的dispatchKeyEvent方法并Hook
            val activityClass = activity.javaClass
            
            XposedHelpers.findAndHookMethod(
                activityClass,
                "dispatchKeyEvent",
                android.view.KeyEvent::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val event = param.args[0] as android.view.KeyEvent
                        
                        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                            val keyCode = event.keyCode
                            
                            if (keyCode == android.view.KeyEvent.KEYCODE_F10 || 
                                keyCode == android.view.KeyEvent.KEYCODE_MENU) {
                                logToAll("F10/MENU key pressed")
                                launchOriginalApp(activity, prefs)
                                param.result = true
                                return
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
                                        logToAll("5 home clicks detected, launching original app")
                                        launchOriginalApp(activity, prefs)
                                        prefs.edit().putInt(KEY_CLICK_COUNT, 0).apply()
                                    }
                                } else {
                                    prefs.edit()
                                        .putInt(KEY_CLICK_COUNT, 1)
                                        .putLong(KEY_LAST_CLICK_TIME, currentTime)
                                        .apply()
                                    logToAll("Home click count reset to 1")
                                }
                            }
                        }
                    }
                }
            )
            
            logToAll("Key listener setup completed")
        } catch (e: Exception) {
            logToAll("Failed to setup key listener: ${e.message}")
            e.printStackTrace()
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
    
    // 创建设置页面内容
    private fun createSettingsContent(activity: Activity, prefs: android.content.SharedPreferences): LinearLayout {
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.parseColor(COLOR_SURFACE))
        layout.setPadding(
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt()
        )
        
        // 设置项列表
        val settings = listOf(
            Triple("夜间模式", "开启/关闭深色主题", "dark_mode"),
            Triple("自动播放音频", "自动播放课本音频", "auto_play_audio"),
            Triple("隐私政策", "查看隐私政策", "privacy_policy"),
            Triple("用户协议", "查看用户协议", "user_agreement"),
            Triple("版本信息", "当前版本：1.0.0", "version_info")
        )
        
        for (setting in settings) {
            layout.addView(createSettingItem(activity, setting.first, setting.second, setting.third, prefs))
        }
        
        return layout
    }
    
    // 创建单个设置项
    private fun createSettingItem(activity: Activity, title: String, description: String, key: String, prefs: android.content.SharedPreferences): LinearLayout {
        val dp = activity.resources.displayMetrics.density
        
        val item = LinearLayout(activity)
        item.orientation = LinearLayout.HORIZONTAL
        item.gravity = android.view.Gravity.CENTER_VERTICAL
        item.setBackgroundColor(Color.parseColor(COLOR_SURFACE_CONTAINER))
        item.setPadding(
            (16 * dp).toInt(),
            (16 * dp).toInt(),
            (16 * dp).toInt(),
            (16 * dp).toInt()
        )
        
        val itemParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        itemParams.setMargins(0, 0, 0, (8 * dp).toInt())
        item.layoutParams = itemParams
        
        // 图标
        val icon = TextView(activity)
        icon.text = "[设]"
        icon.textSize = 20f
        icon.setTextColor(Color.parseColor(COLOR_PRIMARY))
        icon.setPadding(0, 0, (12 * dp).toInt(), 0)
        
        // 文本内容
        val textLayout = LinearLayout(activity)
        textLayout.orientation = LinearLayout.VERTICAL
        textLayout.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        
        val titleText = TextView(activity)
        titleText.text = title
        titleText.textSize = 16f
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)
        titleText.setTextColor(Color.parseColor(COLOR_ON_SURFACE))
        
        val descText = TextView(activity)
        descText.text = description
        descText.textSize = 14f
        descText.setTextColor(Color.parseColor(COLOR_OUTLINE))
        descText.setPadding(0, (4 * dp).toInt(), 0, 0)
        
        textLayout.addView(titleText)
        textLayout.addView(descText)
        
        // 开关控件（仅用于开关项）
        val switch = android.widget.Switch(activity)
        switch.isChecked = prefs.getBoolean(key, false)
        switch.setPadding((8 * dp).toInt(), 0, 0, 0)
        
        item.addView(icon)
        item.addView(textLayout)
        
        // 只为前两个选项添加开关
        if (key == "dark_mode" || key == "auto_play_audio") {
            item.addView(switch)
            
            switch.setOnCheckedChangeListener { _, isChecked ->
                logToAll("Setting changed: $key = $isChecked")
                prefs.edit().putBoolean(key, isChecked).apply()
                try {
                    android.widget.Toast.makeText(activity, "$title: ${if (isChecked) "开启" else "关闭"}", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // 忽略Toast错误
                }
            }
        }
        
        // 添加点击事件
        item.setOnClickListener {
            logToAll("Setting clicked: $title")
            if (key == "privacy_policy" || key == "user_agreement") {
                try {
                    android.widget.Toast.makeText(activity, "$title 功能开发中", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // 忽略Toast错误
                }
            }
        }
        
        return item
    }
    
    // 创建课外页面内容
    private fun createExploreContent(activity: Activity): LinearLayout {
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.parseColor(COLOR_SURFACE))
        layout.setPadding(
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt(),
            (16 * activity.resources.displayMetrics.density).toInt()
        )
        
        // 课外学习卡片
        val exploreItems = listOf(
            Triple("[听]", "听力训练", "提升英语听力能力"),
            Triple("[说]", "口语练习", "提高英语口语表达"),
            Triple("[读]", "阅读理解", "增强英语阅读能力"),
            Triple("[写]", "写作指导", "掌握英语写作技巧")
        )
        
        // 创建2x2网格
        val gridLayout = LinearLayout(activity)
        gridLayout.orientation = LinearLayout.VERTICAL
        
        for (row in 0 until 2) {
            val rowLayout = LinearLayout(activity)
            rowLayout.orientation = LinearLayout.HORIZONTAL
            rowLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            for (col in 0 until 2) {
                val index = row * 2 + col
                if (index < exploreItems.size) {
                    val (icon, title, desc) = exploreItems[index]
                    rowLayout.addView(createExploreCard(activity, icon, title, desc))
                }
            }
            
            gridLayout.addView(rowLayout)
        }
        
        layout.addView(gridLayout)
        
        return layout
    }
    
    // 创建课外学习卡片
    private fun createExploreCard(activity: Activity, icon: String, title: String, description: String): LinearLayout {
        val dp = activity.resources.displayMetrics.density
        
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = android.view.Gravity.CENTER
        
        // 渐变背景
        val gradientDrawable = android.graphics.drawable.GradientDrawable()
        gradientDrawable.orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
        gradientDrawable.colors = intArrayOf(
            Color.parseColor(COLOR_PRIMARY),
            Color.parseColor(COLOR_PRIMARY_CONTAINER)
        )
        gradientDrawable.cornerRadius = (16 * dp).toFloat()
        card.background = gradientDrawable
        
        card.setPadding(
            (16 * dp).toInt(),
            (24 * dp).toInt(),
            (16 * dp).toInt(),
            (24 * dp).toInt()
        )
        
        card.layoutParams = LinearLayout.LayoutParams(
            0,
            (120 * dp).toInt(),
            1.0f
        )
        
        val cardParams = card.layoutParams as LinearLayout.LayoutParams
        cardParams.setMargins(0, 0, (8 * dp).toInt(), (8 * dp).toInt())
        card.layoutParams = cardParams
        
        // 图标
        val iconText = TextView(activity)
        iconText.text = icon
        iconText.textSize = 32f
        iconText.setTextColor(Color.WHITE)
        iconText.gravity = android.view.Gravity.CENTER
        
        // 标题
        val titleText = TextView(activity)
        titleText.text = title
        titleText.textSize = 16f
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)
        titleText.setTextColor(Color.WHITE)
        titleText.gravity = android.view.Gravity.CENTER
        titleText.setPadding(0, (8 * dp).toInt(), 0, 0)
        
        // 描述
        val descText = TextView(activity)
        descText.text = description
        descText.textSize = 12f
        descText.setTextColor(Color.parseColor("#E3F2FD"))
        descText.gravity = android.view.Gravity.CENTER
        descText.setPadding(0, (4 * dp).toInt(), 0, 0)
        
        card.addView(iconText)
        card.addView(titleText)
        card.addView(descText)
        
        // 添加点击事件
        card.setOnClickListener {
            logToAll("Explore clicked: $title")
            try {
                android.widget.Toast.makeText(activity, "$title 功能开发中", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // 忽略Toast错误
            }
        }
        
        return card
    }
    
    // 检查是否是系统关键进程
    private fun isSystemProcess(packageName: String): Boolean {
        // 过滤系统框架
        if (packageName.startsWith("android.")) {
            return true
        }
        
        // 过滤系统应用
        val systemPackages = listOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.mms",
            "com.android.browser",
            "com.android.contacts",
            "com.android.gallery3d",
            "com.android.camera",
            "com.android.calendar",
            "com.android.providers",
            "com.android.certinstaller",
            "com.android.inputmethod",
            "com.android.launcher",
            "com.android.packageinstaller",
            "com.android.server",
            "com.android.shell",
            "com.android.vending"
        )
        
        if (systemPackages.any { packageName.startsWith(it) }) {
            return true
        }
        
        // 过滤 Xposed/LSPosed/NPatch 框架本身
        if (packageName.startsWith("de.robv.android.xposed") ||
            packageName.startsWith("org.lsposed") ||
            packageName.startsWith("io.github.lsposed") ||
            packageName.startsWith("com.zaralyn.study.mask")) {
            return true
        }
        
        return false
    }
}