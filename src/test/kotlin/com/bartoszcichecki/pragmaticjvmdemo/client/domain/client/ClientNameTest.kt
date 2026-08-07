package com.bartoszcichecki.pragmaticjvmdemo.client.domain.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ClientNameTest {
    @Test
    fun `normalizes surrounding whitespace`() {
        assertEquals("Acme   Corporation", ClientName.of(" \tAcme   Corporation\n").value)
    }

    @Test
    fun `accepts a name at the maximum length`() {
        val name = "a".repeat(120)

        assertEquals(name, ClientName.of(name).value)
    }

    @Test
    fun `rejects a name above the maximum length after normalization`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClientName.of(" ${"a".repeat(121)} ")
        }
    }

    @Test
    fun `rejects an empty name`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClientName.of("")
        }
    }

    @Test
    fun `rejects a whitespace-only name`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClientName.of(" \t\n ")
        }
    }

    @Test
    fun `rejects a name containing only non-breaking whitespace`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClientName.of("\u00A0")
        }
    }
}
