# go2servicemode

把一条受支持的 `keyString` 发送给三星 ServiceMode，用于打开工程菜单。

不为任何应用提供常驻 root，不提供 shell，不写系统分区。

---

## 它做什么

在三星设备上，`ServiceModeApp` 声明为 `exported="false"` 且运行在电话进程里，只有 uid 0 能拉起它。这个工具因此需要一次**临时的、仅存在于内存的**提权，用完即止：

1. **事务 1**：运行提权引擎，落地临时 su，并留下一个常驻助手（root）负责后续的特权调用。
2. **事务 2**：把 `am start ... --es keyString <白名单值>` 交给常驻助手执行。

两步分开，是为了任一步失败都能单独定位——合成一步时，"载荷没跑起来"和"`am` 参数不对"表现完全一样。

## 它不做什么

- 不向第三方应用暴露 su / shell
- 不写 `/system`、`/vendor`、`/product`、`/odm`、`/system_ext`、`prism`、`optics`
- 不触碰 `boot`、`vbmeta`、`efs`、`param`、`modem`，不关闭 verity
- 无开机自启，无后台常驻服务
- 不启动、也不依赖 KernelSU Manager

所有写操作只发生在应用自己的 data 目录与 `/data/local/tmp`。

## 要求与限制

| 项 | 说明 |
|---|---|
| 系统 | Android 12+（minSdk 31），在 Android 17 / One UI 上开发与验证 |
| 架构 | arm64-v8a |
| 生效条件 | 设备仍存在 DirtyFrag（CVE-2026-43284）提权链；系统更新后可能失效 |
| 次数 | **一次开机只成功一次**。内核模块加载后，同一 boot 内重复运行会走短路分支 |
| 生命周期 | 纯内存态，重启后 `/system/bin/su`、内核模块、Permissive 状态全部消失 |

## 用法

1. 安装并打开应用。
2. 点「建立临时 su」，等待状态变为**已就绪**。
3. 点下面两个菜单项之一：
   - `GCF 菜单` — keyString `4238378`
   - `NAS / 主菜单` — keyString `27663368378`
4. 每次运行的摘要与原始输出都会落盘，「日志」页可查看、导出、删除。

若第 2 步失败，按日志页的原始输出定位，顺序是：载荷日志 → `/system/bin/su` 是否存在 → 助手心跳 → 助手输出的 `am` 结果。详见 `docs/01-background-and-qa.md` 第五节。

## 构建

```bash
./gradlew assembleDebug      # 需要 JDK 17 与 Android SDK 37
```

完整说明（依赖、签名、仓库内二进制）见 [`docs/03-build.md`](docs/03-build.md)。

## 文档

| 文件 | 内容 |
|---|---|
| [`docs/01-background-and-qa.md`](docs/01-background-and-qa.md) | 前置调查与实践记录（问答式，含证据与已排除方案） |
| [`docs/02-architecture.md`](docs/02-architecture.md) | 分层、两条链路、状态机、关键文件 |
| [`docs/03-build.md`](docs/03-build.md) | 构建、签名、打包、常见问题 |
| [`docs/04-ui-design.md`](docs/04-ui-design.md) | 界面与文案规范（权重阶梯、状态色、折叠政策、文案对照） |
| [`docs/05-legal.md`](docs/05-legal.md) | 许可、第三方组件、二进制来源 |

## 目录

```
app/src/main/kotlin/online/usim/go2servicemode/   UI、事务、日志
app/src/main/java/df/root/                        引擎桥（IPsec + 原生调用）
app/src/main/assets/payload.sh                    事务 1：设备侧脚本
app/src/main/assets/helper.sh                     事务 2：常驻助手
app/src/main/jniLibs/arm64-v8a/libexp.so          提权引擎
tools/patch_libexp_path.py                        引擎内路径补丁（可复现）
docs/                                             文档
```

## 致谢

- **diabl0w** — DFRoot / DirtyFrag 漏洞引擎
- **snothin** — GhostSam，三星 KDP / DEFEX 适配
- **tiann** — KernelSU
- **Kyant (kyant0)** — androidliquidglass / backdrop，本界面的液态玻璃效果
- **kuuky29** — UniRoot
- **xorpix / BuSung-dev** — Root My Galaxy 项目及文档

## 许可

本仓库代码为 MIT，见 [`LICENSE`](LICENSE)。仓库内预编译二进制的来源与再分发注意见 [`docs/05-legal.md`](docs/05-legal.md)。

本项目用于安全研究与自有设备上的实验，按“原样”提供，不附带任何担保。
