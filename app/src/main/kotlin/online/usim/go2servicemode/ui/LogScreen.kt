package online.usim.go2servicemode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import online.usim.go2servicemode.LogEntry
import online.usim.go2servicemode.LogStore
import java.io.File

/**
 * Log page: the current run's outcome plus the run history.
 *
 * The page is one scroll container. Raw output is only a short preview here;
 * the full text opens in LogViewer, which owns its own single list.
 */
@Composable
fun LogScreen(
    backdrop: LayerBackdrop,
    summary: String?,
    log: List<LogEntry>,
    runs: List<File>,
    currentFile: File?,
    onOpenCurrent: () -> Unit,
    onOpenFile: (File) -> Unit,
    onDelete: (File) -> Unit,
    onShare: (File) -> Unit
) {
    var showHistoryHint by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ScreenHeader("运行日志") {
                if (log.isNotEmpty()) {
                    BasicText("${log.size} 行", style = CaptionStyle)
                }
            }
        }

        if (summary != null || log.isNotEmpty()) {
            item {
                SectionCard(level = CardLevel.Normal) {
                    BasicText(summary ?: "本次运行未生成摘要", style = BodyStyle)
                    if (log.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        // Preview only: the last three lines, clipped. No inner
                        // scroll view, so it cannot fight the page's list.
                        Column(Modifier.fillMaxWidth()) {
                            log.takeLast(3).forEach { e ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                    BasicText(
                                        e.time,
                                        modifier = Modifier.padding(end = 8.dp),
                                        style = MonoStyle.copy(color = Tone.InkFaint)
                                    )
                                    BasicText(
                                        e.text,
                                        modifier = Modifier.weight(1f),
                                        style = MonoStyle.copy(color = Tone.InkSoft)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton(backdrop, "查看完整日志") { onOpenCurrent() }
                            if (currentFile != null) {
                                GlassButton(backdrop, "导出") { onShare(currentFile) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText("运行历史", style = SectionStyle)
                Spacer(Modifier.size(8.dp))
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Tone.IdleBg)
                        .clickable { showHistoryHint = !showHistoryHint },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("i", style = CaptionStyle.copy(color = Tone.IdleFg, fontSize = 10.sp))
                }
                Spacer(Modifier.weight(1f))
                BasicText("${runs.size} 次", style = CaptionStyle)
            }
            if (showHistoryHint) {
                Spacer(Modifier.height(6.dp))
                Hint("每次运行自动存盘，可导出为 .txt")
            }
            Spacer(Modifier.height(10.dp))
        }

        if (runs.isEmpty()) {
            item {
                SectionCard(level = CardLevel.Subtle) { Hint("暂无历史记录") }
            }
        } else {
            items(runs) { f ->
                SectionCard(level = CardLevel.Subtle) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BasicText(LogStore.label(f), style = BodyStyle)
                        Spacer(Modifier.weight(1f))
                        BasicText("${f.length()} B", style = CaptionStyle)
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

