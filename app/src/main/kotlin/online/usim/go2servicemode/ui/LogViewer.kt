package online.usim.go2servicemode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import online.usim.go2servicemode.LogEntry

/**
 * Full-screen log viewer.
 *
 * One scroll container, virtualised list, fixed time column. The page behind it
 * never nests a scrollable log inside a scrollable page; that nesting was the
 * cause of the scrolling defects in the first layout.
 *
 * Follow-tail: new lines scroll into view only while the user is at the bottom.
 * Scrolling up stops following and shows a "跳到最新" button.
 */
@Composable
fun LogViewer(
    backdrop: LayerBackdrop,
    title: String,
    summary: String?,
    entries: List<LogEntry>,
    onClose: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val state = rememberLazyListState()
    var follow by remember { mutableStateOf(true) }

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex to state.layoutInfo.visibleItemsInfo.size }
            .collect { (first, visible) ->
                if (visible > 0) {
                    follow = first + visible >= entries.size - 1
                }
            }
    }
    LaunchedEffect(entries.size) {
        if (follow && entries.isNotEmpty()) {
            state.scrollToItem(entries.lastIndex)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Tone.PageBgTop)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(backdrop = backdrop, label = "返回", onClick = onClose)
            Column(Modifier.weight(1f)) {
                BasicText(title, style = BodyStyle)
                BasicText("${entries.size} 行", style = CaptionStyle)
            }
            if (onShare != null) {
                GlassButton(backdrop = backdrop, label = "导出", onClick = onShare)
            }
        }

        if (summary != null) {
            BasicText(
                summary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = BodySoftStyle
            )
        }

        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(entries) { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        BasicText(
                            e.time,
                            modifier = Modifier.width(56.dp),
                            style = MonoStyle.copy(color = Tone.InkFaint)
                        )
                        BasicText(e.text, modifier = Modifier.weight(1f), style = MonoStyle)
                    }
                }
            }
            if (!follow) {
                Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    GlassButton(
                        backdrop = backdrop,
                        label = "跳到最新",
                        accent = true,
                        onClick = { follow = true }
                    )
                }
            }
        }
    }
}
