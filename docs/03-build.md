# 构建

## 环境

| 项 | 版本 |
|---|---|
| JDK | 17 |
| Android SDK | compileSdk 37（minorApiLevel 2），minSdk 31，targetSdk 37 |
| Gradle | 9.7.1（wrapper） |
| AGP | 9.4.0（自带 Kotlin 支持，不要再应用 `org.jetbrains.kotlin.android`） |
| Kotlin Compose 插件 | 2.4.10 |

`local.properties` 只写本机 SDK 路径，**不要提交**（已在 `.gitignore` 中）。

## 命令

```bash
# 调试包（可在设备上用 run-as 检查应用私有目录）
./gradlew assembleDebug

# 发布包
./gradlew assembleRelease
```

产物：

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 签名

`release` 目前复用 `debug` 签名，方便自签名分发。要正式分发时，在 `app/build.gradle.kts` 里换成自己的 keystore：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("...")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

密钥不要进仓库。

## 构建标记

每次构建都会把时间写入 `BuildConfig.BUILD_STAMP`，显示在「关于」页右上角。两个版本界面可能完全一样，构建标记是唯一能确认"装的是不是新包"的东西。

## 依赖

```kotlin
implementation("androidx.activity:activity-compose:1.13.0")
implementation("androidx.compose.foundation:foundation:1.12.0")
implementation("androidx.compose.ui:ui:1.12.0")
implementation("androidx.compose.runtime:runtime:1.12.0")
implementation("io.github.kyant0:backdrop:2.0.1")   // 液态玻璃
implementation("io.github.kyant0:shapes:1.2.1")     // Capsule
```

## 原生库的打包方式

```kotlin
packaging { jniLibs { useLegacyPackaging = true } }
```

引擎需要把 `libexp.so` 从 APK 里**解压到磁盘**再加载，直接页映射的方式不适用。

## 仓库内的二进制

仓库包含两个预编译二进制，构建时会被打进 APK：

| 文件 | 说明 |
|---|---|
| `app/src/main/jniLibs/arm64-v8a/libexp.so` | 提权引擎（已按本工程路径打过补丁） |
| `app/src/main/assets/df/ksud-real` | KernelSU 用户态程序 |

来源、许可与再分发注意见 `docs/05-legal.md`。如果只关心界面与协议，可以不含这两个文件完成构建，但功能不可用。

## 常见构建问题

| 现象 | 原因 |
|---|---|
| `Unresolved reference 'java'`（kts 里用 `java.text.*`） | Kotlin DSL 中 `java` 被 Gradle 扩展遮蔽，改为在文件头 `import java.text.SimpleDateFormat` |
| 应用启动崩溃、`IpSecService` 权限异常 | 清单缺少 `android.permission.INTERNET` |
| `INSTALL_FAILED_REJECTED_BY_BUILDTYPE`（-3001） | 设备侧构建类型校验，与本工程无关，见 `docs/01-background-and-qa.md` |
