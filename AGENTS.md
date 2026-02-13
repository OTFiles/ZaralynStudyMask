# ZaralynStudyMask 项目指南

## 项目概述

ZaralynStudyMask（简称 Zara）是一个 Xposed/NPatch 模块，用于为 Android 应用添加"高中英语学习"的伪装界面。该模块通过多层 Xposed Hook 策略替换目标应用的 MainActivity，用户必须通过特定操作（连击5次主页键或按F10键）才能进入原应用。

**核心功能**：
- Material Design 3 风格的高中英语学习界面
- 年级选择（高一、高二、高三）
- 课本显示与 Markdown 阅读功能
- 课外学习模块（听力、口语、阅读、写作）
- 设置页面（夜间模式、自动播放等）
- 隐蔽入口（连击5次主页键或按F10键）
- 内置日志查看器和调试工具
- 多层 Hook 策略确保兼容性
- 智能应用类型检测和策略选择

**技术栈**：
- Kotlin 1.9.20
- Android Gradle Plugin 8.4.0
- Gradle 8.6
- Xposed API 82
- DataBinding
- Material Design 3
- Fragment 导航
- RecyclerView
- Java 17
- Android SDK 34

**应用信息**：
- 包名：`com.zaralyn.study.mask`
- 应用ID：`com.zaralyn.study.mask`
- 版本：1.0
- 最小 SDK：24
- 目标 SDK：34

---

## 构建和运行

### 环境限制
- **重要**：当前处于 termux 环境，无法进行本地编译
- 编译必须使用 GitHub Actions
- Zara 是 Xposed/NPatch 模块，需要通过 NPatch 工具修补目标应用

### 编译方法

#### 使用 GitHub Actions（推荐）
1. 推送代码到 GitHub
2. Actions 会自动编译并生成 APK
3. 从 Actions artifacts 下载编译产物

**编译产物位置**：
- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`

### GitHub Actions 配置
- 工作流文件：
  - `.github/workflows/build.yml` - 编译工作流
  - `.github/workflows/test.yml` - 测试工作流（Lint检查）
- 使用 `gradle/gradle-build-action@v3` 处理 Gradle 下载和缓存
- 使用 JDK 17 (temurin distribution)
- 自动上传编译产物为 artifacts

---

## 项目架构

### 整体架构设计

ZaralynStudyMask 采用分层模块化架构，主要包含以下核心模块：

1. **Xposed Hook 层** - 负责拦截目标应用的Activity启动和UI设置
2. **检测层** - 智能识别应用类型和主Activity
3. **策略层** - 根据应用类型选择最优UI替换策略
4. **UI 层** - 动态创建伪装界面
5. **事件层** - 处理用户交互事件（F10键、5次点击等）
6. **日志层** - 统一日志管理系统
7. **状态层** - 统一状态管理
8. **核心层** - 提供模块ClassLoader和Context管理

### 项目结构

```
app/src/main/
├── java/com/zaralyn/study/mask/
│   ├── MainActivity.kt                 # 主界面（日志查看器入口）
│   ├── LogViewerActivity.kt           # 日志查看界面
│   ├── XposedHook.kt                  # Xposed 模块入口（核心 Hook 逻辑）
│   ├── ReaderActivity.kt              # Markdown 阅读界面
│   ├── adapter/                       # RecyclerView 适配器
│   │   ├── BookAdapter.kt             # 课本列表适配器
│   │   ├── ExploreAdapter.kt          # 课外学习适配器
│   │   ├── GradeAdapter.kt            # 年级选择适配器（待优化）
│   │   └── SettingsAdapter.kt         # 设置页面适配器
│   ├── core/                          # 核心功能模块
│   │   ├── ModuleClassLoaderManager.kt  # 模块ClassLoader和Context管理
│   │   ├── config/
│   │   │   ├── HookConfig.kt          # Hook策略配置
│   │   │   └── StrategyConfig.kt      # UI替换策略配置
│   │   └── constants/
│   │       └── Constants.kt           # 常量定义
│   ├── detector/                      # 应用类型检测模块
│   │   ├── AppType.kt                 # 应用类型枚举
│   │   └── AppTypeDetector.kt         # 应用类型检测器
│   ├── data/                          # 数据源
│   │   ├── BookData.kt                # 课本数据
│   │   ├── ExploreData.kt             # 课外学习数据
│   │   └── SettingData.kt             # 设置数据
│   ├── event/                         # 事件处理模块
│   │   ├── EventHandler.kt            # 事件处理器接口
│   │   ├── EventManager.kt            # 事件管理器
│   │   └── handlers/
│   │       ├── F10KeyHandler.kt       # F10键处理
│   │       └── FiveTapHandler.kt      # 5次点击处理
│   ├── hook/                          # Hook管理模块
│   │   ├── HookContext.kt             # Hook上下文
│   │   ├── HookManager.kt             # Hook管理器
│   │   ├── HookStrategy.kt            # Hook策略接口
│   │   └── hooks/                     # 具体Hook实现
│   │       ├── ActivityThreadHook.kt
│   │       ├── DecorViewHook.kt
│   │       ├── LayoutInflaterHook.kt
│   │       ├── PhoneWindowHook.kt
│   │       └── SetContentViewHook.kt
│   ├── logger/                        # 日志模块
│   │   ├── Logger.kt                  # 日志接口
│   │   ├── LogLevel.kt                # 日志级别
│   │   └── XposedLogger.kt            # Xposed日志实现
│   ├── model/                         # 数据模型
│   │   ├── Book.kt                    # 课本模型
│   │   ├── ExploreItem.kt             # 课外学习项模型
│   │   ├── Grade.kt                   # 年级模型
│   │   └── SettingItem.kt             # 设置项模型
│   ├── state/                         # 状态管理模块
│   │   └── StateManager.kt            # 统一状态管理器
│   ├── strategy/                      # 策略模块
│   │   ├── StrategySelector.kt        # 策略选择器
│   │   ├── UIReplacementContext.kt    # UI替换上下文
│   │   ├── UIReplacementStrategy.kt   # UI替换策略接口
│   │   └── strategies/                # 具体策略实现
│   │       ├── DecorViewModificationStrategy.kt
│   │       ├── DefaultMaskUIProvider.kt  # UI提供者（纯代码创建）
│   │       ├── FrameLayoutWrapperStrategy.kt
│   │       └── WindowManagerOverlayStrategy.kt
│   ├── ui/fragments/                  # 界面片段
│   │   ├── HomeFragment.kt            # 主页片段
│   │   ├── ExploreFragment.kt         # 课外片段
│   │   └── SettingsFragment.kt        # 设置片段
│   └── view/                          # 视图定位模块
│       ├── CompositeViewLocator.kt    # 组合视图定位器
│       ├── ViewLocator.kt             # 视图定位器接口
│       └── locators/                  # 具体定位器实现
│           ├── ContentViewLocator.kt
│           └── OverlayViewLocator.kt
├── assets/
│   ├── xposed_init                    # Xposed 模块入口声明
│   └── test.md                        # 内部测试文件
└── res/
    ├── color/
    │   └── bottom_nav_color.xml       # 底部导航颜色
    ├── drawable/                      # 图标和背景资源
    ├── layout/                        # 布局文件（使用 DataBinding）
    ├── menu/
    │   └── bottom_nav_menu.xml        # 底部导航菜单
    ├── mipmap-*/                      # 应用图标（所有分辨率）
    └── values/                        # 值资源
        ├── strings.xml                # 字符串资源
        ├── colors.xml                 # 颜色系统
        ├── themes.xml                 # Material Design 3 主题
        └── dimens.xml                 # 尺寸定义
```

---

## 核心架构详解

### 1. ModuleClassLoaderManager - 模块资源管理

**位置**：`core/ModuleClassLoaderManager.kt`

**功能**：管理模块的ClassLoader和Context，解决ClassLoader隔离问题

**关键特性**：
- 使用`createPackageContext()`创建模块Context
- 使用`WeakReference`包裹Context防止内存泄漏
- 提供统一的模块ClassLoader和Context访问接口

**使用示例**：
```kotlin
// 初始化
ModuleClassLoaderManager.initialize(targetContext)

// 获取模块Context
val ctx = ModuleClassLoaderManager.getModuleContext()

// 获取模块ClassLoader
val classLoader = ModuleClassLoaderManager.getModuleClassLoader()

// 加载模块类
val fragmentClass = classLoader.loadClass("com.zaralyn.study.mask.ui.fragments.HomeFragment")
```

### 2. AppTypeDetector - 应用类型检测

**位置**：`detector/AppTypeDetector.kt`

**功能**：智能识别目标应用类型，选择最优Hook策略

**支持的应用类型**：
- `STANDARD` - 标准应用
- `NATIVE` - NativeActivity应用（如游戏）
- `BROWSER` - 浏览器应用
- `GAME` - 游戏应用
- `WEBVIEW` - WebView应用
- `UNKNOWN` - 未知类型

**检测方法**：
- PackageManager查询主Activity
- 检测NativeActivity子类
- 检测Native库加载
- 包名关键词匹配

### 3. HookManager - Hook管理

**位置**：`hook/HookManager.kt`

**功能**：统一管理所有Hook策略的安装和执行

**Hook策略**：
1. `ActivityThreadHook` - 拦截Activity启动（优先级1）
2. `SetContentViewHook` - 拦截setContentView调用（优先级2）
3. `PhoneWindowHook` - 拦截PhoneWindow.setContentView（优先级3）
4. `DecorViewHook` - 拦截DecorView附加到窗口（优先级4）
5. `LayoutInflaterHook` - 劫持布局资源加载（仅日志，优先级5）

**工作流程**：
```
XposedHook.handleLoadPackage()
  ↓
AppTypeDetector.detectAppType()
  ↓
HookManager.installHooks()
  ↓
Hook策略按优先级安装
  ↓
MainActivity检测
  ↓
StrategySelector.replaceUI()
```

### 4. StrategySelector - 策略选择器

**位置**：`strategy/StrategySelector.kt`

**功能**：根据应用类型选择最优UI替换策略

**策略类型**：
- `WINDOW_MANAGER_OVERLAY` - WindowManager覆盖层方案
- `DECOR_VIEW_MODIFICATION` - DecorView修改方案
- `FRAME_LAYOUT_WRAPPER` - FrameLayout包装方案

**应用类型映射**：
```kotlin
STANDARD    → [WindowManagerOverlay, DecorViewModification, FrameLayoutWrapper]
NATIVE      → [WindowManagerOverlay, FrameLayoutWrapper]
BROWSER     → [WindowManagerOverlay]
GAME        → [WindowManagerOverlay]
WEBVIEW     → [WindowManagerOverlay, DecorViewModification]
```

### 5. DefaultMaskUIProvider - UI提供者

**位置**：`strategy/strategies/DefaultMaskUIProvider.kt`

**功能**：使用纯代码创建伪装UI，避免ClassLoader隔离问题

**关键特性**：
- 移除ConstraintLayout依赖
- 使用ContextThemeWrapper包装Context
- 为Material组件提供AppCompat主题
- 动态加载Fragment

**代码示例**：
```kotlin
override fun createMaskUI(activity: Activity): View {
    val ctx = ModuleClassLoaderManager.getModuleContext()
    
    // 使用ContextThemeWrapper包装Context
    val themedContext = ContextThemeWrapper(ctx, R.style.Theme_ZaralynStudyMask)
    
    // 创建Material组件（使用themedContext）
    val appBarLayout = AppBarLayout(themedContext)
    val toolbar = MaterialToolbar(themedContext)
    val bottomNav = BottomNavigationView(themedContext)
    
    // 动态加载Fragment（使用模块ClassLoader）
    loadFragment(activity, "com.zaralyn.study.mask.ui.fragments.HomeFragment")
    
    return rootLayout
}
```

### 6. Logger - 日志系统

**位置**：`logger/`

**功能**：统一的日志管理系统

**日志级别**：
- `VERBOSE` - 详细信息
- `DEBUG` - 调试信息
- `INFO` - 一般信息
- `WARN` - 警告信息
- `ERROR` - 错误信息

**使用示例**：
```kotlin
val logger = Logger.create("MyClass", LogLevel.DEBUG)

logger.debug("Debug message")
logger.info("Info message")
logger.warn("Warning message")
logger.error("Error message", exception)
```

### 7. StateManager - 状态管理

**位置**：`state/StateManager.kt`

**功能**：统一状态管理，支持内存缓存和持久化存储

**特性**：
- 内存缓存（快速访问）
- SharedPreferences持久化
- 类型安全的状态读写

**使用示例**：
```kotlin
val stateManager = StateManager.getInstance(packageName)

// 内存缓存
stateManager.setBoolean("key", true)
val value = stateManager.getBoolean("key", false)

// 持久化存储
stateManager.persistBoolean("key", true)
val persistedValue = stateManager.loadBoolean("key", false)
```

---

## 编码规范

### Xposed 模块开发

1. **必须实现 `IXposedHookLoadPackage` 接口**
2. **在 `assets/xposed_init` 中声明模块入口类**：`com.zaralyn.study.mask.XposedHook`
3. **在 `AndroidManifest.xml` 中添加 Xposed 元数据**
4. **使用 `XposedHelpers` 进行 Hook 操作**
5. **日志输出到 logcat（标签：ZaralynStudyMask）和 XposedBridge.log**
6. **过滤系统关键进程，避免影响系统稳定性**

### ClassLoader 和 Context 使用

**重要规则**：
- ❌ **不要**在目标应用中直接使用`Class.forName()`加载模块类
- ✅ **必须**使用`ModuleClassLoaderManager.getModuleClassLoader()`
- ❌ **不要**使用目标应用的Context创建Fragment
- ✅ **必须**使用`ModuleClassLoaderManager.getModuleContext()`
- ❌ **不要**使用目标应用的LayoutInflater
- ✅ **必须**使用`LayoutInflater.from(ModuleClassLoaderManager.getModuleContext())`

**示例**：
```kotlin
// ❌ 错误
val fragmentClass = Class.forName("com.zaralyn.study.mask.ui.fragments.HomeFragment")
val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

// ✅ 正确
val moduleClassLoader = ModuleClassLoaderManager.getModuleClassLoader()
val fragmentClass = moduleClassLoader.loadClass("com.zaralyn.study.mask.ui.fragments.HomeFragment")
val ctx = ModuleClassLoaderManager.getModuleContext()
val inflater = LayoutInflater.from(ctx)
```

### DataBinding 使用

1. **所有使用 DataBinding 的布局文件必须包含 `<layout>` 根标签**
2. **使用到的命名空间必须在根元素中声明（如 `xmlns:app`）**
3. **Binding 类会自动生成，命名格式为 `XxxBinding`（Xxx 为布局文件名，使用驼峰命名）**
4. **在 `build.gradle` 中启用**：`buildFeatures { dataBinding = true }`
5. **使用模块Context创建DataBinding**：
```kotlin
val ctx = ModuleClassLoaderManager.getModuleContext()
val moduleInflater = LayoutInflater.from(ctx)
_binding = FragmentHomeBinding.inflate(moduleInflater, container, false)
```

### Material 组件主题

**重要规则**：
- Material组件（AppBarLayout、MaterialToolbar、BottomNavigationView等）需要AppCompat主题
- 目标应用可能没有AppCompat主题
- ✅ **必须**使用`ContextThemeWrapper`包装Context

**示例**：
```kotlin
val ctx = ModuleClassLoaderManager.getModuleContext()
val themedContext = ContextThemeWrapper(ctx, R.style.Theme_ZaralynStudyMask)

val appBarLayout = AppBarLayout(themedContext)
val toolbar = MaterialToolbar(themedContext)
val bottomNav = BottomNavigationView(themedContext)
```

### 资源命名规范

- 资源名称必须以字母开头
- 只能包含小写字母、数字和下划线
- 不能以数字开头
- 图标命名使用 `ic_` 前缀

### 颜色系统（Material Design 3）

- 主色：`#1E88E5`（天蓝色）
- 主色容器：`#D1E4FF`（浅蓝色）
- 表面色：`#FFFFFF`（白色）
- 表面容器：`#F3F3F3`（浅灰）
- 描边色：`#74777F`（灰色）
- 文本色：`#1D1B20`（深灰）
- 深色模式：`#121212`（深色背景）

### 尺寸规范

- 圆角：16dp
- 卡片高度：120dp（课本）、60dp（年级卡片）
- 工具栏高度：72dp
- 底部导航栏高度：56dp

### Kotlin 编码规范

- 使用安全类型转换 `as?` 而非强制转换 `as`
- 为 nullable 类型添加适当的 null 检查
- 使用 `companion object` 定义常量
- 遵循 Kotlin 官方代码风格（`kotlin.code.style=official`）
- 使用`WeakReference`防止内存泄漏

---

## 常见错误及避免方法

参考 `error.md` 文档获取详细错误记录。

**重点注意事项**：
- 不要在布局文件中使用未声明的命名空间
- 确保 DataBinding 已在 `build.gradle` 中启用
- 所有布局文件必须包含 `<layout>` 标签
- 资源文件名必须符合 Android 规范
- Xposed Hook 要使用正确的类加载器（`lpparam.classLoader`）
- 过滤系统进程，避免 Hook 系统关键组件
- 使用 Intent extras 而非 SharedPreferences 传递跨实例状态
- Fragment类必须使用模块ClassLoader加载
- Fragment的LayoutInflater必须使用模块Context创建
- Material组件必须使用ContextThemeWrapper包装Context

---

## 核心功能实现

### Xposed Hook 多层策略

#### 1. ActivityThread Hook 法（主要）
- **位置**：`hook/hooks/ActivityThreadHook.kt`
- 捕获 Activity 启动
- 支持 Android 9（4 参数）和 Android 10+（2 参数）双签名
- 使用 Intent extras 传递状态标志，避免死循环

#### 2. setContentView Hook 法
- **位置**：`hook/hooks/SetContentViewHook.kt`
- 拦截 Activity.setContentView(int) 和 setContentView(View)
- 在布局设置时进行替换

#### 3. PhoneWindow Hook 法
- **位置**：`hook/hooks/PhoneWindowHook.kt`
- 底层修改窗口布局
- 通过 PhoneWindow 的 mActivity 字段获取 Activity

#### 4. DecorView Hook 法
- **位置**：`hook/hooks/DecorViewHook.kt`
- 在视图附加到窗口时替换
- 使用 decorView.post 延迟执行 UI 替换

#### 5. LayoutInflater Hook 法
- **位置**：`hook/hooks/LayoutInflaterHook.kt`
- 劫持布局资源加载
- 当前仅用于日志记录，不执行替换

### 主 Activity 识别

**位置**：`detector/AppTypeDetector.kt`

使用多种方法识别目标应用的主 Activity：
1. PackageManager 查询（首选）
2. 类名以 MainActivity 结尾
3. Intent 的 ACTION_MAIN 和 CATEGORY_LAUNCHER
4. 缓存的类名（SharedPreferences）

### UI 替换策略

**位置**：`strategy/`

- 动态创建伪装界面（不依赖 XML 布局）
- 使用 View ID 定位（`android.R.id.custom` 和 `android.R.id.list`）
- 使用 SharedPreferences 保存状态
- 支持恢复原应用功能
- 完全自定义 UI 构建
- 根据应用类型选择最优策略

### 系统进程过滤

**位置**：`XposedHook.kt`

过滤以下包名：
- `android.*` 开头的包名
- 系统应用包名（systemui, settings, phone, mms 等）
- Xposed/LSPosed 框架包名
- 模块自身包名

### 隐蔽入口实现

#### 方法 1：F10 键
- **位置**：`event/handlers/F10KeyHandler.kt`
- Hook dispatchKeyEvent 方法
- 检测 F10 键或 MENU 键
- 使用回退机制（当前类 → 父类 Activity）

#### 方法 2：5 次主页键连击
- **位置**：`event/handlers/FiveTapHandler.kt`
- 在 2 秒内连续点击 5 次主页键
- 使用 SharedPreferences 记录点击次数和时间
- 超时自动重置计数器

### 日志功能
- 输出到 Android logcat（标签：ZaralynStudyMask）
- 同时输出到 XposedBridge.log
- 内置日志查看器界面（LogViewerActivity）
- 支持刷新和清除日志
- 命令行查看：`adb logcat ZaralynStudyMask`

### 年级选择
- 三个年级卡片：高一（Grade 10）、高二（Grade 11）、高三（Grade 12）
- 点击切换当前年级，使用 View ID 定位刷新内容
- 每个年级对应 4 本课本：
  - 高一：必修一、必修二、英语语法、词汇手册
  - 高二：必修三、必修四、阅读训练、写作指南
  - 高三：必修五、选修六、高考真题、冲刺复习

### Markdown 阅读
- 外部文件路径：`/sdcard/EnglishBook/`
- 内部测试文件：`assets/test.md`
- 支持的文件名：
  - `compulsory_1.md`、`compulsory_2.md` 等
- 文件不存在时使用内部测试文件
- 通过 Intent extras 传递标题和文件名参数

---

## 关键修复记录

### 1. ClassLoader隔离问题（8ef132a）
**问题**：createPackageContext()创建的Context的ClassLoader为空数组，无法加载androidx库
**原因**：模块ClassLoader与目标应用ClassLoader隔离
**修复**：创建ModuleClassLoaderManager统一管理模块ClassLoader和Context，使用WeakReference防止内存泄漏

### 2. Fragment类加载失败（4ab8884）
**问题**：Fragment显示空白
**原因**：Class.forName()使用宿主ClassLoader，找不到模块的Fragment类
**修复**：使用ModuleClassLoaderManager.getModuleClassLoader()显式加载Fragment类

### 3. Fragment LayoutInflater问题（db9dc14）
**问题**：Fragment显示空白
**原因**：Fragment的onCreateView使用传入的inflater（来自宿主ClassLoader）
**修复**：使用ModuleClassLoaderManager.getModuleContext()创建LayoutInflater

### 4. Material组件主题问题（8ef132a）
**问题**：雨见浏览器崩溃，Minecraft不替换UI
**原因**：Material组件（AppBarLayout、MaterialToolbar、BottomNavigationView）需要AppCompat主题，但目标应用没有
**修复**：使用ContextThemeWrapper包装Context，提供正确的主题

### 5. 未使用资源清理（fb5bb43）
**问题**：82个Lint警告，用户批评只修复了20个
**原因**：大量未使用的资源文件
**修复**：删除26个未使用的资源（颜色、字符串、尺寸、图标）

### 6. 静态Context泄漏
**问题**：静态Context可能导致内存泄漏
**原因**：直接持有Context引用
**修复**：使用WeakReference包裹Context

### 7. 硬编码/sdcard/路径
**问题**：硬编码的SD卡路径不兼容所有设备
**原因**：直接使用"/sdcard/"字符串
**修复**：使用Environment.getExternalStorageDirectory()

### 8. Android 11+包可见性
**问题**：Android 11+需要声明包可见性
**原因**：PackageManager API在Android 11+受限
**修复**：在AndroidManifest.xml中添加<queries>标签

### 9. DataBindingWithoutKapt警告
**问题**：DataBinding需要kapt插件
**原因**：缺少kotlin-kapt插件
**修复**：在build.gradle中添加kotlin-kapt插件

### 历史修复（旧版本）
- F10 死循环问题 - 改用 Intent extras 传递状态标志
- 年级选择不刷新问题 - 使用 View ID 进行精确定位
- dispatchKeyEvent Hook 崩溃 - 实现回退机制，添加实例检查
- performLaunchActivity 方法签名兼容性 - 实现双签名 Hook
- 模块通用化 - 移除包名限制，改用 PackageManager 动态查询
- 安全类型转换 - 全部改为安全转换 `as?` 并添加 null 检查

---

## 课本文件放置

将 Markdown 格式的课本文件放置在：
```
/sdcard/EnglishBook/
```

**支持的文件名**：
- `compulsory_1.md` - 必修一
- `compulsory_2.md` - 必修二
- `grammar.md` - 英语语法
- `vocabulary.md` - 词汇手册
- `compulsory_3.md` - 必修三
- `compulsory_4.md` - 必修四
- `reading.md` - 阅读训练
- `writing.md` - 写作指南
- `compulsory_5.md` - 必修五
- `elective_6.md` - 选修六
- `exam_papers.md` - 高考真题
- `sprint_review.md` - 冲刺复习

---

## 临时文件处理

以下目录是临时的，不应提交到 Git：
- `高中英语_icon/`（图标源文件）

已在 `.gitignore` 中排除。

---

## 版本兼容性

- Android Gradle Plugin 8.4.0 要求 Gradle 8.6+
- 升级 Gradle 时必须同时检查 AGP 版本兼容性
- Xposed API 版本：82
- Xposed 最小版本：93（AndroidManifest.xml 中声明）
- 最小 SDK：24
- 目标 SDK：34
- Java 版本：17
- kotlin-kapt 插件：必需

---

## 使用方法

### 作为 Xposed 模块使用
1. 安装 LSPosed 或 NPatch
2. 编译并安装 ZaralynStudyMask APK
3. 在 LSPosed/NPatch 中选择目标应用进行修补
4. 启动修补后的应用

### 查看日志
1. 打开 ZaralynStudyMask 应用
2. 点击"查看日志"按钮
3. 在日志查看器中查看模块运行日志
4. 使用 adb logcat ZaralynStudyMask 命令也可查看日志

### 进入原应用
- **方法 1**：按 F10 键或 MENU 键
- **方法 2**：在 2 秒内连击 5 次主页键

---

## NPatch 配置

`patch.json` 文件包含模块配置：
- 版本：1.0.0
- 目标包名：com.target.app（示例）
- 权限：READ_EXTERNAL_STORAGE、READ_MEDIA_IMAGES、READ_MEDIA_VIDEO、READ_MEDIA_AUDIO
- 修改类型：activity_replacement、permission_add

---

## 开发注意事项

### Hook 策略
- 所有 Hook 方法协同工作，确保最大兼容性
- Hook 顺序：ActivityThread → setContentView → PhoneWindow → DecorView → LayoutInflater
- 每个 Hook 都有独立的错误处理和日志记录
- 使用HookConfig配置启用/禁用Hook策略

### ClassLoader 管理
- 始终使用ModuleClassLoaderManager访问模块资源
- Fragment类必须使用模块ClassLoader加载
- Fragment的LayoutInflater必须使用模块Context创建
- Material组件必须使用ContextThemeWrapper包装Context

### 状态管理
- 使用 Intent extras 传递跨实例状态标志
- 使用 StateManager 统一管理状态
- 避免在 Hook 中修改 SharedPreferences 导致的状态混乱

### 安全性
- 过滤系统进程，避免 Hook 系统关键组件
- 使用安全类型转换，防止 ClassCastException
- 为所有 Hook 添加 try-catch 块，防止单个 Hook 失败影响整体
- 使用WeakReference防止内存泄漏

### 调试
- 所有关键操作都有日志输出
- 使用 LogViewerActivity 查看实时日志
- 支持通过 adb logcat 进行远程调试
- 使用分级日志系统（VERBOSE、DEBUG、INFO、WARN、ERROR）

### 性能优化
- 使用策略模式按需选择Hook策略
- 应用类型检测使用缓存避免重复检测
- 策略实例使用缓存避免重复创建
- 状态管理使用内存缓存和持久化存储

---

## 许可证

MIT License