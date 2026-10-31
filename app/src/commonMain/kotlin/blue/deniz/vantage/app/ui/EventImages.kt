package blue.deniz.vantage.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import blue.deniz.vantage.app.imageBitmapFromArgb
import blue.deniz.vantage.core.Blurhash
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import directory.evnt.Media
import directory.evnt.hasText
import directory.evnt.translate
import kotlin.math.roundToInt

const val BACKGROUND_DIM_ALPHA = 0.8f
private const val BLURHASH_DECODE_SIZE = 32

private fun hexColor(hex: String) = Color(0xFF000000 or hex.removePrefix("#").toLong(16))

@Composable
fun EventBackground(media: Media, preferredLanguages: List<String>, modifier: Modifier = Modifier, dimAlpha: Float = BACKGROUND_DIM_ALPHA) {
    Box(modifier.clipToBounds()) {
        OpenEvntImage(media, preferredLanguages, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dimAlpha)))
    }
}

@Composable
fun OpenEvntImage(
    media: Media,
    preferredLanguages: List<String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onShown: () -> Unit = {},
    onFailed: () -> Unit = {},
) {
    val url = media.sources.firstOrNull()?.url ?: return
    val dominantColor = remember(media.presentation?.dominantColor) { media.presentation?.dominantColor?.let(::hexColor) }
    val blurhashPainter = remember(media.presentation?.blurhash) { media.presentation?.blurhash?.let(::blurhashPainterOrNull) }
    LaunchedEffect(dominantColor, blurhashPainter) { if (dominantColor != null || blurhashPainter != null) onShown() }
    val context = LocalPlatformContext.current
    val request = remember(url, context) { ImageRequest.Builder(context).data(url).crossfade(true).build() }
    Box(modifier) {
        if (dominantColor != null) Box(Modifier.matchParentSize().background(dominantColor))
        AsyncImage(
            model = request,
            contentDescription = media.alt?.takeIf { it.hasText() }?.translate(preferredLanguages),
            modifier = Modifier.matchParentSize(),
            placeholder = blurhashPainter,
            error = blurhashPainter,
            contentScale = contentScale,
            onSuccess = { onShown() },
            onError = { onFailed() },
        )
    }
}

private fun blurhashPainterOrNull(hash: String): Painter? {
    val size = BLURHASH_DECODE_SIZE
    val pixels = Blurhash.decodeToArgb(hash, size, size) ?: return null
    return StretchedImagePainter(imageBitmapFromArgb(pixels, size, size))
}

private class StretchedImagePainter(private val image: ImageBitmap) : Painter() {
    override val intrinsicSize get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawImage(image, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
    }
}
