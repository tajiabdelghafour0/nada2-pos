package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.GroceryRepository
import com.example.ui.GroceryViewModel
import com.example.ui.GroceryViewModelFactory
import com.example.ui.screens.GroceryApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = GroceryRepository(database)
        val viewModelFactory = GroceryViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[GroceryViewModel::class.java]

        setContent {
            MyApplicationTheme {
                GroceryApp(viewModel = viewModel)
            }
        }
    }
}

