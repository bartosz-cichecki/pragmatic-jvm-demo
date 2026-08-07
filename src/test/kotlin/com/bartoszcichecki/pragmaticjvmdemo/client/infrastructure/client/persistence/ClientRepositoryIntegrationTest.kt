package com.bartoszcichecki.pragmaticjvmdemo.client.infrastructure.client.persistence

import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.Client
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.ClientId
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.ClientName
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.outside.ClientTimeProvider
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.repository.ClientRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.TransactionRequiredException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

@Testcontainers
@SpringBootTest
@Transactional
class ClientRepositoryIntegrationTest
    @Autowired
    constructor(
        private val repository: ClientRepository,
        private val entityManager: EntityManager,
        private val timeProvider: ClientTimeProvider,
    ) {
        @Test
        fun `persisted client remains behaviourally valid after reload`() {
            val client =
                Client.create(
                    id = CLIENT_ID,
                    name = ClientName.of("Acme"),
                    description = "Initial description",
                    timeProvider = timeProvider,
                )

            repository.save(client)
            entityManager.flush()
            entityManager.clear()

            val reloaded = requireNotNull(repository.findById(CLIENT_ID))

            assertTrue(reloaded.rename(ClientName.of("Renamed after reload")))
            assertTrue(reloaded.changeDescription("Changed after reload"))

            repository.save(reloaded)
            entityManager.flush()
            entityManager.clear()

            val reloadedAgain = requireNotNull(repository.findById(CLIENT_ID))

            assertFalse(reloadedAgain.rename(ClientName.of("Renamed after reload")))
            assertFalse(reloadedAgain.changeDescription("Changed after reload"))

            assertTrue(reloadedAgain.deactivate())
            repository.save(reloadedAgain)
            entityManager.flush()
            entityManager.clear()

            val reloadedInactive = requireNotNull(repository.findById(CLIENT_ID))

            assertFalse(reloadedInactive.deactivate())
            assertThrows(IllegalStateException::class.java) {
                reloadedInactive.rename(ClientName.of("Rejected rename"))
            }
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        fun `repository does not open a transaction for save`() {
            val client =
                Client.create(
                    id = CLIENT_WITHOUT_TRANSACTION_ID,
                    name = ClientName.of("No transaction"),
                    description = null,
                    timeProvider = timeProvider,
                )

            val exception =
                assertThrows(InvalidDataAccessApiUsageException::class.java) {
                    repository.save(client)
                }

            assertInstanceOf(TransactionRequiredException::class.java, exception.cause)
        }

        companion object {
            @Container
            @ServiceConnection
            @JvmField
            val postgres = PostgreSQLContainer("postgres:17.6-alpine")

            private val CLIENT_ID = ClientId(UUID.fromString("8ea6ea10-dad1-43c1-9955-88ee88274b0d"))
            private val CLIENT_WITHOUT_TRANSACTION_ID =
                ClientId(UUID.fromString("3122767a-d5f3-4214-89ee-a829d5f8bc8c"))
        }
    }
