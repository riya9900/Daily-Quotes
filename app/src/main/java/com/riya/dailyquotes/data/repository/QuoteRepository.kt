package com.riya.dailyquotes.data.repository

import android.util.Log
import com.riya.dailyquotes.data.api.RetrofitClient
import com.riya.dailyquotes.data.api.ZenQuotesApiService
import com.riya.dailyquotes.data.model.Quote
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Custom exception representing ZenQuotes rate limiting (HTTP 429 or rate-limit payload).
 */
class RateLimitException(
    message: String = "Too many requests. Please try again later."
) : Exception(message)

/**
 * Repository responsible for fetching quotes from the remote ZenQuotes data source.
 * Handles API responses, empty lists, rate limits (HTTP 429), and network errors gracefully with Logcat logging.
 */
class QuoteRepository(
    private val apiService: ZenQuotesApiService = RetrofitClient.apiService
) {

    companion object {
        private const val TAG = "DailyQuotes"
    }

    /**
     * Fetches a real random quote from the ZenQuotes API.
     * Makes strictly ONE request and avoids retries.
     * @return [Result.success] with the [Quote] or [Result.failure] with a user-friendly [Throwable].
     */
    suspend fun getRandomQuote(): Result<Quote> {
        return try {
            Log.d(TAG, "Requesting random quote from ZenQuotes API...")
            val responseList = apiService.getRandomQuote()
            Log.d(TAG, "ZenQuotes response received. List size: ${responseList.size}")

            if (responseList.isNotEmpty()) {
                val item = responseList.first()

                // Check for ZenQuotes rate limit message in response body
                if (item.author.equals("zenquotes.io", ignoreCase = true) ||
                    item.quote.contains("Too many requests", ignoreCase = true)
                ) {
                    val rateLimitMsg = "Too many requests. Please try again later."
                    Log.w(TAG, "ZenQuotes rate-limited response in body: ${item.quote}")
                    Result.failure(RateLimitException(rateLimitMsg))
                } else if (item.quote.isBlank()) {
                    Log.w(TAG, "ZenQuotes returned an empty quote.")
                    Result.failure(Exception("Received empty quote data."))
                } else {
                    Log.d(TAG, "Successfully retrieved quote: \"${item.quote}\" — ${item.author}")
                    Result.success(item)
                }
            } else {
                Log.w(TAG, "ZenQuotes returned an empty list.")
                Result.failure(Exception("No quote returned from the server."))
            }
        } catch (httpEx: HttpException) {
            if (httpEx.code() == 429) {
                Log.w(TAG, "ZenQuotes returned HTTP 429 Too Many Requests")
                Result.failure(RateLimitException("Too many requests. Please try again later."))
            } else {
                Log.e(TAG, "HTTP error from ZenQuotes: ${httpEx.code()} - ${httpEx.message()}", httpEx)
                Result.failure(Exception("Server returned an error (${httpEx.code()}). Please try again later."))
            }
        } catch (timeoutEx: SocketTimeoutException) {
            Log.e(TAG, "Network timeout connecting to ZenQuotes: ${timeoutEx.message}")
            Result.failure(Exception("Connection timed out. Please check your internet connection."))
        } catch (hostEx: UnknownHostException) {
            Log.e(TAG, "Unable to resolve ZenQuotes host: ${hostEx.message}")
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (ioEx: IOException) {
            Log.e(TAG, "Network I/O error fetching quote: ${ioEx.message}", ioEx)
            Result.failure(Exception("Network error. Please check your connection and try again."))
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error fetching quote: ${t.javaClass.simpleName} - ${t.message}", t)
            Result.failure(t)
        }
    }
}
