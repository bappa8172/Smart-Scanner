package com.smartprivacy.scanner.analyzer

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface VirusTotalService {
    @GET("files/{id}")
    suspend fun getFileReport(
        @Header("x-apikey") apiKey: String,
        @Path("id") fileHash: String
    ): Response<VTResponse>

    companion object {
        const val BASE_URL = "https://www.virustotal.com/api/v3/"
    }
}
