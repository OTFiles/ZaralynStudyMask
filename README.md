# ZaralynStudyMask（Zara）

Zara 是一个 NPatch 模块，为软件刷入它之后可以为 APP 添加一层伪装层，它将修改软件的入口（主活动，Main Activity）为一个特定的页面。

## 功能特性

- **Material Design 3 界面** - 现代化的 UI 设计
- **年级选择** - 支持高一、高二、高三三个年级
- **课本显示与阅读** - 根据年级显示对应的课本
- **课外学习功能** - 听力、口语、阅读、写作模块
- **设置页面** - 夜间模式、自动播放等功能
- **隐蔽入口** - 连击 5 次主页键或按 F10 键进入原应用

## 使用说明

### 安装

1. 使用 GitHub Actions 编译生成 APK
2. 将 APK 安装到设备上
3. NPatch 会自动修改目标应用的入口

### 进入原应用

- **方法 1**：按 F10 键
- **方法 2**：连击 5 次主页键（2秒内）

### 添加课本内容

将 Markdown 格式的课本文件放置在以下目录：
```
/sdcard/EnglishBook/
```

支持的文件名：
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

## 开发环境

- Android SDK 34
- Kotlin 1.9.20
- Gradle 8.3
- Material Design 3

## 编译

### 使用 GitHub Actions

推送代码到 GitHub 后，Actions 会自动编译并生成 APK 文件。

### 本地编译

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

编译产物位置：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 项目结构

```
app/
├── src/main/
│   ├── java/com/zaralyn/study/mask/
│   │   ├── MainActivity.kt          # 主界面
│   │   ├── ReaderActivity.kt        # 阅读界面
│   │   ├── adapter/                 # 适配器
│   │   ├── data/                    # 数据源
│   │   ├── model/                   # 数据模型
│   │   └── ui/fragments/            # 界面片段
│   ├── res/
│   │   ├── drawable/               # 图标资源
│   │   ├── layout/                 # 布局文件
│   │   └── values/                 # 值资源
│   └── assets/                     # 资产文件
└── build.gradle
```

## 注意事项

1. 当前在 termux 环境无法进行编译操作
2. 编译全部使用 GitHub Actions
3. Zara 是一个 NPatch 模块，而不是一个完整的 APK
4. 需要配置正确的目标应用包名才能正常工作

## 许可证

MIT License