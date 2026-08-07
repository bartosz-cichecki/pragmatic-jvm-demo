package com.bartoszcichecki.pragmaticjvmdemo.client.domain.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ClientIdTest {
    @Test
    fun `identifiers with the same value are equal`() {
        val value = UUID.fromString("8ea6ea10-dad1-43c1-9955-88ee88274b0d")

        assertEquals(ClientId(value), ClientId(value))
        assertNotEquals(
            ClientId(value),
            ClientId(UUID.fromString("3122767a-d5f3-4214-89ee-a829d5f8bc8c")),
        )
    }
}
