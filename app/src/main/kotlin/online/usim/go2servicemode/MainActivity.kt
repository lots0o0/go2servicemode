package online.usim.go2servicemode

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.usim.go2servicemode.ui.AboutScreen
import online.usim.go2servicemode.ui.AppBackground
import online.usim.go2servicemode.ui.LogScreen
import online.usim.go2servicemode.ui.LogViewer
import online.usim.go2servicemode.ui.MainScreen
import online.usim.go2servicemode.ui.NavBar
import online.usim.go2servicemode.ui.TxState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PAGE_MAIN = 0
private const val PAGE_LOG = 1
private const val PAGE_COUNT = 2

/** Where the transaction-1 payload records its own progress (root-owned, 0644). */
private const val TX1_LOG = "/data/local/tmp/g2sm-tx1.log"

private val TimeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

/** A full-screen log view: either the current run or a stored run file. */
private data class ViewerTarget(
    val title: String,
    val summary: String?,
    val entries: List<LogEntry>,
    val file: File?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0, 0),
            navigationBarStyle = SystemBarStyle.light(0, 0)
        )
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val pagerState = rememberPagerState(initialPage = PAGE_MAIN) { PAGE_COUNT }
    val scope = rememberCoroutineScope()

    val log = remember { mutableStateListOf<LogEntry>() }
    var runFile by remember { mutableStateOf<File?>(null) }
    var runs by remember { mutableStateOf(LogStore.list(context)) }
    var viewer by remember { mutableStateOf<ViewerTarget?>(null) }
    var summary by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var apkHash by remember { mutableStateOf<String?>(null) }
    var device by remember { mutableStateOf(DeviceProbe.probe(context)) }
    var tx by remember {
        mutableStateOf(if (device.canOpenMenu) TxState.Ready else TxState.NotRun)
    }
    var showAbout by remember { mutableStateOf(false) }

    fun add(line: String) {
        val e = LogEntry(TimeFmt.format(Date()), line)
        android.util.Log.i("go2servicemode", e.time + " " + line)
        log.add(e)
        runFile?.let { LogStore.append(it, e.time, line) }
    }

    fun share(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "导出日志"))
        }
    }

    fun startRun(label: String) {
        log.clear()
        summary = null
        runFile = LogStore.newRun(context)
        runs = LogStore.list(context)
        busy = true
        busyMessage = label
        add(label)
        runCatching {
            context.startForegroundService(
                Intent(context, RunService::class.java).putExtra(RunService.EXTRA_TEXT, label)
            )
        }
    }

    fun finishRun(outcome: String, elapsedMs: Long) {
        val text = "$outcome · ${"%.1f".format(elapsedMs / 1000.0)}s"
        summary = text
        runFile?.let { LogStore.writeSummary(it, text) }
        runCatching { context.stopService(Intent(context, RunService::class.java)) }
        busy = false
        busyMessage = null
        runs = LogStore.list(context)
    }

    LaunchedEffect(Unit) {
        // The channel files must exist before the helper starts, and they must
        // be created here: root-created files carry no MLS categories and the
        // app could never read them back.
        withContext(Dispatchers.IO) { MenuTx.ensure(context) }
        apkHash = withContext(Dispatchers.IO) { ApkInfo.shortHash(context) }
        device = withContext(Dispatchers.IO) { DeviceProbe.probe(context) }
        if (log.isEmpty()) {
            val latest = withContext(Dispatchers.IO) { LogStore.list(context).firstOrNull() }
            if (latest != null) {
                val restored = withContext(Dispatchers.IO) {
                    LogStore.summaryOf(latest) to LogStore.parse(latest)
                }
                if (restored.second.isNotEmpty() || restored.first != null) {
                    runFile = latest
                    summary = restored.first
                    restored.second.forEach { log.add(it) }
                }
            }
        }
    }

    BackHandler(enabled = viewer != null || showAbout || pagerState.currentPage != PAGE_MAIN) {
        when {
            viewer != null -> viewer = null
            showAbout -> showAbout = false
            else -> scope.launch { pagerState.animateScrollToPage(PAGE_MAIN) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) { AppBackground() }

        Column(Modifier.fillMaxSize()) {
            // The About overlay lives inside the page area, not above the whole
            // screen: otherwise it covers the navigation and the only way back
            // is the system gesture.
            Box(Modifier.weight(1f)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                    when (index) {
                        PAGE_MAIN -> MainScreen(
                            backdrop = backdrop,
                            device = device,
                            busy = busy,
                            tx = tx,
                            busyMessage = busyMessage,
                            onEstablish = {
                                if (!busy) {
                                    tx = TxState.Running
                                    MenuTx.clearStatus(context)
                                    startRun("建立临时 su")
                                    scope.launch {
                                        pagerState.animateScrollToPage(PAGE_LOG)
                                        val t0 = System.currentTimeMillis()
                                        val rc = withContext(Dispatchers.IO) {
                                            Engine.establishSu(context) { line -> add(line) }
                                        }
                                        val elapsed = System.currentTimeMillis() - t0
                                        add("engine rc=$rc  ${elapsed}ms")
                                        val tx1 = withContext(Dispatchers.IO) {
                                            runCatching { File(TX1_LOG).readLines() }.getOrNull()
                                        }
                                        if (tx1.isNullOrEmpty()) {
                                            add("$TX1_LOG 读不到")
                                        } else {
                                            tx1.filter { it.isNotBlank() }.forEach { add(it) }
                                        }
                                        // The payload starts the helper as its
                                        // last step, so it lands a moment after
                                        // the engine returns.
                                        val ready = withContext(Dispatchers.IO) {
                                            var ok = MenuTx.ready(context)
                                            var tries = 0
                                            while (!ok && tries < 25) {
                                                Thread.sleep(100)
                                                ok = MenuTx.ready(context)
                                                tries++
                                            }
                                            ok
                                        }
                                        device = withContext(Dispatchers.IO) { DeviceProbe.probe(context) }
                                        tx = if (ready) TxState.Ready else TxState.Failed
                                        finishRun(
                                            if (ready) "建立临时 su · 已就绪" else "建立临时 su · 失败",
                                            elapsed
                                        )
                                    }
                                }
                            },
                            onOpenMenu = { key ->
                                if (!busy) {
                                    val name =
                                        if (key == Engine.KEY_NAS) "NAS / 主菜单" else "GCF 菜单"
                                    startRun("打开 $name")
                                    scope.launch {
                                        pagerState.animateScrollToPage(PAGE_LOG)
                                        val t0 = System.currentTimeMillis()
                                        val rc = withContext(Dispatchers.IO) {
                                            MenuTx.request(context, key) { line -> add(line) }
                                        }
                                        finishRun(
                                            if (rc == 0) "打开 $name · 已发送" else "打开 $name · 失败",
                                            System.currentTimeMillis() - t0
                                        )
                                    }
                                }
                            }
                        )

                        else -> LogScreen(
                            backdrop = backdrop,
                            runs = runs,
                            onOpenFile = { f ->
                                viewer = ViewerTarget(
                                    title = LogStore.label(f),
                                    summary = LogStore.summaryOf(f),
                                    entries = LogStore.parse(f),
                                    file = f
                                )
                            },
                            onDelete = { f ->
                                LogStore.delete(f)
                                runs = LogStore.list(context)
                            },
                            onShare = { f -> share(f) }
                        )
                    }
                }

                AboutOverlay(showAbout, apkHash, backdrop)
            }

            NavBar(
                backdrop = backdrop,
                page = if (showAbout) -1 else pagerState.currentPage,
                aboutOpen = showAbout,
                onTab = { target ->
                    showAbout = false
                    scope.launch { pagerState.animateScrollToPage(target) }
                },
                onAbout = { showAbout = !showAbout }
            )
        }

        val target = viewer
        if (target != null) {
            LogViewer(
                backdrop = backdrop,
                title = target.title,
                summary = target.summary,
                entries = target.entries,
                onClose = { viewer = null },
                onShare = target.file?.let { f -> { share(f) } }
            )
        }
    }
}

/**
 * About is an overlay on top of the page area, not a third pager page: opening
 * it must not reset the other two pages. It is a separate composable so the
 * ColumnScope AnimatedVisibility overload cannot shadow the plain one.
 */
@Composable
private fun AboutOverlay(visible: Boolean, apkHash: String?, backdrop: com.kyant.backdrop.backdrops.LayerBackdrop) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 6 },
        exit = fadeOut() + slideOutVertically { it / 6 }
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF7F9FD))) {
            AboutScreen(
                backdrop = backdrop,
                version = "V${BuildConfig.VERSION_NAME}",
                buildDate = BuildConfig.BUILD_DATE,
                apkHash = apkHash
            )
        }
    }
}
