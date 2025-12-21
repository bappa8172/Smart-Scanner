package com.smartprivacy.scanner.data

import com.smartprivacy.scanner.analyzer.VTResponse
import com.smartprivacy.scanner.analyzer.VirusTotalService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VirusTotalRepository {
    private val apiKey = "17d67ff8a49991792bf39349152062c7d7634b8f76aa19fc2d8f60b4a50c61b0"

    private val service: VirusTotalService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(VirusTotalService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(VirusTotalService::class.java)
    }

    suspend fun getAppReport(sha256: String): VTResponse? {
        return try {
            val response = service.getFileReport(apiKey, sha256)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
