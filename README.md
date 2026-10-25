<div align="center">

# go2servicemode

**三星 ServiceMode 启动工具**

把一条受支持的 `keyString` 发给三星 ServiceMode，打开工程菜单。不提供 root 通道、shell，或供其他应用使用的特权接口。

[![License](https://img.shields.io/badge/license-MIT-3DA639?style=flat-square)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android%2012%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
[![Version](https://img.shields.io/badge/version-V0.0.2-2F6BFF?style=flat-square)](https://github.com/lots0o0/go2servicemode/releases)
![ABI](https://img.shields.io/badge/ABI-arm64--v8a-6B7280?style=flat-square)

</div>

---

## 它解决什么

三星的 `ServiceModeApp` 声明为 `exported="false"`，并且运行在电话进程（uid 1001）里，只有 uid 0 能拉起它。所以在非 root 状态下，普通应用和拨号暗码都进不去这个菜单。

这个工具的做法是：用一次**纯内存的临时提权**换取调用权限，把 `keyString` 发出去，然后立刻收尾。没有常驻 root，没有授权面板，没有开机自启。

## 特性

- **两个事务**：① 建立临时 su ② 发送启动指令。分开是为了任一步失败都能单独定位
- **不碰固件**：不写 `/system`、`/vendor`、`/product`、`/odm`、`/system_ext`、`prism`、`optics`；不碰 `boot`、`vbmeta`、`efs`、`param`、`modem`；不关 verity
- **不对外暴露 su**：应用侧看不到 su，特权调用只在 root 助手内部发生
- **日志落盘**：每次运行一个文件，全屏查看器可读、导出、删除
- **可核对**：关于页显示构建日期与**当前安装包自身的 SHA-256**
- **液态玻璃界面**：基于 `kyant0/backdrop` 的 highlight / innerShadow / lens

## 版本

| 版本 | 构建日期 | APK SHA-256 | 证书 SHA-256 |
|---|---|---|---|
| **V0.0.2** | 2026-10-25 19:07 | `b700479909bad3b4d2e4bf9617701511d0b7580b8567beb316eb5f240db7ebf4` | `176f2588f9b5b009f5dff42b7cd497f9d8462e74a68deb7a73a9b5ed51cbc9bd` |
| V0.0.1 | 2026-10-25 18:47 | `5e6b60436a98b832b8f691919c9cdbb855567fe8660f9d6381da7726c9e811a0` | `176f2588f9b5b009f5dff42b7cd497f9d8462e74a68deb7a73a9b5ed51cbc9bd` |

安装后在「关于」页右上角可看到 `V0.0.2`、构建日期与 APK 的 SHA-256 前 16 位，可与上表核对。

> 升级要求 `versionCode` 递增且签名证书一致。换签名等于换应用，无法覆盖安装。

## 安装

| 项 | 要求 |
|---|---|
| 系统 | Android 12 以上（minSdk 31），在 Android 17 / One UI 上验证 |
| 架构 | arm64-v8a |
| 前提 | 设备仍存在 DirtyFrag（CVE-2026-43284）提权链 |

从 Releases 下载 `app-release.apk` 安装即可。

## 使用

1. 打开应用，点「建立临时 su」，等状态变成**已就绪**
2. 点「GCF 菜单」或「NAS / 主菜单」
3. 「日志」页可以查看每次运行的结果；点「查看」进入全屏日志

失败时按这个顺序排查：载荷日志 → `/system/bin/su` 是否存在 → 助手心跳 → 助手输出的 `am` 结果 → 是否在同一次开机内重复运行过。

## 限制

| 项 | 说明 |
|---|---|
| 次数 | **一次开机只成功一次**，重复运行会走短路分支 |
| 生命周期 | 重启后内核模块、临时 su、SELinux 状态全部消失 |
| 系统更新 | 漏洞修复后失效 |
| 通道延迟 | 助手轮询信箱，约 200 ms 量级 |

<details>
<summary><b>工作原理（点开）</b></summary>

```
事务 1  App → IpSec 建链 → 引擎提权 → payload（u:r:vendor_modprobe:s0）
        payload：落 ksud → bind-mount 到 /system/bin/atrace
                 → late-load --allow-shell → 启动常驻助手
        App：等 su 出现 → 等助手心跳 → Ready

事务 2  App：写 files/tx2/req（序号 + am start 命令）
        助手：读变化 → su -c "<命令>" → 追加 out.log
        App：读增量 → 日志页展示
```

应用进程看不到 su：ksud 的 bind-mount 位于应用不共享的 mount namespace，且内核授权门只放行 uid 2000。因此应用侧不持有任何特权，只与 root 助手通过应用自己的目录交换文本。

</details>

## 文档

| 文件 | 内容 |
|---|---|
| [第三方与许可](docs/第三方与许可.md) | 二进制来源、依赖许可、免责 |

## 构建

```bash
./gradlew assembleRelease
```

需要 JDK 17 与 Android SDK 37。签名材料从 `keystore.properties`（不进版本库）或 `GO2SM_*` 环境变量读取；没有时构建会回退 debug 签名并给出警告。

## 支持

如果它帮你省下了几步操作，可以请作者喝杯咖啡。这份支持会用在多版本固件包的核查和设备上。

<table>
<tr>
<td align="center" width="180">
<img src="docs/assets/reward-wechat.png" width="150" alt="微信赞赏码"><br>
<sub>微信赞赏码</sub>
</td>
<td valign="top">
<b>Ko-fi</b> · <a href="https://ko-fi.com/lots0">https://ko-fi.com/lots0</a><br><br>
<b>支付宝</b> · 357459415@qq.com<br>
<sub>转账时备注「赞赏」即可</sub>
</td>
</tr>
</table>

## 许可

本仓库代码为 **MIT**，见 [LICENSE](LICENSE)。

仓库内含两个预编译二进制（`ksud-real`、`libexp.so`），来源与再分发条件见[第三方与许可](docs/第三方与许可.md)。

## 致谢

**项目引用**

- **lots0** — 项目维护与多版本固件包审查
- **diabl0w** — DFRoot / DirtyFrag
- **snothin** — GhostSam，三星 KDP / DEFEX 适配
- **tiann** — KernelSU

**文档与参考项目**

- **kuuky29** — UniRoot
- **xorpix / BuSung-dev** — Root My Galaxy 项目及文档
- **lots0 私有仓库**（未公开）— 版本包归档与审查记录

**特别感谢**

- **Gary** — 设备提供方。因对包审查的判断失误导致 D2 锁定，拆机维修解决；这也是本工具不对外暴露 su 能力的初衷。

---

<div align="center"><sub>本工具按“原样”提供，不附带任何担保；使用前请自行确认风险与当地法规。</sub></div>
