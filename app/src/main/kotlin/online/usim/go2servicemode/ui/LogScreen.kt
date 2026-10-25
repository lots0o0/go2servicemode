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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import online.usim.go2servicemode.LogEntry
import online.usim.go2servicemode.LogStore
import java.io.File

/**
 * Log page.
 *
 * Two layers, because a user and a maintainer need different things from the
 * same run: a one-line outcome on top, the raw engine output folded underneath.
 * The previous version printed shell commands straight into the page and left
 * `rc=0` competing with the result.
 */
@Composable
fun LogScreen(
    summary: String?,
    log: List<LogEntry>,
    runs: List<File>,
    viewing: String?,
    onOpen: (File) -> Unit,
    onClose: () -> Unit,
    onDelete: (File) -> Unit,
    onShare: (File) -> Unit
) {
    var rawExpanded by rememberSaveable { mutableStateOf(false) }
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
                    BasicText("${log.size} 条", style = CaptionStyle)
                }
            }
        }

        if (summary != null || log.isNotEmpty()) {
            item {
                SectionCard(level = CardLevel.Normal) {
                    BasicText(summary ?: "已运行，未生成摘要", style = BodyStyle)
                    if (log.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        CollapsibleHeader("原始输出", rawExpanded) { rawExpanded = !rawExpanded }
                        if (rawExpanded) {
                            Spacer(Modifier.height(8.dp))
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 340.dp)
                                    .clip(InnerShape)
                                    .background(Tone.Field)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                log.forEach { MonoLine(it.time, it.text) }
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
                Spacer(Modifier.width(8.dp))
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

        if (viewing != null) {
            item {
                SectionCard(level = CardLevel.Normal) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BasicText("日志内容", style = SectionStyle.copy(fontSize = 14.sp))
                        Spacer(Modifier.weight(1f))
                        GhostButton("关闭") { onClose() }
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        viewing.lineSequence().filter { it.isNotBlank() }.forEach { line ->
                            BasicText(
                                line,
                                modifier = Modifier.padding(vertical = 1.dp),
                                style = MonoStyle
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (runs.isEmpty()) {
            item {
                SectionCard(level = CardLevel.Subtle) {
                    Hint("暂无历史记录")
                }
            }
        } else {
            items(runs.size) { i ->
                val f = runs[i]
                SectionCard(level = CardLevel.Subtle) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BasicText(LogStore.label(f), style = BodyStyle)
                        Spacer(Modifier.weight(1f))
                        BasicText("${f.length()} B", style = CaptionStyle)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("查看") { onOpen(f) }
                        GhostButton("导出") { onShare(f) }
                        GhostButton("删除") { onDelete(f) }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MonoLine(time: String, text: String) {
    Row(Modifier.padding(vertical = 1.dp)) {
        BasicText(time, modifier = Modifier.width(60.dp), style = MonoStyle.copy(color = Tone.InkFaint))
        BasicText(text, modifier = Modifier.weight(1f), style = MonoStyle)
    }
}
