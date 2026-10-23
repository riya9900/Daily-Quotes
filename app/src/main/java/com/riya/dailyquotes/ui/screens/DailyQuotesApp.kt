package com.riya.dailyquotes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riya.dailyquotes.ui.QuoteUiState
import com.riya.dailyquotes.ui.QuoteViewModel

/**
 * Screen state enum for top-level navigation.
 */
enum class ScreenState {
    HOME,
    QUOTE
}

/**
 * Root composable coordinating screen transitions between [HomeScreen] and [QuoteScreen].
 */
@Composable
fun DailyQuotesApp(
    viewModel: QuoteViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentScreen by rememberSaveable { mutableStateOf(ScreenState.HOME) }

    // Intercept Android back gesture/button when on QuoteScreen to return to HomeScreen
    BackHandler(enabled = currentScreen == ScreenState.QUOTE) {
        currentScreen = ScreenState.HOME
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == ScreenState.QUOTE) {
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "AppScreenTransition",
        modifier = modifier
    ) { screen ->
        when (screen) {
            ScreenState.HOME -> {
                HomeScreen(
                    onGetInspiredClick = {
                        viewModel.loadInitialQuoteIfNeeded()
                        currentScreen = ScreenState.QUOTE
                    }
                )
            }
            ScreenState.QUOTE -> {
                QuoteScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        currentScreen = ScreenState.HOME
                    }
                )
            }
        }
    }
}
