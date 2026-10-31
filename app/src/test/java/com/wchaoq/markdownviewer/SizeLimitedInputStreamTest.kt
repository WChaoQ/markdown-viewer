package com.wchaoq.markdownviewer

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class SizeLimitedInputStreamTest {
    @Test
    fun acceptsInputExactlyAtLimit() {
        val expected = byteArrayOf(1, 2, 3, 4)
        val input = SizeLimitedInputStream(ByteArrayInputStream(expected), expected.size)

        assertArrayEquals(expected, input.readBytes())
    }

    @Test
    fun rejectsInputOneByteOverLimitWithSpecificException() {
        val input = SizeLimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 2)

        assertThrows(DocumentTooLargeException::class.java) { input.readBytes() }
    }

    @Test
    fun rejectsOversizedInputWhenReadingSingleBytes() {
        val input = SizeLimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2)), 1)

        assertEquals(1, input.read())
        assertThrows(DocumentTooLargeException::class.java) { input.read() }
    }

    @Test
    fun rejectsOversizedInputWhenSkippingBytes() {
        val input = SizeLimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 2)

        assertThrows(DocumentTooLargeException::class.java) { input.skip(3) }
    }

    @Test
    fun acceptsEmptyInputWithZeroLimit() {
        val input = SizeLimitedInputStream(ByteArrayInputStream(byteArrayOf()), 0)

        assertEquals(-1, input.read())
    }

    @Test
    fun rejectsNegativeLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            SizeLimitedInputStream(ByteArrayInputStream(byteArrayOf()), -1)
        }
    }

    @Test
    fun doesNotSupportMarkAndReset() {
        val input = SizeLimitedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)

        assertFalse(input.markSupported())
        assertThrows(IOException::class.java) { input.reset() }
    }
}
