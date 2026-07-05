package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.ClientRequestRepository
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(this)
        val repository = ClientRequestRepository(database.clientRequestDao())
        val sharedPrefs = getSharedPreferences("auriga_form_drafts", android.content.Context.MODE_PRIVATE)
        
        // 2. Initialize ViewModel with Factory
        val factory = MainViewModelFactory(repository, sharedPrefs)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

