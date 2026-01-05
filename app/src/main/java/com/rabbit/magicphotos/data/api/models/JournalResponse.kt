package com.rabbit.magicphotos.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JournalResponse(
    val journal: Journal
)

@Serializable
data class Journal(
    val entries: List<JournalEntry>
)

@Serializable
data class JournalEntry(
    @SerialName("_id")
    val id: String,
    val userId: String,
    val createdOn: String,
    val modifiedOn: String,
    val archived: Boolean = false,
    val type: String,
    val title: String,
    val data: EntryData? = null,
    val utterance: Utterance? = null
)

@Serializable
data class EntryData(
    val magicCameraData: MagicCameraData? = null,
    val betaRabbitData: BetaRabbitData? = null
)

@Serializable
data class MagicCameraData(
    val originalImage: ImageUrl? = null,
    val aiGeneratedImages: List<ImageUrl> = emptyList(),
    val imageDescription: String = ""
)

@Serializable
data class ImageUrl(
    val url: String
)

@Serializable
data class BetaRabbitData(
    val textContent: String = "",
    val titleImageUrl: String = ""
)

@Serializable
data class Utterance(
    val prompt: String = "",
    val intention: String = ""
)

// Request body for fetchUserJournal
@Serializable
data class FetchJournalRequest(
    val accessToken: String
)

// Helper extension to check if entry is a Magic Camera photo
fun JournalEntry.isMagicCamera(): Boolean = type == "magic-camera"

// Helper to get all S3 URLs from a Magic Camera entry
fun JournalEntry.getS3Urls(): List<String> {
    if (!isMagicCamera()) return emptyList()
    
    val urls = mutableListOf<String>()
    data?.magicCameraData?.originalImage?.url?.let { urls.add(it) }
    data?.magicCameraData?.aiGeneratedImages?.forEach { urls.add(it.url) }
    return urls
}

