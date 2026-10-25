package online.usim.go2servicemode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card weight ladder.
 *
 * Mirrors the information hierarchy the UI is supposed to have: a safety notice
 * and an acknowledgement list must not be able to look the same, which was the
 * main visual defect of the earlier layout.
 */
enum class CardLevel { Primary, Danger, Normal, Subtle }

@Composable
fun ScreenHeader(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(title, style = TitleStyle)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SectionCard(
    level: CardLevel = CardLevel.Normal,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = if (level == CardLevel.Subtle) InnerShape else CardShape
    val bg = when (level) {
        CardLevel.Danger -> Tone.DangerBg
        CardLevel.Subtle -> Tone.CardSubtle
        else -> Tone.Card
    }
    val border = when (level) {
        CardLevel.Primary -> Modifier.border(1.5.dp, Tone.Accent.copy(alpha = 0.55f), shape)
        CardLevel.Danger -> Modifier.border(1.dp, Tone.Danger.copy(alpha = 0.35f), shape)
        else -> Modifier
    }
    Column(
        modifier
            .fillMaxWidth()
            .then(border)
            .clip(shape)
            .background(bg)
            .padding(horizontal = 18.dp, vertical = if (level == CardLevel.Subtle) 14.dp else 17.dp),
        content = content
    )
}

/** Status chip. "Not run" is a chip too - bare grey text is too easy to miss. */
@Composable
fun StatusChip(label: String, fg: Color, bg: Color) {
    Box(
        Modifier
            .clip(ChipShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        BasicText(label, style = androidx.compose.ui.text.TextStyle(color = fg, fontSize = 11.sp))
    }
}

@Composable
fun ActionRow(
    title: String,
    subtitle: String? = null,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(InnerShape)
            .background(if (enabled) Tone.Field else Tone.CardSubtle)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                title,
                style = BodyStyle.copy(
                    color = if (enabled) Tone.Ink else Tone.InkFaint
                )
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                BasicText(subtitle, style = CaptionStyle)
            }
        }
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .background(if (enabled) Tone.Ink else Tone.IdleBg),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "›",
                style = androidx.compose.ui.text.TextStyle(
                    color = if (enabled) Color.White else Tone.InkFaint,
                    fontSize = 14.sp
                )
            )
        }
    }
}

/** Small, low-weight button for secondary actions (view / export / delete). */
@Composable
fun GhostButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(ChipShape)
            .background(Tone.Field)
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        BasicText(label, style = BodySoftStyle.copy(color = Tone.Ink, fontSize = 12.sp))
    }
}

/**
 * Disclosure header. Engineering detail lives here, collapsed, so the page can
 * stay free of it without hiding it from anyone who wants it.
 */
@Composable
fun CollapsibleHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(InnerShape)
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(title, style = SectionStyle.copy(fontSize = 14.sp))
        Spacer(Modifier.weight(1f))
        BasicText(
            if (expanded) "收起" else "展开",
            style = CaptionStyle.copy(color = Tone.Accent)
        )
    }
}

/** Caption used for one-line hints under a component. */
@Composable
fun Hint(text: String) {
    BasicText(text, style = CaptionStyle)
}
