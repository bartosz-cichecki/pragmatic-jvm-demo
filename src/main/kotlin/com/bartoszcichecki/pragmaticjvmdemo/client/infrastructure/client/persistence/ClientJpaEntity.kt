package com.bartoszcichecki.pragmaticjvmdemo.client.infrastructure.client.persistence

import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.Client
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.ClientId
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.ClientName
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.outside.ClientTimeProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "clients", schema = "client")
internal class ClientJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "name", nullable = false, length = 120)
    val name: String,
    @Column(name = "description")
    val description: String?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deactivated_at")
    val deactivatedAt: Instant?,
) {
    fun toDomain(timeProvider: ClientTimeProvider): Client =
        Client.restore(
            state =
                Client.PersistenceState(
                    id = ClientId(id),
                    name = ClientName.of(name),
                    description = description,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    deactivatedAt = deactivatedAt,
                ),
            timeProvider = timeProvider,
        )

    companion object {
        fun from(client: Client): ClientJpaEntity {
            val state = client.persistenceState()

            return ClientJpaEntity(
                id = state.id.value,
                name = state.name.value,
                description = state.description,
                createdAt = state.createdAt,
                updatedAt = state.updatedAt,
                deactivatedAt = state.deactivatedAt,
            )
        }
    }
}
