package com.riya.dailyquotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.riya.dailyquotes.ui.screens.DailyQuotesApp
import com.riya.dailyquotes.ui.theme.DailyQuotesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DailyQuotesTheme {
                DailyQuotesApp()
            }
        }
    }
}