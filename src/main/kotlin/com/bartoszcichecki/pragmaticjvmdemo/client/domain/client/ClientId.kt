package com.bartoszcichecki.pragmaticjvmdemo.client.domain.client

import java.util.UUID

@JvmInline
value class ClientId(
    val value: UUID,
)
