# ZaralynStudyMask 项目开发错误记录

## 一、编译配置错误

### 1. Gradle 版本不匹配
**错误**：`Minimum supported Gradle version is 8.4. Current version is 8.3`

**原因**：Android Gradle Plugin 8.3.0 要求 Gradle 版本至少为 8.4，但项目使用的是 8.3

**解决**：
- 第一次升级：Gradle 8.3 → 8.4
- 第二次升级：Gradle 8.4 → 8.6（因为 AGP 8.4.0 要求 Gradle 8.6+）

**影响文件**：
- `.github/workflows/build.yml`
- `gradle/wrapper/gradle-wrapper.properties`
- `build.gradle`

---

### 2. Gradle Wrapper 文件缺失
**错误**：`Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

**原因**：缺少 `gradle-wrapper.jar` 文件

**解决**：使用 GitHub Actions 的 `gradle/gradle-build-action@v3` 并指定 gradle 版本，替代使用本地 `./gradlew` 脚本

---

## 二、资源文件错误

### 3. 图标文件命名不符合规范
**错误**：`The resource name must start with a letter`

**原因**：从 高中英语_icon 目录复制的图标文件名不以字母开头：
- `66.png`
- `-j.png`
- `Os.png`
- `qc.png`

**解决**：
1. 删除所有无效的图标文件
2. 将有效的图标重命名为符合 Android 规范的名称：
   - `66.png` → `ic_launcher.png`
   - `Os.png` → `ic_launcher_round.png`
3. 复制到所有分辨率的 mipmap 目录

---

### 4. ic_launcher_background.xml 格式错误
**错误**：`Can't determine type for tag '<path android:fillColor="#1E88E5" android:pathData="M0,0h108v108h-108z"/>'`

**原因**：文件中包含矢量图形代码，但放在了 values 目录中应该是颜色值

**解决**：将其修改为纯颜色值
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#1E88E5</color>
</resources>
```

---

## 三、DataBinding 相关错误

### 5. DataBinding 未启用
**错误**：`Unresolved reference: databinding`、`Unresolved reference: ActivityMainBinding` 等

**原因**：未在 build.gradle 中启用 DataBinding 功能

**解决**：在 `app/build.gradle` 的 `android {}` 块中添加：
```gradle
buildFeatures {
    dataBinding = true
}
```

---

### 6. buildFeatures 位置错误
**错误**：`Could not find method buildFeatures() for arguments [...] on project ':app'`

**原因**：`buildFeatures` 块被错误地放在了 `android {}` 块的外面

**解决**：将 `buildFeatures` 移到 `android {}` 块内部

---

### 7. 布局文件缺少 layout 标签
**错误**：`Unresolved reference: databinding`（启用 DataBinding 后仍然出现）

**原因**：布局文件没有包含 `<layout>` 标签，导致 DataBinding 无法生成 Binding 类

**解决**：为所有使用 DataBinding 的布局文件添加 `<layout>` 根标签：
- `activity_main.xml`
- `fragment_home.xml`
- `fragment_explore.xml`
- `fragment_settings.xml`
- `activity_reader.xml`
- `item_grade_card.xml`
- `item_book_card.xml`
- `item_explore_card.xml`
- `item_setting.xml`

---

### 8. 布局文件缺少 xmlns:app 命名空间
**错误**：`AttributePrefixUnbound?androidx.cardview.widget.CardView&app:cardBackgroundColor&app`

**原因**：布局文件使用了 `app:` 命名空间的属性（如 `app:cardBackgroundColor`），但没有在根元素中声明

**解决**：在 `<layout>` 标签中添加 `xmlns:app="http://schemas.android.com/apk/res-auto"`
```xml
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
```

---

## 四、用户重要需求

### 9. 软件名称修改
**需求**：将软件名改为"高中英语"

**实现**：修改 `app/src/main/res/values/strings.xml` 中的 `app_name` 值
```xml
<string name="app_name">高中英语</string>
```

---

### 10. 应用图标替换
**需求**：使用 `@/data/data/com.termux/files/home/高中英语_icon/` 目录下的图标作为应用图标

**实现**：
1. 将 `66.png` 重命名为 `ic_launcher.png`（应用图标）
2. 将 `Os.png` 重命名为 `ic_launcher_round.png`（圆形图标）
3. 复制到所有分辨率目录：
   - `mipmap-mdpi/`
   - `mipmap-hdpi/`
   - `mipmap-xhdpi/`
   - `mipmap-xxhdpi/`
   - `mipmap-xxxhdpi/`

---

## 五、开发注意事项

### 11. Android 资源命名规范
- 资源名称必须以字母开头
- 只能包含小写字母、数字和下划线
- 不能以数字开头

### 12. DataBinding 使用规范
- 布局文件必须包含 `<layout>` 根标签
- 使用到的命名空间必须在根元素中声明
- DataBinding 会自动生成 `XxxBinding` 类（Xxx 为布局文件名，使用驼峰命名）

### 13. Gradle 版本兼容性
- Android Gradle Plugin 8.3.0 要求 Gradle 8.4+
- Android Gradle Plugin 8.4.0 要求 Gradle 8.6+
- 升级 Gradle 时需要同时检查 AGP 版本兼容性

### 14. GitHub Actions 配置
- 在 termux 环境下无法编译，必须使用 GitHub Actions
- 使用 `gradle/gradle-build-action@v3` 可以自动处理 Gradle 下载和缓存
- 编译后的 APK 会自动上传为 artifact

### 15. 临时文件处理
- `高中英语_icon/` 目录是临时的，需要添加到 `.gitignore` 中
- 不要提交临时文件到 Git 仓库

---

## 六、项目结构总结

```
ZaralynStudyMask/
├── .github/workflows/build.yml    # GitHub Actions 编译配置
├── app/
│   ├── build.gradle               # 应用级构建配置
│   ├── proguard-rules.pro          # ProGuard 规则
│   └── src/main/
│       ├── AndroidManifest.xml     # 应用清单
│       ├── assets/test.md         # 测试 Markdown 文件
│       ├── java/com/zaralyn/study/mask/
│       │   ├── MainActivity.kt     # 主界面
│       │   ├── ReaderActivity.kt   # 阅读界面
│       │   ├── adapter/           # RecyclerView 适配器
│       │   ├── data/               # 数据源
│       │   ├── model/              # 数据模型
│       │   └── ui/fragments/      # 界面片段
│       └── res/
│           ├── drawable/           # 图标和背景
│           ├── layout/             # 布局文件（所有都包含 layout 标签）
│           ├── menu/               # 菜单资源
│           ├── mipmap-*/           # 应用图标
│           └── values/             # 值资源
├── build.gradle                    # 项目级构建配置
├── gradle/wrapper/                 # Gradle Wrapper
├── patch.json                      # NPatch 配置
└── settings.gradle                 # Gradle 设置
```

---

## 七、功能总结

### 已实现功能
1. Material Design 3 界面（天蓝色主题）
2. 年级选择（高一、高二、高三）
3. 课本显示与切换
4. Markdown 阅读功能
5. 课外学习功能（听力、口语、阅读、写作）
6. 设置页面（夜间模式、自动播放等）
7. 隐蔽入口（连击5次主页键或按F10键）
8. GitHub Actions 自动编译

### 技术栈
- Kotlin
- Android Gradle Plugin 8.4.0
- Gradle 8.6
- DataBinding
- Material Design 3
- Fragment 导航
- RecyclerView
