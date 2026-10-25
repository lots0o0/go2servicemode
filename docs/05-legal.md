# 许可与第三方组件

## 本仓库代码

MIT，见根目录 `LICENSE`。

## 仓库内的预编译二进制

| 文件 | 上游 | 许可 |
|---|---|---|
| `app/src/main/assets/df/ksud-real` | KernelSU（`ksud` 用户态） | GPL-3.0（上游 KernelSU 项目） |
| `app/src/main/jniLibs/arm64-v8a/libexp.so` | DirtyFrag / DFRoot 适配层，来源为社区分发的 Root My Galaxy 系列产物 | **上游未明确声明** |

这两点必须说清楚：

1. **KernelSU 是 GPL-3.0**。再分发它的二进制时，需要同时提供对应源码的获取方式。本仓库不包含 KernelSU 源码，请以上游仓库为准；如果这个约束对你不适用，请自行从上游构建 `ksud` 并替换该文件。
2. **`libexp.so` 的许可证未被上游明确标注**。本仓库把它作为"原样使用的研究产物"收录，并附上可复现的路径补丁脚本（`tools/patch_libexp_path.py`）与未打补丁的原件（`tools/libexp.so.orig`）以便核对。若你是权利人且不希望它以这种方式被收录，请提出，会立刻移除。

如果你只要阅读协议与界面部分，可以删除这两个二进制后构建（功能不可用，但工程可编译）。

## 运行时依赖（Maven）

| 依赖 | 许可 |
|---|---|
| `androidx.*`（Compose / Activity） | Apache-2.0 |
| `io.github.kyant0:backdrop`、`io.github.kyant0:shapes` | 见上游仓库（androidliquidglass 系列） |

## 参考项目与致谢

| 对象 | 关系 |
|---|---|
| diabl0w — DFRoot / DirtyFrag | 提权引擎（CVE-2026-43284） |
| snothin — GhostSam | 三星 KDP / DEFEX 适配思路 |
| tiann — KernelSU | `ksud` 与 `late-load` 机制 |
| Kyant (kyant0) — androidliquidglass / backdrop | 液态玻璃界面 |
| kuuky29 — UniRoot | 引擎调用与载荷编排的参考实现 |
| xorpix / BuSung-dev — Root My Galaxy | 三星设备适配与文档 |

上述项目各自适用其自身许可。本仓库只调用/参考其公开成果，不重新授权它们的代码。

## 免责

本项目用于安全研究与自有设备上的实验。它依赖一个未被修复的提权漏洞，可能随系统更新失效，也可能在某些设备上完全不可用。使用前请确认当地法律与设备条款，并自行承担风险。作者不对任何设备损坏、数据丢失或保修失效负责。
