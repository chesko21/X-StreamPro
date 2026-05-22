package com.chesko.x_streampro.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val baseUrl: String,
    val username: String,
    val password: String
)