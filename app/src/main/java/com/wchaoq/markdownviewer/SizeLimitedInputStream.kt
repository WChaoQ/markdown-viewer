package com.wchaoq.markdownviewer

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Limits the total number of bytes exposed to a reader.
 *
 * One extra byte is probed when the limit is reached so oversized input is
 * rejected instead of being silently truncated.
 */
internal class SizeLimitedInputStream(
    input: InputStream,
    private val maxBytes: Int
) : FilterInputStream(input) {
    private var bytesRead = 0L

    init {
        require(maxBytes >= 0) { "maxBytes must not be negative" }
    }

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) addBytes(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || length > buffer.size - offset) {
            throw IndexOutOfBoundsException()
        }
        if (length == 0) return 0

        val remainingPlusOne = (maxBytes.toLong() - bytesRead + 1L).coerceAtLeast(1L)
        val count = super.read(buffer, offset, minOf(length.toLong(), remainingPlusOne).toInt())
        if (count > 0) addBytes(count)
        return count
    }

    override fun skip(count: Long): Long {
        if (count <= 0L) return 0L
        val remainingPlusOne = (maxBytes.toLong() - bytesRead + 1L).coerceAtLeast(1L)
        val skipped = super.skip(minOf(count, remainingPlusOne))
        if (skipped > 0L) addBytes(skipped)
        return skipped
    }

    override fun markSupported(): Boolean = false

    override fun mark(readLimit: Int) = Unit

    override fun reset() {
        throw IOException("Mark/reset is not supported.")
    }

    private fun addBytes(count: Long) {
        bytesRead += count
        if (bytesRead > maxBytes.toLong()) {
            throw DocumentTooLargeException()
        }
    }
}
