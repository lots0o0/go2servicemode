package online.usim.go2servicemode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

/**
 * Liquid-glass surface used by the navigation capsules.
 *
 * The backdrop has to be applied to the page background container (see
 * `AppBackground`), otherwise drawBackdrop has nothing to sample and the effect
 * silently degrades to a flat fill.
 */
@Composable
fun AppBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0f to Tone.PageBgTop,
                    0.55f to Color(0xFFEFF3FB),
                    1f to Color(0xFFF7F5FB),
                    start = Offset.Zero,
                    end = Offset(1100f, 2200f)
                )
            )
    ) {
        Blob(Color(0xFFBBD3FF), Modifier.padding(start = 20.dp, top = 150.dp).size(280.dp))
        Blob(Color(0xFFD9C8FF), Modifier.padding(start = 180.dp, top = 420.dp).size(240.dp))
        Blob(Color(0xFFB7EFE2), Modifier.padding(start = 40.dp, top = 700.dp).size(260.dp))
    }
}

@Composable
private fun Blob(color: Color, modifier: Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.75f), Color.Transparent)))
    )
}

@Composable
fun NavBar(
    backdrop: LayerBackdrop,
    page: Int,
    aboutOpen: Boolean,
    onTab: (Int) -> Unit,
    onAbout: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCapsule(backdrop, "主页", selected = page == 0 && !aboutOpen) { onTab(0) }
        GlassCapsule(backdrop, "日志", selected = page == 1 && !aboutOpen) { onTab(1) }
        Spacer(Modifier.weight(1f))
        GlassCapsule(backdrop, "关于", selected = aboutOpen, onDark = true, onClick = onAbout)
    }
}

@Composable
private fun GlassCapsule(
    backdrop: LayerBackdrop,
    label: String,
    selected: Boolean,
    onDark: Boolean = false,
    onClick: () -> Unit
) {
    val fill = when {
        selected && onDark -> Tone.Ink.copy(alpha = 0.94f)
        selected -> Tone.Ink.copy(alpha = 0.92f)
        else -> Color.White.copy(alpha = 0.55f)
    }
    val textColor = if (selected) Color.White else Tone.InkSoft
    Box(
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(10f.dp.toPx(), 22f.dp.toPx())
                },
                onDrawSurface = { drawRect(fill) }
            )
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = if (selected) 0.9f else 0.6f),
                    style = Stroke(width = 1.2f.dp.toPx()),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            }
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(color = textColor, fontSize = 14.sp)
        )
    }
}
