package com.bartoszcichecki.pragmaticjvmdemo.health

import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.datasource.hikari.connection-timeout=1000"],
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseHealthIntegrationTest
    @Autowired
    constructor(
        @LocalServerPort private val port: Int,
    ) {
        private val httpClient = HttpClient.newHttpClient()

        @Test
        fun `readiness follows PostgreSQL availability while liveness remains up`() {
            assertHealth("readiness", expectedStatusCode = 200, expectedStatus = "UP")
            assertHealth("liveness", expectedStatusCode = 200, expectedStatus = "UP")

            postgres.stop()

            await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted {
                    assertHealth("readiness", expectedStatusCode = 503, expectedStatus = "DOWN")
                }

            assertHealth("liveness", expectedStatusCode = 200, expectedStatus = "UP")
        }

        private fun assertHealth(
            group: String,
            expectedStatusCode: Int,
            expectedStatus: String,
        ) {
            val response =
                httpClient.send(
                    HttpRequest
                        .newBuilder(URI("http://localhost:$port/actuator/health/$group"))
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )

            assertEquals(expectedStatusCode, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"$expectedStatus\""))
        }

        companion object {
            @Container
            @ServiceConnection
            @JvmField
            val postgres = PostgreSQLContainer("postgres:17.6-alpine")
        }
    }
