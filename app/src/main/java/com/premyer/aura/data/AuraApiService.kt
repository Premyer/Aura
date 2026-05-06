package com.premyer.aura.data

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AuraApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://ais-pre-6nunyp4pojxildv7adxgq3-523251667507.europe-west2.run.app"

    suspend fun getResponse(request: AuraRequest): AuraResponse {
        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("$baseUrl/api/assistant")
            .post(requestBody)
            .build()

        return client.newCall(httpRequest).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "{}"
                gson.fromJson(responseBody, AuraResponse::class.java)
            } else {
                AuraResponse(
                    reply = "API Error: ${response.code}",
                    status = "error"
                )
            }
        }
    }
}
