package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.WorkoutApp
import com.example.ui.WorkoutViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: WorkoutViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      WorkoutApp(viewModel)
    }
  }
}
