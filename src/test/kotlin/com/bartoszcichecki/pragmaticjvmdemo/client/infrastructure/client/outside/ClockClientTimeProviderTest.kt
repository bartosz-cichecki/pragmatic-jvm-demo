package com.bartoszcichecki.pragmaticjvmdemo.client.infrastructure.client.outside

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ClockClientTimeProviderTest {
    @Test
    fun `obtains current time from the injected clock`() {
        val currentTime = Instant.parse("2026-08-07T10:15:30Z")
        val provider = ClockClientTimeProvider(Clock.fixed(currentTime, ZoneOffset.UTC))

        assertEquals(currentTime, provider.currentTime())
    }
}
