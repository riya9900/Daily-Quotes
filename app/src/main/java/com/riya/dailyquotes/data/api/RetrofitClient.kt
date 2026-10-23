package com.riya.dailyquotes.data.api

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object that creates and provides the Retrofit instance for ZenQuotes API.
 * Configures OkHttpClient with appropriate timeouts, HTTP/1.1 protocol, standard User-Agent,
 * and comprehensive request/response logging to ensure reliable quote fetching on all devices.
 */
object RetrofitClient {

    private const val TAG = "DailyQuotes"
    private const val BASE_URL = "https://zenquotes.io/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Force HTTP/1.1 to avoid Apache server ALPN/HTTP2 stream drops on mobile carriers
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                // Attach a standard Mobile User-Agent and JSON headers to avoid CDN/carrier blocks
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android) DailyQuotes/1.0")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .build()

                Log.d(TAG, "--> Sending request: ${requestWithHeaders.method()} ${requestWithHeaders.url()}")
                val startTime = System.currentTimeMillis()
                try {
                    val response = chain.proceed(requestWithHeaders)
                    val durationMs = System.currentTimeMillis() - startTime
                    Log.d(TAG, "<-- Response: code=${response.code()} in ${durationMs}ms for ${requestWithHeaders.url()}")
                    response
                } catch (e: Exception) {
                    val durationMs = System.currentTimeMillis() - startTime
                    Log.e(TAG, "<-- Request failed after ${durationMs}ms for ${requestWithHeaders.url()}: ${e.message}", e)
                    throw e
                }
            }
            .build()
    }

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ZenQuotesApiService by lazy {
        retrofit.create(ZenQuotesApiService::class.java)
    }
}
