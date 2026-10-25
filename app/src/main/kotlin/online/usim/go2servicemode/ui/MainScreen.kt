package online.usim.go2servicemode.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import online.usim.go2servicemode.DeviceSnapshot
import online.usim.go2servicemode.Engine

/** State of transaction 1 as shown to the user. */
enum class TxState { NotRun, Running, Ready, Failed }

/**
 * Home page: operations only. Two cards of different weights — bootstrapping the
 * privilege channel, then what it enables.
 */
@Composable
fun MainScreen(
    backdrop: LayerBackdrop,
    device: DeviceSnapshot,
    busy: Boolean,
    tx: TxState,
    busyMessage: String?,
    onEstablish: () -> Unit,
    onOpenMenu: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        ScreenHeader("go2servicemode")
        BasicText("三星 ServiceMode 启动工具", style = BodySoftStyle)
        Spacer(Modifier.height(4.dp))
        BasicText(device.description, style = CaptionStyle)
        Spacer(Modifier.height(18.dp))

        SectionCard(level = CardLevel.Primary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    BasicText("建立临时 su", style = SectionStyle.copy(fontSize = 18.sp))
                    Spacer(Modifier.height(3.dp))
                    Hint("权限通道，重启后失效")
                }
                val (label, fg, bg) = when (tx) {
                    TxState.Ready -> Triple("已就绪", Tone.OkFg, Tone.OkBg)
                    TxState.Running -> Triple("执行中", Tone.BusyFg, Tone.BusyBg)
                    TxState.Failed -> Triple("失败", Tone.FailFg, Tone.FailBg)
                    TxState.NotRun -> Triple("未执行", Tone.IdleFg, Tone.IdleBg)
                }
                StatusChip(label, fg, bg)
            }
            Spacer(Modifier.height(14.dp))
            GlassButton(
                backdrop = backdrop,
                label = if (tx == TxState.Ready) "重新执行" else "执行",
                modifier = Modifier.fillMaxWidth(),
                accent = true,
                enabled = !busy,
                onClick = onEstablish
            )
        }

        Spacer(Modifier.height(12.dp))

        val ready = device.canOpenMenu
        SectionCard(level = if (ready) CardLevel.Primary else CardLevel.Normal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText("打开 ServiceMode", style = SectionStyle)
                Spacer(Modifier.weight(1f))
                if (!ready) StatusChip("待解锁", Tone.IdleFg, Tone.IdleBg)
            }
            Spacer(Modifier.height(4.dp))
            Hint(if (ready) "选择要发送的 keyString" else "完成上一步后可用")
            Spacer(Modifier.height(12.dp))

            GlassRow(
                backdrop = backdrop,
                title = "GCF 菜单",
                subtitle = "keyString ${Engine.KEY_GCF}",
                enabled = ready && !busy
            ) { onOpenMenu(Engine.KEY_GCF) }

            Spacer(Modifier.height(8.dp))

            GlassRow(
                backdrop = backdrop,
                title = "NAS / 主菜单",
                subtitle = "keyString ${Engine.KEY_NAS}",
                enabled = ready && !busy
            ) { onOpenMenu(Engine.KEY_NAS) }
        }

        if (busy && busyMessage != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicText("·", style = BodySoftStyle.copy(color = Tone.BusyFg, fontSize = 22.sp))
                BasicText(busyMessage, style = BodySoftStyle)
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}
