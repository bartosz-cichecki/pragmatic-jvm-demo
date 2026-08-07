package com.bartoszcichecki.pragmaticjvmdemo.client.infrastructure.client.outside

import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.outside.ClientTimeProvider
import java.time.Clock
import java.time.Instant

internal class ClockClientTimeProvider(
    private val clock: Clock,
) : ClientTimeProvider {
    override fun currentTime(): Instant = clock.instant()
}
