package com.rabbit.magicphotos.data.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ResourcesResponse(
    val resources: List<String>
)

