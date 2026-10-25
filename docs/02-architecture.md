# 架构

这个工程的复杂度几乎全部来自一个约束：**应用进程看不到 su**。其余设计都是它的推论。

## 分层

```
┌──────────────────────────────────────────────────────────┐
│ UI 层        ui/MainScreen · LogScreen · AboutScreen     │
│              只做展示与触发，不含任何特权逻辑             │
├──────────────────────────────────────────────────────────┤
│ 事务层       Engine（事务 1） · MenuTx（事务 2）          │
│              各自只有一个职责，互不 import                │
├──────────────────────────────────────────────────────────┤
│ 模型层       LogStore（落盘与回填） · RunService（前台）  │
├──────────────────────────────────────────────────────────┤
│ 桥层         df/root/DFBridge.java                        │
│              IPsec 封装 socket · 加载 libexp · 落盘载荷   │
├──────────────────────────────────────────────────────────┤
│ 设备侧       assets/payload.sh（事务 1，root）            │
│              assets/helper.sh（事务 2 常驻助手，root）     │
│              assets/df/ksud-real（KernelSU 用户态）        │
└──────────────────────────────────────────────────────────┘
```

依赖方向单向向下：UI 不知道 su 存在，事务层不知道界面。

## 两条链路

### 事务 1：建立临时 su

```
MainScreen（点击）
  → Engine.establishSu()
      → DFBridge.establishSu()
          → 打开 IPsec UDP 封装 socket / 分配 SPI / 构造 transform
          → 把 ksud-real、helper.sh、payload.sh 落盘到应用 data 目录
          → nativeRunAll(...)：引擎提权并执行 payload
  → 等待 /system/bin/su 出现
  → 等待助手心跳
  → 状态：Ready / Failed
```

payload（在 `vendor_modprobe` 内执行）只做四件事：落一份 ksud、bind-mount 到系统程序路径、`late-load --allow-shell`、启动助手。

payload **不含任何 `am` 调用**，也不接受运行时字符串。

### 事务 2：发送启动指令

```
MainScreen（点击某个 keyString）
  → MenuTx.request()
      → 校验白名单
      → 写 files/tx2/req（序号 + 命令）
  ← 助手轮询到序号变化
      → su -c "<命令>"
      → 追加结果到 files/tx2/out.log
  ← MenuTx 读到增量，作为原始输出
```

只有助手执行特权命令。应用一个字节的 su 都不碰。

## 为什么是这个文件信箱，而不是 socket / FIFO / binder

| 通道 | 结论 |
|---|---|
| 应用 `exec su` | 应用 uid 下路径不存在 |
| FIFO | 应用写 `app_data_file:fifo_file` 被静默拒绝；root 建的 FIFO 又缺应用 MLS 类别 |
| abstract socket | 应用侧可创建，但 root 侧没有可用的 shell 客户端去连 |
| binder | 需要常驻的、被系统认可的注册方，风险和成本都不可控 |
| 普通文件信箱 | 双方读写都验证通过；代价只是助手侧一次 200 ms 轮询 |

文件中谁创建不能随便换：**必须由应用创建**。root 创建的文件只带 `s0`，不带 `s0:cXXX,cYYY`，应用随后读不了。

## 状态机

事务 1 只有四个可见状态：

```
NotRun ──点击──▶ Running ──助手心跳正常──▶ Ready
                    │
                    └──超时/引擎失败──▶ Failed
```

事务 2 的可用性是 `Ready` 的推论，不单独存状态。

助手存活用心跳（`status` 的 mtime 在数秒内）判断，不用 pid：进程被杀后 pid 会留在磁盘上，应用也无法 stat 一个 root 进程。

## 日志

- 每次运行一个文件：`files/runs/run-yyyyMMdd-HHmmss.log`
- 第一行是 `## <摘要>`，之后是带时间戳的原始行
- 页面展示：摘要在上，原始输出折叠在下
- 启动时从最新的文件回填，不依赖 composition 内存

回填不是"锦上添花"：系统弹窗会让 Activity 重建，内存状态会丢，用户就会看到"明明跑过却没有日志"。

## 关键文件

| 路径 | 职责 |
|---|---|
| `MainActivity.kt` | 页面骨架、状态持有、两个事务的编排 |
| `ui/Theme.kt` | 颜色与四级文字层级 |
| `ui/Glass.kt` | 液态玻璃背景与导航胶囊 |
| `ui/Components.kt` | 卡片权重阶梯、状态标签、折叠头、动作行 |
| `ui/MainScreen.kt` | 主页：两个操作卡 |
| `ui/LogScreen.kt` | 日志：摘要 + 折叠原始输出 + 运行历史 |
| `ui/AboutScreen.kt` | 关于：风险声明 → 说明 → 实现（折叠）→ 致谢 |
| `Engine.kt` | 事务 1 的包装与白名单 |
| `MenuTx.kt` | 事务 2 的信箱协议 |
| `LogStore.kt` | 运行记录落盘、摘要、回填 |
| `RunService.kt` | 前台服务，只防冻结，不承载逻辑 |
| `DFBridge.java` | 引擎桥：IPsec、载荷落盘、原生调用 |
| `assets/payload.sh` | 事务 1 的设备侧脚本 |
| `assets/helper.sh` | 事务 2 的常驻助手 |
| `tools/patch_libexp_path.py` | 修正引擎里硬编码的载荷路径（见下） |

## libexp.so 的补丁

引擎里硬编码了上游的载荷路径 `/data/data/<upstream>/ksud`，长度固定 48 字节。本工程把其中的字符串原地替换成自己的路径，余下用 NUL 填充，文件大小不变。补丁脚本是可复现的，见 `tools/patch_libexp_path.py`。

**这一步不是可选的**：不打补丁时，载荷会被写到上游路径，引擎去执行时会找不到本工程的文件。
