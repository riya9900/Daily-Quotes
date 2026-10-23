package com.riya.dailyquotes.data.api

import com.riya.dailyquotes.data.model.Quote
import retrofit2.http.GET

/**
 * Retrofit service interface for ZenQuotes REST API.
 * Base URL: https://zenquotes.io/
 */
interface ZenQuotesApiService {

    /**
     * Fetches a random quote.
     * ZenQuotes returns a JSON array containing one quote object:
     * e.g., [{"q":"...","a":"...","h":"..."}]
     */
    @GET("api/random")
    suspend fun getRandomQuote(): List<Quote>
}
