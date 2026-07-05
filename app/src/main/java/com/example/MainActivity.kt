package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.presentation.benchmark.BenchmarkScreen
import com.example.presentation.benchmark.BenchmarkViewModel
import com.example.presentation.sudoku.SudokuScreen
import com.example.presentation.sudoku.SudokuViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "sudoku") {
          composable("sudoku") {
            val viewModel: SudokuViewModel = viewModel()
            SudokuScreen(
              viewModel = viewModel,
              onNavigateToBenchmark = { navController.navigate("benchmark") }
            )
          }
          composable("benchmark") {
            val benchmarkViewModel: BenchmarkViewModel = viewModel()
            BenchmarkScreen(
              viewModel = benchmarkViewModel,
              onNavigateBack = { navController.popBackStack() }
            )
          }
        }
      }
    }
  }
}
