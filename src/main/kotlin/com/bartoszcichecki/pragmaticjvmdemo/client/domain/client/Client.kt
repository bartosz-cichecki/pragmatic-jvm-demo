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

    private fun ensureActive() {
        check(deactivatedAt == null) { "Inactive client cannot be changed" }
    }
}
