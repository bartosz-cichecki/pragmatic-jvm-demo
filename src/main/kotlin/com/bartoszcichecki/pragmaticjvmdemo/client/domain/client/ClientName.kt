package com.bartoszcichecki.pragmaticjvmdemo.client.domain.client

@JvmInline
value class ClientName private constructor(
    val value: String,
) {
    companion object {
        private const val MAX_LENGTH = 120

        fun of(value: String): ClientName {
            val normalizedValue = value.trim()

            require(normalizedValue.isNotBlank()) { "Client name must not be blank" }
            require(normalizedValue.length <= MAX_LENGTH) {
                "Client name must not be longer than $MAX_LENGTH characters"
            }

            return ClientName(normalizedValue)
        }
    }
}
