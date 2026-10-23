package com.riya.dailyquotes.ui

import com.riya.dailyquotes.data.model.Quote

/**
 * Represents the distinct UI states for the Daily Quotes screen.
 */
sealed interface QuoteUiState {

    /**
     * Loading state while a quote is being fetched from the API.
     */
    data object Loading : QuoteUiState

    /**
     * Success state with the real quote returned by the API.
     */
    data class Success(val quote: Quote) : QuoteUiState

    /**
     * Error state when the network request fails or device is offline.
     */
    data class Error(
        val title: String = "Couldn't load a quote",
        val message: String = "Please check your internet connection and try again."
    ) : QuoteUiState
}
