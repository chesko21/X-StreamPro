package com.chesko.x_streampro.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val username: String? = null,
    val status: String? = null,
    @SerialName("exp_date") val expDate: String? = null,
    @SerialName("is_trial") val isTrial: String? = null,
    val active_cons: String? = null,
    @SerialName("max_connections") val maxConnections: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val auth: Int? = null
)