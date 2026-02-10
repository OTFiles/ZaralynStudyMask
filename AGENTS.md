# ZaralynStudyMask 项目指南

## 项目概述

ZaralynStudyMask（简称 Zara）是一个 NPatch 模块，用于为 Android 应用添加"高中英语学习"的伪装界面。该模块会替换目标应用的 MainActivity，用户必须通过特定操作（连击5次主页键或按F10键）才能进入原应用。

**核心功能**：
- Material Design 3 风格的高中英语学习界面
- 年级选择（高一、高二、高三）
- 课本显示与 Markdown 阅读功能
- 课外学习模块（听力、口语、阅读、写作）
- 设置页面（夜间模式、自动播放等）
- 隐蔽入口（连击5次主页键或按F10键）

**技术栈**：
- Kotlin 1.9.20
- Android Gradle Plugin 8.4.0
- Gradle 8.6
- DataBinding
- Material Design 3
- Fragment 导航
- RecyclerView
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
- Zara 是 NPatch 模块，不是完整的 APK

### 编译方法

#### 使用 GitHub Actions（推荐）
1. 推送代码到 GitHub
2. Actions 会自动编译并生成 APK
3. 从 Actions artifacts 下载编译产物

#### 本地编译（仅当有编译环境时）
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

**编译产物位置**：
- Debug：`app/build/flows/outputs/apk/debug/app-debug.apk`
- Release：`app/build/flows/outputs/apk/release/app-release.apk`

### GitHub Actions 配置
- 工作流文件：`.github/workflows/build.yml`
- 使用 `gradle/gradle-build-action@v3` 处理 Gradle 下载和缓存
- 自动上传编译产物为 artifacts

---

## 开发约定

### 项目结构

```
app/src/main/
├── java/com/zaralyn/study/mask/
│   ├── MainActivity.kt          # 主界面入口
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
└── res/
    ├── drawable/               # 图标和背景资源
    ├── layout/                 # 布局文件（所有必须包含 `<layout>` 标签）
    ├── menu/                   # 菜单资源
    ├── mipmap-*/               # 应用图标（所有分辨率）
    └── values/                 # 值资源
        ├── strings.xml         # 字符串资源
        ├── colors.xml          # 颜色系统
        ├── themes.xml          # Material Design 3 主题
        └── dimens.xml          # 尺寸定义
```

### 编码规范

1. **DataBinding 使用**：
   - 所有使用 DataBinding 的布局文件必须包含 `<layout>` 根标签
   - 使用到的命名空间必须在根元素中声明（如 `xmlns:app`）
   - Binding 类会自动生成，命名格式为 `XxxBinding`（Xxx 为布局文件名，使用驼峰命名）

2. **资源命名规范**：
   - 资源名称必须以字母开头
   - 只能包含小写字母、数字和下划线
   - 不能以数字开头
   - 图标命名使用 `ic_` 前缀

3. **颜色系统**：
   - 主色：`#1E88E5`（天蓝色）
   - 主色容器：`#D1E4FF`（浅蓝色）
   - 表面色：`#FFFFFF`（白色）
   - 表面容器：`#F3F3F3`（浅灰）
   - 描边色：`#74777F`（灰色）
   - 文本色：`#1D1B20`（深灰）

4. **尺寸规范**：
   - 圆角：16dp
   - 卡片高度：120dp（课本）、60dp（年级卡片）
   - 工具栏高度：72dp
   - 底部导航栏高度：56dp

### 常见错误及避免方法

参考 `error.md` 文档获取详细错误记录。

**重点注意事项**：
- 不要在布局文件中使用未声明的命名空间
- 确保 DataBinding 已在 `build.gradle` 中启用
- 所有布局文件必须包含 `<layout>` 标签
- 资源文件名必须符合 Android 规范

---

## 核心功能实现

### 年级选择
- 三个年级卡片：高一（Grade 10）、高二（Grade 11）、高三（Grade 12）
- 每个年级对应 4 本课本：
  - 高一：必修一、必修二、英语语法、词汇手册
  - 高二：必修三、必修四、阅读训练、写作指南
  - 高三：必修五、选修六、高考真题、冲刺复习

### 隐蔽入口
- **方法 1**：按 F10 键
- **方法 2**：在 2 秒内连击 5 次主页键

### Markdown 阅读
- 外部文件路径：`/sdcard/EnglishBook/`
- 内部测试文件：`assets/test.md`
- 支持的文件名：
  - `compulsory_1.md`、`compulsory_2.md` 等
- 文件不存在时使用内部测试文件

---

## 课本文件放置

将 Markdown 格式的课本文件放置在：
```
/sdcard/EnglishBook/
```

---

## 临时文件处理

以下目录是临时的，不应提交到 Git：
- `高中英语_icon/`（图标源文件）

已在 `.gitignore` 中排除。

---

## 版本兼容性

- Android Gradle Plugin 8.4.0 要求 Gradle 8.6+
- 升级 Gradle 时必须同时检查 AGP 版本兼容性

---

## 许可证

MIT License