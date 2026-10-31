package blue.deniz.vantage.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow

object Blurhash {
    private const val BASE83_DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~"

    fun decodeToArgb(hash: String, width: Int, height: Int): IntArray? {
        if (hash.length < 6 || hash.any { it !in BASE83_DIGITS }) return null
        val sizeFlag = decodeBase83(hash, 0, 1)
        val componentsX = sizeFlag % 9 + 1
        val componentsY = sizeFlag / 9 + 1
        if (hash.length != 4 + 2 * componentsX * componentsY) return null
        val maxAc = (decodeBase83(hash, 1, 2) + 1) / 166.0
        val colors = Array(componentsX * componentsY) { i ->
            if (i == 0) {
                val value = decodeBase83(hash, 2, 6)
                doubleArrayOf(srgbToLinear(value shr 16), srgbToLinear((value shr 8) and 255), srgbToLinear(value and 255))
            } else {
                val value = decodeBase83(hash, 4 + i * 2, 6 + i * 2)
                val quantised = intArrayOf(value / (19 * 19), value / 19 % 19, value % 19)
                DoubleArray(3) { channel -> signedSquare((quantised[channel] - 9) / 9.0) * maxAc }
            }
        }
        val cosX = Array(width) { x -> DoubleArray(componentsX) { i -> cos(PI * x * i / width) } }
        val cosY = Array(height) { y -> DoubleArray(componentsY) { j -> cos(PI * y * j / height) } }
        return IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            var r = 0.0
            var g = 0.0
            var b = 0.0
            for (j in 0 until componentsY) for (i in 0 until componentsX) {
                val basis = cosX[x][i] * cosY[y][j]
                val color = colors[i + j * componentsX]
                r += color[0] * basis
                g += color[1] * basis
                b += color[2] * basis
            }
            (0xFF shl 24) or (linearToSrgb(r) shl 16) or (linearToSrgb(g) shl 8) or linearToSrgb(b)
        }
    }

    private fun decodeBase83(hash: String, start: Int, end: Int) =
        (start until end).fold(0) { value, index -> value * 83 + BASE83_DIGITS.indexOf(hash[index]) }

    private fun signedSquare(value: Double) = value * abs(value)

    private fun srgbToLinear(value: Int): Double {
        val v = value / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun linearToSrgb(value: Double): Int {
        val v = value.coerceIn(0.0, 1.0)
        return if (v <= 0.0031308) (v * 12.92 * 255 + 0.5).toInt() else ((1.055 * v.pow(1 / 2.4) - 0.055) * 255 + 0.5).toInt()
    }
}
