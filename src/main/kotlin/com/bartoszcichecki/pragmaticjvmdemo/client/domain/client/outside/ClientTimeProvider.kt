package com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.outside

import java.time.Instant

fun interface ClientTimeProvider {
    fun currentTime(): Instant
}
