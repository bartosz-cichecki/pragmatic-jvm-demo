package com.bartoszcichecki.pragmaticjvmdemo.client.infrastructure.configuration

import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.outside.ClientTimeProvider
import com.bartoszcichecki.pragmaticjvmdemo.client.infrastructure.client.outside.ClockClientTimeProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration(proxyBeanMethods = false)
internal class ClientInfrastructureConfiguration {
    @Bean
    fun clientClock(): Clock = Clock.systemUTC()

    @Bean
    fun clientTimeProvider(clientClock: Clock): ClientTimeProvider = ClockClientTimeProvider(clientClock)
}
