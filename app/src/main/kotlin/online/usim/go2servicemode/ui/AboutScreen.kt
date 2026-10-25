package online.usim.go2servicemode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * About page. Order is the hierarchy: safety notice, one-line description,
 * implementation (folded), credits (smallest).
 */
@Composable
fun AboutScreen(version: String, buildDate: String, apkHash: String?) {
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
                Column {
                    BasicText(version, style = CaptionStyle)
                    BasicText(buildDate, style = CaptionStyle)
                    if (apkHash != null) {
                        BasicText("SHA-256 $apkHash", style = CaptionStyle)
                    }
                }
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
                    "三星 ServiceMode 启动工具，防止固化分区等危险产生，不主动对外输出 su 能力，" +
                        "且不建议使用任何 SU 管理器介入，请谨慎操作！",
                    style = BodySoftStyle
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            SectionCard(level = CardLevel.Normal) {
                CollapsibleHeader("实现方式", showImpl) { showImpl = !showImpl }
                if (showImpl) {
                    Spacer(Modifier.height(8.dp))
                    BasicText("基于 CVE-2026-43284 等公开研究成果实现。", style = BodySoftStyle)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            SectionCard(level = CardLevel.Subtle) {
                BasicText("致谢", style = SectionStyle.copy(fontSize = 14.sp))
                Spacer(Modifier.height(10.dp))
                CreditGroup(
                    "项目引用",
                    listOf(
                        "lots0 — 多版本包审查",
                        "diabl0w — DFRoot / DirtyFrag",
                        "snothin — GhostSam，三星 KDP / DEFEX 适配",
                        "tiann — KernelSU"
                    )
                )
                Spacer(Modifier.height(12.dp))
                CreditGroup(
                    "文档与参考项目",
                    listOf(
                        "kuuky29 — UniRoot",
                        "xorpix / BuSung-dev — Root My Galaxy 项目及文档",
                        "lots0 私有仓库（未公开）— 版本包归档与审查记录"
                    )
                )
                Spacer(Modifier.height(12.dp))
                CreditGroup(
                    "特别感谢",
                    listOf(
                        "Gary — 设备提供方",
                        "因对包审查的判断失误导致 D2 锁定，拆机维修解决。这也是本工具不对外暴露 su 能力的初衷。"
                    )
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
        BasicText(line, style = BodySoftStyle.copy(fontSize = 13.sp))
        Spacer(Modifier.height(4.dp))
    }
}
