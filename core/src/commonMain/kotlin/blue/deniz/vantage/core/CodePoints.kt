package blue.deniz.vantage.core

internal fun codePointText(codePoint: Int): String? = when {
    codePoint in 1..0xFFFF && codePoint !in 0xD800..0xDFFF -> codePoint.toChar().toString()
    codePoint in 0x10000..0x10FFFF -> {
        val offset = codePoint - 0x10000
        charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
    }
    else -> null
}
