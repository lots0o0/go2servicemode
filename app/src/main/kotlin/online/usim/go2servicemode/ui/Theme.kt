package online.usim.go2servicemode.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual tokens.
 *
 * The rule this file exists to enforce: only four levels of emphasis are
 * allowed (title / body / caption / mono), and weight comes from a component's
 * *role* - a safety notice must never look like an acknowledgement entry.
 */
object Tone {
    val Ink = Color(0xFF111318)
    val InkSoft = Color(0xFF6B7280)
    val InkFaint = Color(0xFF98A1B0)
    val PageBg = Color(0xFFF4F6FA)
    val PageBgTop = Color(0xFFF8FAFE)
    val Card = Color(0xFFFFFFFF)
    val CardSubtle = Color(0xFFF2F5FA)
    val Field = Color(0xFFF4F6FB)
    val Hairline = Color(0xFFE6EAF1)

    val Accent = Color(0xFF2F6BFF)
    val Danger = Color(0xFFD14343)
    val DangerBg = Color(0xFFFDF4F4)

    val OkFg = Color(0xFF1B7F4B)
    val OkBg = Color(0xFFDCF3E4)
    val BusyFg = Color(0xFF1D5FD0)
    val BusyBg = Color(0xFFDDE8FF)
    val FailFg = Color(0xFFB4232B)
    val FailBg = Color(0xFFFBE1E2)
    val IdleFg = Color(0xFF78808E)
    val IdleBg = Color(0xFFEDF0F5)
}

val CardShape = RoundedCornerShape(22.dp)
val InnerShape = RoundedCornerShape(16.dp)
val ChipShape = RoundedCornerShape(50)

/** Page title: noun only. No "what this page is" sentence underneath. */
val TitleStyle = TextStyle(
    color = Tone.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold
)

/** Card heading. */
val SectionStyle = TextStyle(
    color = Tone.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
)

val BodyStyle = TextStyle(color = Tone.Ink, fontSize = 14.sp, lineHeight = 20.sp)
val BodySoftStyle = TextStyle(color = Tone.InkSoft, fontSize = 13.sp, lineHeight = 19.sp)

/** Parameters, byte counts, build stamps: present but never competing. */
val CaptionStyle = TextStyle(color = Tone.InkFaint, fontSize = 11.sp)
val MonoStyle = TextStyle(
    color = Color(0xFF44506A), fontSize = 11.sp, fontFamily = FontFamily.Monospace
)
