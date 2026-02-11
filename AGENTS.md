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
- 工作流文件：`.github/workflows/build.yml`
- 使用 `gradle/gradle-build-action@v3` 处理 Gradle 下载和缓存
- 使用 JDK 17 (temurin distribution)
- 自动上传编译产物为 artifacts

---

## 开发约定

### 项目结构

```
app/src/main/
├── java/com/zaralyn/study/mask/
│   ├── MainActivity.kt          # 主界面（日志查看器入口）
│   ├── LogViewerActivity.kt    # 日志查看界面
│   ├── XposedHook.kt            # Xposed 模块入口（核心 Hook 逻辑）
│   ├── ZaralynMainActivity.kt  # 伪装界面（使用 DataBinding）
│   ├── ReaderActivity.kt        # Markdown 阅读界面
│   ├── adapter/                 # RecyclerView 适配器
│   │   ├── BookAdapter.kt       # 课本列表适配器
│   │   ├── ExploreAdapter.kt    # 课外学习适配器
│   │   ├── GradeAdapter.kt      # 年级选择适配器
│   │   └── SettingsAdapter.kt   # 设置页面适配器
│   ├── data/                    # 数据源
│   │   ├── BookData.kt         # 课本数据
│   │   ├── ExploreData.kt      # 课外学习数据
│   │   └── SettingData.kt       # 设置数据
│   ├── model/                   # 数据模型
│   │   ├── Book.kt              # 课本模型
│   │   ├── Grade.kt             # 年级模型
│   │   ├── ExploreItem.kt       # 课外学习项模型
│   │   └── SettingItem.kt       # 设置项模型
│   └── ui/fragments/            # 界面片段
│       ├── HomeFragment.kt      # 主页片段
│       ├── ExploreFragment.kt   # 课外片段
│       └── SettingsFragment.kt  # 设置片段
├── assets/
│   ├── xposed_init              # Xposed 模块入口声明
│   └── test.md                   # 内部测试文件
└── res/
    ├── drawable/               # 图标和背景资源
    ├── layout/                 # 布局文件（使用 DataBinding）
    ├── menu/                   # 菜单资源
    ├── mipmap-*/               # 应用图标（所有分辨率）
    └── values/                 # 值资源
        ├── strings.xml         # 字符串资源
        ├── colors.xml          # 颜色系统
        ├── themes.xml          # Material Design 3 主题
        └── dimens.xml          # 尺寸定义
```

### 编码规范

1. **Xposed 模块开发**：
   - 必须实现 `IXposedHookLoadPackage` 接口
   - 在 `assets/xposed_init` 中声明模块入口类：`com.zaralyn.study.mask.XposedHook`
   - 在 `AndroidManifest.xml` 中添加 Xposed 元数据
   - 使用 `XposedHelpers` 进行 Hook 操作
   - 日志输出到 logcat（标签：ZaralynStudyMask）和 XposedBridge.log
   - 过滤系统关键进程，避免影响系统稳定性

2. **DataBinding 使用**：
   - 所有使用 DataBinding 的布局文件必须包含 `<layout>` 根标签
   - 使用到的命名空间必须在根元素中声明（如 `xmlns:app`）
   - Binding 类会自动生成，命名格式为 `XxxBinding`（Xxx 为布局文件名，使用驼峰命名）
   - 在 `build.gradle` 中启用：`buildFeatures { dataBinding = true }`

3. **资源命名规范**：
   - 资源名称必须以字母开头
   - 只能包含小写字母、数字和下划线
   - 不能以数字开头
   - 图标命名使用 `ic_` 前缀

4. **颜色系统（Material Design 3）**：
   - 主色：`#1E88E5`（天蓝色）
   - 主色容器：`#D1E4FF`（浅蓝色）
   - 表面色：`#FFFFFF`（白色）
   - 表面容器：`#F3F3F3`（浅灰）
   - 描边色：`#74777F`（灰色）
   - 文本色：`#1D1B20`（深灰）
   - 深色模式：`#121212`（深色背景）

5. **尺寸规范**：
   - 圆角：16dp
   - 卡片高度：120dp（课本）、60dp（年级卡片）
   - 工具栏高度：72dp
   - 底部导航栏高度：56dp

6. **Kotlin 编码规范**：
   - 使用安全类型转换 `as?` 而非强制转换 `as`
   - 为 nullable 类型添加适当的 null 检查
   - 使用 `companion object` 定义常量
   - 遵循 Kotlin 官方代码风格（`kotlin.code.style=official`）

### 常见错误及避免方法

参考 `error.md` 文档获取详细错误记录。

**重点注意事项**：
- 不要在布局文件中使用未声明的命名空间
- 确保 DataBinding 已在 `build.gradle` 中启用
- 所有布局文件必须包含 `<layout>` 标签
- 资源文件名必须符合 Android 规范
- Xposed Hook 要使用正确的类加载器（`lpparam.classLoader`）
- 过滤系统进程，避免 Hook 系统关键组件
- 使用 Intent extras 而非 SharedPreferences 传递跨实例状态

---

## 核心功能实现

### Xposed Hook 多层策略

XposedHook.kt 实现了5层 Hook 方案来确保最大兼容性：

#### 1. ActivityThread Hook 法（主要）
```kotlin
private fun hookActivityThread(classLoader: ClassLoader)
```
- 捕获 Activity 启动
- 支持 Android 9（4 参数）和 Android 10+（2 参数）双签名
- 使用 Intent extras 传递状态标志，避免死循环

#### 2. setContentView Hook 法
```kotlin
private fun hookSetContentView(classLoader: ClassLoader)
```
- 拦截 Activity.setContentView(int) 和 setContentView(View)
- 在布局设置时进行替换

#### 3. PhoneWindow Hook 法
```kotlin
private fun hookPhoneWindow(classLoader: ClassLoader)
```
- 底层修改窗口布局
- 通过 PhoneWindow 的 mActivity 字段获取 Activity

#### 4. DecorView Hook 法
```kotlin
private fun hookDecorView(classLoader: ClassLoader)
```
- 在视图附加到窗口时替换
- 使用 decorView.post 延迟执行 UI 替换

#### 5. LayoutInflater Hook 法
```kotlin
private fun hookLayoutInflater(classLoader: ClassLoader)
```
- 劫持布局资源加载
- 当前仅用于日志记录，不执行替换

### 主 Activity 识别
使用多种方法识别目标应用的主 Activity：
1. PackageManager 查询（首选）
2. 类名以 MainActivity 结尾
3. Intent 的 ACTION_MAIN 和 CATEGORY_LAUNCHER
4. 缓存的类名（SharedPreferences）

```kotlin
private fun findMainActivityClass(classLoader: ClassLoader, packageName: String): String?
```

### UI 替换策略
- 动态创建伪装界面（不依赖 XML 布局）
- 使用 View ID 定位（`android.R.id.custom` 和 `android.R.id.list`）
- 使用 SharedPreferences 保存状态
- 支持恢复原应用功能
- 完全自定义 UI 构建

### 系统进程过滤
```kotlin
private fun isSystemProcess(packageName: String): Boolean
```
过滤以下包名：
- `android.*` 开头的包名
- 系统应用包名（systemui, settings, phone, mms 等）
- Xposed/LSPosed 框架包名
- 模块自身包名

### 隐蔽入口实现

#### 方法 1：F10 键
```kotlin
private fun setupKeyListener(activity: Activity, prefs: SharedPreferences)
```
- Hook dispatchKeyEvent 方法
- 检测 F10 键或 MENU 键
- 使用回退机制（当前类 → 父类 Activity）

#### 方法 2：5 次主页键连击
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

### 1. F10 死循环问题（c835668）
**问题**：按 F10 后应用无限重启，一直显示伪装界面
**原因**：`KEY_SHOW_ORIGINAL` 在 performLaunchActivity Hook 中被错误重置
**修复**：改用 Intent extras 传递状态标志

### 2. 年级选择不刷新问题（c835668）
**问题**：点击"高二"后课本列表不更新
**原因**：视图层级遍历不可靠，类型转换错误
**修复**：使用 View ID（`android.R.id.custom` 和 `android.R.id.list`）进行精确定位

### 3. dispatchKeyEvent Hook 崩溃（827a0bd）
**问题**：NoSuchMethodError：MainActivity 没有重写 dispatchKeyEvent
**修复**：实现回退机制（当前类 → 父类 Activity），添加实例检查

### 4. performLaunchActivity 方法签名兼容性（d3d67a8）
**问题**：Android 9 和 Android 10+ 方法签名不同
**修复**：实现双签名 Hook（2 参数和 4 参数）

### 5. 模块通用化（d3d67a8）
**问题**：硬编码的 TARGET_PACKAGE 限制模块只能用于一个应用
**修复**：移除包名限制，改用 PackageManager 动态查询

### 6. 安全类型转换
**问题**：多处使用强制转换 `as` 导致崩溃
**修复**：全部改为安全转换 `as?` 并添加 null 检查

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
- 所有 5 个 Hook 方法协同工作，确保最大兼容性
- Hook 顺序：ActivityThread → setContentView → PhoneWindow → DecorView → LayoutInflater
- 每个 Hook 都有独立的错误处理和日志记录

### 状态管理
- 使用 Intent extras 传递跨实例状态标志
- 使用 SharedPreferences 持久化存储（年级选择、UI 替换标志等）
- 避免在 Hook 中修改 SharedPreferences 导致的状态混乱

### 安全性
- 过滤系统进程，避免 Hook 系统关键组件
- 使用安全类型转换，防止 ClassCastException
- 为所有 Hook 添加 try-catch 块，防止单个 Hook 失败影响整体

### 调试
- 所有关键操作都有日志输出
- 使用 LogViewerActivity 查看实时日志
- 支持通过 adb logcat 进行远程调试

---

## 许可证

MIT License