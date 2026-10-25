package online.usim.go2servicemode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/**
 * Liquid glass, using the library's own highlight / inner-shadow passes instead
 * of painted borders. Selection is expressed by refraction strength, not by
 * filling the capsule with a dark colour.
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

/** One glass surface for every capsule in the app. */
private fun Modifier.glass(
    backdrop: LayerBackdrop,
    tint: Color,
    lensRefractionDp: Float,
    blurDp: Float = 2f,
    highlight: Highlight = Highlight.Default
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { Capsule() },
    effects = {
        vibrancy()
        blur(blurDp.dp.toPx())
        lens(lensRefractionDp.dp.toPx(), (lensRefractionDp * 2f).dp.toPx())
    },
    highlight = { highlight },
    shadow = { Shadow.Default },
    innerShadow = { InnerShadow.Default },
    onDrawSurface = { drawRect(tint) }
)

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
        NavCapsule(backdrop, "主页", selected = page == 0 && !aboutOpen) { onTab(0) }
        NavCapsule(backdrop, "日志", selected = page == 1 && !aboutOpen) { onTab(1) }
        Spacer(Modifier.weight(1f))
        NavCapsule(backdrop, "关于", selected = aboutOpen, onClick = onAbout)
    }
}

@Composable
private fun NavCapsule(
    backdrop: LayerBackdrop,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .glass(
                backdrop = backdrop,
                tint = Color.White.copy(alpha = if (selected) 0.30f else 0.12f),
                lensRefractionDp = if (selected) 20f else 8f,
                blurDp = if (selected) 3f else 1.5f,
                highlight = if (selected) Highlight.Default else Highlight.Ambient
            )
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (selected) Tone.Ink else Tone.InkSoft,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

/** Primary/secondary action, same material; emphasis comes from the tint. */
@Composable
fun GlassButton(
    backdrop: LayerBackdrop,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    if (!enabled) {
        Box(
            modifier
                .clip(ChipShape)
                .background(Tone.CardSubtle)
                .padding(horizontal = 18.dp, vertical = 11.dp)
        ) {
            BasicText(label, style = TextStyle(color = Tone.InkFaint, fontSize = 13.sp))
        }
        return
    }
    Box(
        modifier
            .glass(
                backdrop = backdrop,
                tint = if (accent) Tone.Accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.16f),
                lensRefractionDp = if (accent) 18f else 10f,
                highlight = if (accent) Highlight.Default else Highlight.Ambient
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (accent) Tone.Accent else Tone.Ink,
                fontSize = 13.sp,
                fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

/** Menu row as a wide glass capsule. */
@Composable
fun GlassRow(
    backdrop: LayerBackdrop,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (!enabled) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(ChipShape)
                .background(Tone.CardSubtle)
                .padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(title, style = BodyStyle.copy(color = Tone.InkFaint))
                Spacer(Modifier.size(2.dp))
                BasicText(subtitle, style = CaptionStyle)
            }
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(Tone.IdleBg),
                contentAlignment = Alignment.Center
            ) { BasicText("›", style = TextStyle(color = Tone.InkFaint, fontSize = 14.sp)) }
        }
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .glass(
                backdrop = backdrop,
                tint = Color.White.copy(alpha = 0.16f),
                lensRefractionDp = 10f,
                highlight = Highlight.Ambient
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = BodyStyle)
            Spacer(Modifier.size(2.dp))
            BasicText(subtitle, style = CaptionStyle)
        }
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(Tone.Ink.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) { BasicText("›", style = TextStyle(color = Color.White, fontSize = 14.sp)) }
    }
}
