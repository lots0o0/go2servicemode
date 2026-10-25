package online.usim.go2servicemode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import online.usim.go2servicemode.LogStore
import java.io.File

/**
 * Every run is persisted, so this page is the run list and nothing else: the
 * full text opens in LogViewer from the stored file. There is no separate
 * "current run" block, which would show the same record twice.
 */
@Composable
fun LogScreen(
    backdrop: LayerBackdrop,
    runs: List<File>,
    onOpenFile: (File) -> Unit,
    onDelete: (File) -> Unit,
    onShare: (File) -> Unit
) {
    var showHint by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ScreenHeader("运行历史") {
                BasicText("${runs.size} 次", style = CaptionStyle)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Hint("每次运行自动存盘")
                Spacer(Modifier.size(8.dp))
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Tone.IdleBg)
                        .clickable { showHint = !showHint },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("i", style = CaptionStyle.copy(color = Tone.IdleFg, fontSize = 10.sp))
                }
            }
            if (showHint) {
                Spacer(Modifier.height(6.dp))
                Hint("日志保存在应用私有目录，可导出为 .txt 分享")
            }
            Spacer(Modifier.height(12.dp))
        }

        if (runs.isEmpty()) {
            item {
                SectionCard(level = CardLevel.Subtle) { Hint("暂无记录") }
            }
        } else {
            items(runs) { f ->
                SectionCard(level = CardLevel.Subtle) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BasicText(LogStore.label(f), style = BodyStyle)
                        Spacer(Modifier.weight(1f))
                        BasicText("${f.length()} B", style = CaptionStyle)
                    }
                    val summary = LogStore.summaryOf(f)
                    if (summary != null) {
                        Spacer(Modifier.height(4.dp))
                        BasicText(summary, style = BodySoftStyle)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(backdrop, "查看") { onOpenFile(f) }
                        GlassButton(backdrop, "导出") { onShare(f) }
                        GlassButton(backdrop, "删除") { onDelete(f) }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
