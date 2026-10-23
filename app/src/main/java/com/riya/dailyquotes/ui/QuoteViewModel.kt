package com.riya.dailyquotes.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riya.dailyquotes.data.model.Quote
import com.riya.dailyquotes.data.repository.QuoteRepository
import com.riya.dailyquotes.data.repository.RateLimitException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing UI state and API requests for Daily Quotes.
 * Architecture:
 * - App startup makes ZERO requests to ZenQuotes.
 * - Single-flight execution: strictly ONE request at a time using a synchronous lock & Job.
 * - Rapid tap debouncing to ignore accidental double-clicks.
 * - Client-side sliding-window rate limiter: caps requests at 4 per 30 seconds to prevent
 *   hitting ZenQuotes free tier rate limit (5 requests per 30s per IP).
 * - After a quote is fetched successfully, it is stored in [savedQuote] and kept across navigation.
 * - HTTP 429 rate limit is handled cleanly with a 32s cooldown to prevent resetting server bans.
 * - No automatic retries, no loops, no duplicate triggers from recomposition.
 */
class QuoteViewModel(
    private val repository: QuoteRepository = QuoteRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "DailyQuotes"
        private const val RATE_LIMIT_COOLDOWN_MS = 32_000L // 32s cooldown when 429 is hit
        private const val RATE_LIMIT_WINDOW_MS = 30_000L // 30s ZenQuotes free tier window
        private const val MAX_REQUESTS_PER_WINDOW = 4 // Keep under ZenQuotes free limit of 5 per 30s
        private const val MIN_CLICK_INTERVAL_MS = 800L // Debounce rapid spam clicks
    }

    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    // Preserved quote in ViewModel state across navigation and failed refreshes
    private var savedQuote: Quote? = null

    // Track rate limit cooldown from ZenQuotes
    private var rateLimitCooldownUntilMs: Long = 0L

    // Debounce tracking
    private var lastClickTimeMs: Long = 0L

    // Sliding window of request timestamps in the last 30s
    private val requestTimestamps = ArrayDeque<Long>()

    // Active network job guard
    private var activeJob: Job? = null

    /**
     * Checks if a quote has already been loaded into state.
     */
    fun hasLoadedQuote(): Boolean = savedQuote != null

    /**
     * Called when navigating to QuoteScreen from HomeScreen.
     * If a quote is already in state, restores it without making any API request.
     * If no quote exists yet, requests exactly one quote.
     */
    fun loadInitialQuoteIfNeeded() {
        savedQuote?.let { quote ->
            Log.d(TAG, "Restoring existing quote from ViewModel state without API call.")
            _uiState.value = QuoteUiState.Success(quote)
            return
        }
        fetchQuote()
    }

    /**
     * Fetches a single fresh quote from the ZenQuotes API.
     * Enforces single-flight execution, disables duplicate rapid clicks,
     * and guards against HTTP 429 penalty extensions.
     */
    fun fetchQuote() {
        val now = System.currentTimeMillis()

        // 1. Debounce rapid double-clicks
        if (now - lastClickTimeMs < MIN_CLICK_INTERVAL_MS) {
            Log.d(TAG, "Ignoring rapid click (debounced).")
            return
        }
        lastClickTimeMs = now

        // 2. Guard against duplicate calls during recomposition or while a request is running
        if (_isFetching.value || activeJob?.isActive == true) {
            Log.d(TAG, "fetchQuote called while a request is already running. Ignoring.")
            return
        }

        // 3. Guard against hammering ZenQuotes during an active rate limit cooldown (HTTP 429)
        if (now < rateLimitCooldownUntilMs) {
            val remainingSec = ((rateLimitCooldownUntilMs - now) / 1000).coerceAtLeast(1)
            Log.w(TAG, "ZenQuotes rate-limit cooldown active. Please wait ${remainingSec}s before retrying.")
            _uiState.value = QuoteUiState.Error(
                title = "Couldn't load a quote",
                message = "Too many requests. Please try again later."
            )
            return
        }

        // 4. Client-side sliding-window rate limit guard: max 4 requests per 30 seconds
        synchronized(requestTimestamps) {
            while (requestTimestamps.isNotEmpty() && (now - requestTimestamps.first()) >= RATE_LIMIT_WINDOW_MS) {
                requestTimestamps.removeFirst()
            }

            if (requestTimestamps.size >= MAX_REQUESTS_PER_WINDOW) {
                val waitSec = (((requestTimestamps.first() + RATE_LIMIT_WINDOW_MS) - now) / 1000).coerceAtLeast(1)
                Log.w(TAG, "Client rate limit reached (${requestTimestamps.size} requests in 30s). Wait ${waitSec}s to prevent HTTP 429.")
                _uiState.value = QuoteUiState.Error(
                    title = "Couldn't load a quote",
                    message = "Too many requests. Please try again later."
                )
                return
            }

            requestTimestamps.addLast(now)
        }

        // 5. Mark as fetching immediately to disable buttons and prevent recomposition triggers
        _isFetching.value = true
        _uiState.value = QuoteUiState.Loading

        activeJob = viewModelScope.launch {
            try {
                Log.d(TAG, "Executing single API request to ZenQuotes...")
                val result = repository.getRandomQuote()

                result.onSuccess { quote ->
                    Log.d(TAG, "Quote successfully received and stored in ViewModel: \"${quote.displayQuote}\"")
                    savedQuote = quote
                    rateLimitCooldownUntilMs = 0L // Clear cooldown on success
                    _uiState.value = QuoteUiState.Success(quote)
                }.onFailure { throwable ->
                    Log.e(TAG, "Quote request failed: ${throwable.message}", throwable)

                    val isRateLimit = throwable is RateLimitException ||
                        throwable.message?.contains("429", ignoreCase = true) == true ||
                        throwable.message?.contains("Too many requests", ignoreCase = true) == true

                    if (isRateLimit) {
                        rateLimitCooldownUntilMs = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
                    }

                    val friendlyMessage = if (isRateLimit) {
                        "Too many requests. Please try again later."
                    } else {
                        throwable.localizedMessage?.takeIf { it.isNotBlank() && !it.contains("HTTP", ignoreCase = true) }
                            ?: "Please check your internet connection and try again."
                    }

                    _uiState.value = QuoteUiState.Error(
                        title = "Couldn't load a quote",
                        message = friendlyMessage
                    )
                }
            } finally {
                _isFetching.value = false
            }
        }
    }
}

