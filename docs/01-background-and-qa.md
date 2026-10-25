# 前置调查与实践记录

本文记录这个工具在动手写代码之前必须先回答的问题，以及每一条结论是怎么被验证的。

写法是问答，只写事实：结论、观测手段、反例。不含设备序列号、个人环境、时间线，也不含与实现无关的经历。

验证环境：一台三星 Android 17（One UI）设备，SELinux 处于 Enforcing，未解锁 bootloader，未刷入任何持久化 root。

---

## A. 目标与边界

**Q0. 这个工具到底要做什么？**

A：只做一件事——把一个受支持的 `keyString` 发送给三星 ServiceMode 组件，让它打开工程菜单。它不提供常驻 root、不提供 shell、不提供授权面板。

**Q0.1 明确不做的事**

- 不向第三方应用提供 su / shell 通道
- 不写入 `/system`、`/vendor`、`/product`、`/odm`、`/system_ext`、`prism`、`optics`
- 不触碰 `boot` / `vbmeta` / `efs` / `param` / `modem`
- 不关闭 verity，不安装开机自启

这些都是硬约束，写进代码注释里，不是口头承诺。

---

## B. 为什么必须先有临时 root

**Q1. 普通应用直接 `startActivity` 打开 ServiceMode，可以吗？**

A：不行，实测被拒。

`com.sec.android.RilServiceModeApp/.ServiceModeApp` 的 `ServiceModeApp` 声明为 `exported="false"`，并且运行在 `com.android.phone` 进程（uid 1001）。从 adb shell（uid 2000）尝试时返回：

```
java.lang.SecurityException: Permission Denial: starting Intent ...
not exported from uid 1001
```

在 `ActivityManagerService` 一侧，只有 uid 0 能通过 `checkStartAnyActivityPermission`。所以调用方必须是 root。

**Q2. 拨号器暗码能不能替代？**

A：不能。`ServiceModeApp` 需要 `KEYTYPE` / `KEYSTRING` 的 `signature|privileged` 权限，拨号器没有这个权限，暗码触发的路径拿不到该权限。

**Q3. 那临时 root 用什么实现？**

A：DirtyFrag（CVE-2026-43284）作为提权引擎，KernelSU 的 `ksud` 作为用户态，走 `late-load --allow-shell`。引擎跑在设备自带的 `libexp.so` 适配层里，通过 bind-mount 覆盖一个真实系统路径上的程序来通过 DEFEX 校验。

**Q4. 这个临时 root 有什么已知限制？**

A：实测只有两条：

1. **一次开机只成功一次**。`dfr_lkm` 加载后，同一 boot 内再次运行会走短路分支（引擎约 1 秒返回、不打补丁、不触发、不 bind-mount）。
2. **重启即消失**。内核模块、Permissive 状态、`/system/bin/su` 全部是内存态，重新开机后归零。

---

## C. payload 在提权后的上下文里能做什么

**Q5. 提权后，payload 里能直接调 `am` 吗？**

A：不能。引擎把 payload 放在一个私有 mount namespace 里执行，上下文是 `u:r:vendor_modprobe:s0`。在这个上下文里：

| 操作 | 结果 |
|---|---|
| `sh` / `cp` / `mount` / `ls` / `echo` / `printf` | 可用 |
| `am` / `cmd` / `app_process`（即任何 Java 进程） | 被杀，SIGKILL，退出码 137 |
| 写 `/proc/self/attr/exec` 转换 SELinux 域 | 失败（`setexec_rc=1`） |

结论：`am start` 不能在提权上下文里跑，必须换到正常域。

**Q6. `/system/bin/su` 为什么在 payload 里一开始看不到？**

A：`late-load` 是异步的：它 fork 出常驻进程后立刻返回 0，su 的 bind-mount 稍后才落地。实测第一次运行记录到 `tx1-su-missing`，但紧接着人工检查时 `/system/bin/su` 已经存在。两者不矛盾，是时序问题。修法是加有界等待后再判定。

**Q7. su 落地之后，它挂在哪个 namespace？**

A：挂在引擎那个私有 namespace 的视图里，同时 `adb shell`（uid 2000）也能看到并使用它：

```
$ su -c id
uid=0(root) ... context=u:r:ksu:s0
```

但**应用进程看不到**：

```
$ run-as <app> ls -l /system/bin/su
/system/bin/su: No such file or directory
```

`/system/bin/su` 的 bind-mount 位于应用不共享的 mount namespace。这一条直接否掉了一整类设计（应用 `ProcessBuilder("su")`），也是整个架构里最关键的一个约束。

---

## D. 应用与 root 之间怎么通信

**Q8. 应用能不能自己 `exec` su？**

A：不能，两个层面都不行：

1. 应用 uid 下该路径不存在（见 Q7）；
2. KernelSU 的授权门在内核侧（`ksuctl`），`--allow-shell` 只放行 uid 2000，应用 uid（10xxx）不在其中。

**Q9. 用 FIFO 做命令通道行不行？**

A：不行，实测被两件事同时挡住：

1. SELinux 拒绝应用写 `app_data_file:fifo_file`，而且没留 avc 记录（静默拒绝），表现只是 `Permission denied`；
2. 即使改成由 root 创建 FIFO，root 创建的文件只带 `s0`、不带应用的 MLS 类别（`s0:cXXX,cYYY`），应用**连读都读不了**。

**Q10. 最终用的通道是什么？**

A：一个由**应用**创建的文件信箱：

```
files/tx2/req     <- 应用写入：第一行序号，第二行命令
files/tx2/out.log <- root 助手追加 stdout / stderr
files/tx2/status  <- root 助手写自己的 pid，作为心跳
```

三个文件都由应用创建，因此带应用自己的 MLS 类别；root 对它们读写都验证通过（root 读应用文件、root 往应用文件追加都成功）。方向反过来就失败——所以"谁创建文件"是协议的一部分，不是实现细节。

配套结论：

- 助手必须**常驻**，阻塞在自己的循环里，不能每次重新提权（一次开机只有一次机会）；
- 应用侧不能靠"status 文件存在"判断助手存活：进程被杀后 pid 会留在磁盘上。用心跳（status 的 mtime 在数秒内更新）判断。

**Q11. 为什么把流程拆成两个事务？**

A：为了归因。合成一步时，"载荷没跑起来"和"`am` 参数不对"表现完全一样，实测中这两种情况都真实发生过，且都表现为"没有反应"。拆开后：

- 事务 1：跑引擎、等 su、起助手 → 结论只有"通道就绪 / 不可用"；
- 事务 2：往信箱投一条 `am start` → 结论只有"已发送 / 失败"。

---

## E. 观测手段（为什么这部分写得这么细）

**Q12. 为什么要专门设计日志落点？**

A：因为早期版本的"没有反应"至少三次是**观测错误**，不是功能错误：

1. payload 的日志重定向建出了文件，但内容为 0 字节 —— 于是"载荷没执行"这个结论是错的；
2. 应用的运行日志只存在内存里，系统弹窗（管理器提示、权限提示）导致 Activity 重建后，界面显示"没有日志"，而磁盘上记录完整；
3. 引擎在同一次 boot 内的第二次运行会短路，耗时从约 2.9 秒降到约 1 秒，曾把正常的一次运行误判成短路。

现在的约定：

- payload 的日志写到 `/data/local/tmp/g2sm-tx1.log`，root 可写、adb 可直接读，不用 `run-as`；
- 日志行只用字面量 + `$?`，不做命令替换，避免"文件存在但没内容"这类哑失败；
- 每次运行一条落盘记录，页面从磁盘回填，不依赖内存状态。

**Q13. 为什么要在界面上显示构建标记？**

A：两个只改了 payload、没改界面的版本在手机上一模一样，无法用肉眼确认"装的是不是新包"。现在构建时间写进 `BuildConfig`，显示在「关于」页右上角。

---

## F. 实践记录（流程性，不含设备与个人信息）

**F1. 提权链路**

1. 运行引擎 → 内核模块加载 → SELinux 短暂切 Permissive → 触发补丁 → 私有 namespace → bind-mount 一个系统程序为 `ksud` → `ksud late-load --allow-shell`
2. 等待 `/system/bin/su` 出现（有界等待，最长数秒）
3. 由事务 1 的载荷启动常驻助手（后台）
4. 助手写心跳，应用侧轮询心跳判定就绪

**F2. 发送链路**

1. 应用把 `am start --user 0 -n <组件> --es keyString <白名单值>` 写入 `req`
2. 助手读到变化 → `su -c <命令>` → 结果追加到 `out.log`
3. 应用读增量并展示摘要

**F3. 白名单**

只允许两个值：`4238378`（GCF 菜单）、`27663368378`（NAS / 主菜单）。校验在应用侧和 Java 桥各做一次；payload 里没有任何 `am` 调用，也不接受运行时传入的字符串。

**F4. 失败时的观测顺序**

1. `/data/local/tmp/g2sm-tx1.log`：载荷是否执行、`cp/mount/late-load` 的返回码
2. `ls -l /system/bin/su`：su 是否落地
3. `files/tx2/status` 的 mtime：助手是否存活
4. `files/tx2/out.log`：`am` 的实际输出
5. 同一次 boot 运行过第二次 → 先排除短路，再判断功能失败

---

## G. 已排除的方案（供后来者少走）

| 方案 | 排除原因 |
|---|---|
| 应用直接 `ProcessBuilder("su")` | 应用 uid 下该路径不存在（Q7），且授权门只认 uid 2000 |
| `ksud debug su` | 需要常驻守护进程的文件描述符，独立调用失败 |
| 从 `/proc/1/root` 绕 namespace | 只切换了 mount 视图，PID namespace 仍是私有 |
| Java 侧改 `/proc/self/attr/exec` 换域 | 写入被拒（`setexec_rc=1`） |
| 拨号器暗码 | 缺少 signature\|privileged 权限（Q2） |
| 往 `/data` 安装三星签名系统 APK | 被设备侧构建类型校验拒绝（`INSTALL_FAILED_REJECTED_BY_BUILDTYPE`，-3001） |
| FIFO 命令通道 | SELinux `fifo_file` + MLS 类别不匹配（Q9） |
| 用内核模块持久化 | 违反"不改动固件/分区"的边界 |
