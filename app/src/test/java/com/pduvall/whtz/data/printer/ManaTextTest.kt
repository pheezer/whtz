package com.pduvall.whtz.data.printer

import org.junit.Assert.assertEquals
import org.junit.Test

class ManaTextTest {

    @Test
    fun `basic symbols strip braces`() {
        assertEquals("1WW", ManaText.toAscii("{1}{W}{W}"))
        assertEquals("2R", ManaText.toAscii("{2}{R}"))
    }

    @Test
    fun `hybrid and phyrexian keep slashes`() {
        assertEquals("2/W", ManaText.toAscii("{2/W}"))
        assertEquals("W/U", ManaText.toAscii("{W/U}"))
        assertEquals("W/P", ManaText.toAscii("{W/P}"))
    }

    @Test
    fun `symbols inside rules text are converted`() {
        assertEquals(
            "T: Add G.",
            ManaText.toAscii("{T}: Add {G}."),
        )
    }

    @Test
    fun `null and blank`() {
        assertEquals("", ManaText.toAscii(null))
        assertEquals("plain text", ManaText.toAscii("plain text"))
    }
}
