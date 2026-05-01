package com.papoter.app.data

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

interface OllamaApiService {
    @GET("api/tags")
    suspend fun listModels(): Response<OllamaTagResponse>

    @POST("api/chat")
    suspend fun chat(@Body request: OllamaChatRequest): Response<OllamaChatResponse>

    @Streaming
    @POST("api/chat")
    suspend fun chatStream(@Body request: OllamaChatRequest): ResponseBody

    @POST("api/generate")
    suspend fun generate(@Body request: Map<String, Any>): Response<OllamaChatResponse>
}

object ApiClient {
    fun create(baseUrl: String): OllamaApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OllamaApiService::class.java)
    }
}
