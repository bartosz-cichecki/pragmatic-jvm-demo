package com.bartoszcichecki.pragmaticjvmdemo.client.domain.client

import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.outside.ClientTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ClientTest {
    @Test
    fun `creates an active client with a name and optional description`() {
        val timeProvider = FakeClientTimeProvider(CREATED_AT)
        val client = createClient(timeProvider, description = "Important client")

        assertFalse(client.rename(ClientName.of("Acme")))
        assertFalse(client.changeDescription("Important client"))
        assertEquals(listOf(CREATED_AT), timeProvider.providedTimes)
    }

    @Test
    fun `renames an active client and treats the same normalized name as no-op`() {
        val timeProvider = FakeClientTimeProvider(CREATED_AT, RENAMED_AT)
        val client = createClient(timeProvider)

        assertTrue(client.rename(ClientName.of("New name")))
        assertFalse(client.rename(ClientName.of("  New name\t")))
        assertEquals(listOf(CREATED_AT, RENAMED_AT), timeProvider.providedTimes)
    }

    @Test
    fun `sets changes and clears a description`() {
        val timeProvider =
            FakeClientTimeProvider(CREATED_AT, DESCRIPTION_SET_AT, DESCRIPTION_CHANGED_AT, DESCRIPTION_CLEARED_AT)
        val client = createClient(timeProvider)

        assertTrue(client.changeDescription("Initial description"))
        assertFalse(client.changeDescription("Initial description"))
        assertTrue(client.changeDescription("Changed description"))
        assertTrue(client.changeDescription(null))
        assertFalse(client.changeDescription(null))
        assertEquals(
            listOf(CREATED_AT, DESCRIPTION_SET_AT, DESCRIPTION_CHANGED_AT, DESCRIPTION_CLEARED_AT),
            timeProvider.providedTimes,
        )
    }

    @Test
    fun `deactivates an active client`() {
        val timeProvider = FakeClientTimeProvider(CREATED_AT, DEACTIVATED_AT)
        val client = createClient(timeProvider)

        assertTrue(client.deactivate())
        assertEquals(listOf(CREATED_AT, DEACTIVATED_AT), timeProvider.providedTimes)
    }

    @Test
    fun `repeated deactivation is an idempotent no-op`() {
        val timeProvider = FakeClientTimeProvider(CREATED_AT, DEACTIVATED_AT)
        val client = createClient(timeProvider)

        assertTrue(client.deactivate())
        assertFalse(client.deactivate())
        assertEquals(listOf(CREATED_AT, DEACTIVATED_AT), timeProvider.providedTimes)
    }

    @Test
    fun `rejects renaming an inactive client even when the name is unchanged`() {
        val timeProvider = FakeClientTimeProvider(CREATED_AT, DEACTIVATED_AT)
        val client = createClient(timeProvider)
        client.deactivate()

        assertThrows(IllegalStateException::class.java) {
            client.rename(ClientName.of("Acme"))
        }
        assertEquals(listOf(CREATED_AT, DEACTIVATED_AT), timeProvider.providedTimes)
    }

    @Test
    fun `rejects description changes on an inactive client including clearing and no-op`() {
        val timeProvider = FakeClientTimeProvider(CREATED_AT, DEACTIVATED_AT)
        val client = createClient(timeProvider, description = "Initial description")
        client.deactivate()

        assertThrows(IllegalStateException::class.java) {
            client.changeDescription("Changed description")
        }
        assertThrows(IllegalStateException::class.java) {
            client.changeDescription(null)
        }
        assertThrows(IllegalStateException::class.java) {
            client.changeDescription("Initial description")
        }
        assertEquals(listOf(CREATED_AT, DEACTIVATED_AT), timeProvider.providedTimes)
    }

    private fun createClient(
        timeProvider: ClientTimeProvider,
        description: String? = null,
    ): Client =
        Client.create(
            id = CLIENT_ID,
            name = ClientName.of("Acme"),
            description = description,
            timeProvider = timeProvider,
        )

    private class FakeClientTimeProvider(
        vararg times: Instant,
    ) : ClientTimeProvider {
        private val remainingTimes = ArrayDeque(times.toList())
        val providedTimes = mutableListOf<Instant>()

        override fun currentTime(): Instant =
            remainingTimes.removeFirst().also {
                providedTimes += it
            }
    }

    private companion object {
        val CLIENT_ID = ClientId(UUID.fromString("8ea6ea10-dad1-43c1-9955-88ee88274b0d"))
        val CREATED_AT: Instant = Instant.parse("2026-01-02T03:04:05Z")
        val RENAMED_AT: Instant = Instant.parse("2026-01-03T03:04:05Z")
        val DESCRIPTION_SET_AT: Instant = Instant.parse("2026-01-04T03:04:05Z")
        val DESCRIPTION_CHANGED_AT: Instant = Instant.parse("2026-01-05T03:04:05Z")
        val DESCRIPTION_CLEARED_AT: Instant = Instant.parse("2026-01-06T03:04:05Z")
        val DEACTIVATED_AT: Instant = Instant.parse("2026-01-07T03:04:05Z")
    }
}
