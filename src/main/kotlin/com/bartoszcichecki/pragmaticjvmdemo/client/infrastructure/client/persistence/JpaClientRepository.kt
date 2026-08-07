package com.bartoszcichecki.pragmaticjvmdemo.client.infrastructure.client.persistence

import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.Client
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.ClientId
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.outside.ClientTimeProvider
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.repository.ClientRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
internal class JpaClientRepository(
    private val entityManager: EntityManager,
    private val timeProvider: ClientTimeProvider,
) : ClientRepository {
    override fun save(client: Client) {
        entityManager.merge(ClientJpaEntity.from(client))
    }

    override fun findById(id: ClientId): Client? {
        val entity = entityManager.find(ClientJpaEntity::class.java, id.value)
        return entity?.toDomain(timeProvider)
    }
}
