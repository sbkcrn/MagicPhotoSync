package com.rabbit.magicphotos.data.api

import com.rabbit.magicphotos.data.api.models.FetchJournalRequest
import com.rabbit.magicphotos.data.api.models.JournalResponse
import com.rabbit.magicphotos.data.api.models.ResourcesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RabbitHoleApi {
    
    companion object {
        const val BASE_URL = "https://hole.rabbit.tech/"
    }
    
    /**
     * Fetch all journal entries for the authenticated user.
     * 
     * @param request Request containing the JWT access token
     * @return JournalResponse containing all entries
     */
    @POST("apis/fetchUserJournal")
    suspend fun fetchUserJournal(
        @Body request: FetchJournalRequest
    ): JournalResponse
    
    /**
     * Convert S3 URIs to pre-signed download URLs.
     * 
     * @param accessToken The JWT access token
     * @param urls JSON array of S3 URIs to convert
     * @return ResourcesResponse containing signed HTTPS URLs
     */
    @GET("apis/fetchJournalEntryResources")
    suspend fun fetchJournalEntryResources(
        @Query("accessToken") accessToken: String,
        @Query("urls") urls: String // JSON array as string
    ): ResourcesResponse
}

