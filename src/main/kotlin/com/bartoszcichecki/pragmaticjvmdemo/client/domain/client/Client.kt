package com.bartoszcichecki.pragmaticjvmdemo.client.domain.client

import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.outside.ClientTimeProvider
import java.time.Instant

class Client private constructor(
    private val id: ClientId,
    private var name: ClientName,
    private var description: String?,
    private val timeProvider: ClientTimeProvider,
    private val createdAt: Instant,
    private var updatedAt: Instant,
    private var deactivatedAt: Instant?,
) {
    companion object {
        fun create(
            id: ClientId,
            name: ClientName,
            description: String?,
            timeProvider: ClientTimeProvider,
        ): Client {
            val createdAt = timeProvider.currentTime()

            return Client(
                id = id,
                name = name,
                description = description,
                timeProvider = timeProvider,
                createdAt = createdAt,
                updatedAt = createdAt,
                deactivatedAt = null,
            )
        }

        internal fun restore(
            state: PersistenceState,
            timeProvider: ClientTimeProvider,
        ): Client =
            Client(
                id = state.id,
                name = state.name,
                description = state.description,
                timeProvider = timeProvider,
                createdAt = state.createdAt,
                updatedAt = state.updatedAt,
                deactivatedAt = state.deactivatedAt,
            )
    }

    fun rename(newName: ClientName): Boolean {
        ensureActive()
        if (name == newName) {
            return false
        }

        name = newName
        updatedAt = timeProvider.currentTime()
        return true
    }

    fun changeDescription(newDescription: String?): Boolean {
        ensureActive()
        if (description == newDescription) {
            return false
        }

        description = newDescription
        updatedAt = timeProvider.currentTime()
        return true
    }

    fun deactivate(): Boolean {
        if (deactivatedAt != null) {
            return false
        }

        val deactivatedAt = timeProvider.currentTime()
        this.deactivatedAt = deactivatedAt
        updatedAt = deactivatedAt
        return true
    }

    internal fun persistenceState(): PersistenceState =
        PersistenceState(
            id = id,
            name = name,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deactivatedAt = deactivatedAt,
        )

    private fun ensureActive() {
        check(deactivatedAt == null) { "Inactive client cannot be changed" }
    }

    internal data class PersistenceState(
        val id: ClientId,
        val name: ClientName,
        val description: String?,
        val createdAt: Instant,
        val updatedAt: Instant,
        val deactivatedAt: Instant?,
    )
}
