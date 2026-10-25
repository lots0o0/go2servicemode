package online.usim.go2servicemode.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.usim.go2servicemode.BuildConfig

/**
 * About page.
 *
 * The order is the point: safety notice first, one-line description second,
 * implementation third and folded, acknowledgements last and smallest. Earlier
 * this page was five equally weighted cards, which made the risk statement
 * indistinguishable from a credits list.
 */
@Composable
fun AboutScreen() {
    var showImpl by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader("关于") {
                BasicText(
                    "${BuildConfig.BUILD_STAMP} · v${BuildConfig.VERSION_NAME}",
                    style = CaptionStyle
                )
            }
        }

        item {
            SectionCard(level = CardLevel.Danger) {
                BasicText("安全边界", style = SectionStyle.copy(color = Tone.FailFg))
                Spacer(Modifier.height(8.dp))
                BasicText(
                    "仅用于拉起三星 ServiceMode。不对外暴露 su / shell，不写入 /system 分区，" +
                        "不改动 optics 与 prism，无开机自启。临时权限只存在于内存，重启即消失。",
                    style = BodySoftStyle.copy(color = Tone.Ink)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            SectionCard(level = CardLevel.Normal) {
                BasicText("工具说明", style = SectionStyle)
                Spacer(Modifier.height(8.dp))
                BasicText(
                    "把选定的 keyString 发给三星 ServiceMode。没有常驻 root，没有授权面板，" +
                        "也没有对其他应用开放的通道。",
                    style = BodySoftStyle
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            SectionCard(level = CardLevel.Normal) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CollapsibleHeader("实现方式", showImpl) { showImpl = !showImpl }
                }
                if (showImpl) {
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "基于 DirtyFrag（CVE-2026-43284）与 KernelSU classic 实现；" +
                            "不使用 Next 分支，不启动也不依赖 KernelSU Manager。\n" +
                            "操作拆成两步：① 建立临时 su ② 发送启动指令。分开是为了任一步失败都能单独定位。",
                        style = BodySoftStyle
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            SectionCard(level = CardLevel.Subtle) {
                BasicText("致谢", style = SectionStyle.copy(fontSize = 14.sp))
                Spacer(Modifier.height(10.dp))
                CreditGroup(
                    "引擎与核心适配",
                    listOf(
                        "diabl0w — DFRoot / DirtyFrag 漏洞引擎",
                        "snothin — GhostSam，三星 KDP / DEFEX 适配",
                        "tiann — KernelSU"
                    )
                )
                Spacer(Modifier.height(12.dp))
                CreditGroup(
                    "UI 组件",
                    listOf("Kyant (kyant0) — androidliquidglass / backdrop，液态玻璃效果")
                )
                Spacer(Modifier.height(12.dp))
                CreditGroup(
                    "文档与参考项目",
                    listOf(
                        "kuuky29 — UniRoot",
                        "xorpix / BuSung-dev — Root My Galaxy 项目及文档"
                    )
                )
                Spacer(Modifier.height(14.dp))
                BasicText(
                    "开源许可与第三方组件归属见仓库 docs/05-legal.md。",
                    style = CaptionStyle
                )
            }
            Spacer(Modifier.height(16.dp))
            BasicText(
                "本工具按“原样”提供，不附带任何担保；使用前请自行确认风险与当地法规。",
                style = CaptionStyle
            )
        }
    }
}

@Composable
private fun CreditGroup(title: String, entries: List<String>) {
    BasicText(
        title,
        style = CaptionStyle.copy(color = Tone.InkSoft, fontSize = 12.sp)
    )
    Spacer(Modifier.height(6.dp))
    entries.forEach { line ->
        Row(Modifier.padding(vertical = 2.dp)) {
            BasicText(line, style = BodySoftStyle.copy(fontSize = 13.sp))
        }
    }
}
