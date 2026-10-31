package blue.deniz.vantage.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import blue.deniz.vantage.app.resources.Res
import blue.deniz.vantage.app.resources.allDrawableResources
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val REGIONAL_INDICATOR_HIGH_SURROGATE = '\uD83C'
private val REGIONAL_INDICATOR_LOW_SURROGATES = '\uDDE6'..'\uDDFF'
private const val FLAG_EMOJI_LENGTH = 4
private const val FLAG_CONTENT_PREFIX = "flag:"
private const val FLAG_HEIGHT_EM = 0.85

// Twemoji draws each flag 36 wide and 26 tall, centred in a 36 by 36 square.
private const val TWEMOJI_FLAG_ASPECT_RATIO = 36.0 / 26.0
private val FLAG_PLACEHOLDER = Placeholder(
    width = (FLAG_HEIGHT_EM * TWEMOJI_FLAG_ASPECT_RATIO).em,
    height = FLAG_HEIGHT_EM.em,
    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
)

class TextWithFlagImages(val text: AnnotatedString, val inlineContent: Map<String, InlineTextContent>)

fun textWithFlagImages(text: String): TextWithFlagImages? {
    if (REGIONAL_INDICATOR_HIGH_SURROGATE !in text) return null
    val codes = mutableSetOf<String>()
    val annotated = buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val code = text.flagCodeAt(index)?.takeIf { flagImageOf(it) != null }
            if (code == null) {
                append(text[index])
                index++
                continue
            }
            appendInlineContent(FLAG_CONTENT_PREFIX + code, text.substring(index, index + FLAG_EMOJI_LENGTH))
            codes += code
            index += FLAG_EMOJI_LENGTH
        }
    }
    if (codes.isEmpty()) return null
    return TextWithFlagImages(annotated, codes.associate { code -> FLAG_CONTENT_PREFIX + code to InlineTextContent(FLAG_PLACEHOLDER) { FlagImage(code) } })
}

private fun String.flagCodeAt(index: Int): String? {
    if (index + FLAG_EMOJI_LENGTH > length) return null
    if (this[index] != REGIONAL_INDICATOR_HIGH_SURROGATE || this[index + 2] != REGIONAL_INDICATOR_HIGH_SURROGATE) return null
    val first = this[index + 1]
    val second = this[index + 3]
    if (first !in REGIONAL_INDICATOR_LOW_SURROGATES || second !in REGIONAL_INDICATOR_LOW_SURROGATES) return null
    return "${'A' + (first - REGIONAL_INDICATOR_LOW_SURROGATES.first)}${'A' + (second - REGIONAL_INDICATOR_LOW_SURROGATES.first)}"
}

private fun flagImageOf(code: String): DrawableResource? = Res.allDrawableResources["flag_${code.lowercase()}"]

@Composable
private fun FlagImage(code: String) {
    val resource = flagImageOf(code) ?: return
    Image(painterResource(resource), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}
