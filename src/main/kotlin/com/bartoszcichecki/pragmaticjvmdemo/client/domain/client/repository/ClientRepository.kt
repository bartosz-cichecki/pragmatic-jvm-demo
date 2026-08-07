package com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.repository

import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.Client
import com.bartoszcichecki.pragmaticjvmdemo.client.domain.client.ClientId

interface ClientRepository {
    fun save(client: Client)

    fun findById(id: ClientId): Client?
}
