package com.riya.dailyquotes.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model representing a quote response item from ZenQuotes API.
 * API Endpoint: https://zenquotes.io/api/random
 *
 * @property quote The text of the quote (mapped from 'q')
 * @property author The author of the quote (mapped from 'a')
 * @property htmlFormat HTML formatted quote representation (mapped from 'h')
 */
data class Quote(
    @SerializedName("q")
    val quote: String = "",

    @SerializedName("a")
    val author: String = "",

    @SerializedName("h")
    val htmlFormat: String? = null
) {
    /**
     * Cleaned display quote string.
     */
    val displayQuote: String
        get() = quote.trim()

    /**
     * Cleaned author string, falling back to "Unknown" if missing or blank.
     */
    val displayAuthor: String
        get() = if (author.isBlank() || author.trim().equals("zenquotes.io", ignoreCase = true)) {
            "Unknown"
        } else {
            author.trim()
        }

    /**
     * Formatted string for sharing via Android Sharesheet.
     */
    fun toShareableText(): String = "\"$displayQuote\"\n— $displayAuthor"
}
