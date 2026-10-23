package com.riya.dailyquotes.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riya.dailyquotes.ui.QuoteViewModel

/**
 * Top-level screen composable forwarding to [DailyQuotesApp].
 * Preserves compatibility with existing invocations.
 */
@Composable
fun DailyQuotesScreen(
    viewModel: QuoteViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    DailyQuotesApp(viewModel = viewModel, modifier = modifier)
}
