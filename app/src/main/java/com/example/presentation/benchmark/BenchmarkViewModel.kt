package com.example.presentation.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.algorithms.*
import com.example.domain.utils.Difficulty
import com.example.domain.utils.SudokuGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BenchmarkRecord(
    val algorithm: String,
    val difficulty: Difficulty,
    val timeMs: Long,
    val steps: Int,
    val backtracks: Int,
    val ramKb: Long,
    val success: Boolean
)

data class BenchmarkSummary(
    val algorithm: String,
    val avgTimeMs: Double,
    val avgSteps: Double,
    val avgBacktracks: Double,
    val avgRamKb: Double,
    val successRate: Double,
    val totalRuns: Int,
    val color: androidx.compose.ui.graphics.Color
)

data class BenchmarkUiState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val currentTask: String = "Ready to start",
    val totalPuzzles: Int = 1000,
    val completedPuzzles: Int = 0,
    val summaries: List<BenchmarkSummary> = emptyList()
)

class BenchmarkViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    private var benchmarkJob: Job? = null

    private val colors = listOf(
        androidx.compose.ui.graphics.Color(0xFFE91E63),
        androidx.compose.ui.graphics.Color(0xFF2196F3),
        androidx.compose.ui.graphics.Color(0xFF4CAF50),
        androidx.compose.ui.graphics.Color(0xFFFF9800),
        androidx.compose.ui.graphics.Color(0xFF9C27B0)
    )

    fun startBenchmark() {
        if (_uiState.value.isRunning) return

        benchmarkJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isRunning = true, progress = 0f, completedPuzzles = 0, summaries = emptyList(), currentTask = "Generating puzzles...") }

            val totalPuzzles = 1000
            val puzzlesPerDiff = totalPuzzles / Difficulty.entries.size

            val puzzles = mutableListOf<Pair<Difficulty, com.example.domain.models.SudokuBoard>>()
            for (diff in Difficulty.entries) {
                for (i in 0 until puzzlesPerDiff) {
                    puzzles.add(Pair(diff, SudokuGenerator.generate(diff)))
                }
            }

            val algorithms = listOf(
                BacktrackingSolver(),
                MRVSolver(),
                ConstraintPropagationSolver(),
                DancingLinksSolver()
            )

            val records = mutableListOf<BenchmarkRecord>()
            var completed = 0
            val totalTasks = puzzles.size * algorithms.size

            for ((idx, algo) in algorithms.withIndex()) {
                _uiState.update { it.copy(currentTask = "Evaluating ${algo.name}...") }
                for ((diff, board) in puzzles) {
                    // Try to trigger GC occasionally, but not every step to avoid overhead
                    if (completed % 50 == 0) System.gc()

                    val startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

                    val result = algo.solve(board, 0L).last()

                    val endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                    val usedMem = maxOf(0L, (endMem - startMem) / 1024)

                    records.add(
                        BenchmarkRecord(
                            algorithm = algo.name,
                            difficulty = diff,
                            timeMs = result.stats.timeMs,
                            steps = result.stats.steps,
                            backtracks = result.stats.backtracks,
                            ramKb = usedMem,
                            success = result.isSuccess
                        )
                    )

                    completed++
                    if (completed % 20 == 0) {
                        _uiState.update {
                            it.copy(
                                progress = completed.toFloat() / totalTasks,
                                completedPuzzles = completed / algorithms.size
                            )
                        }
                    }
                }
            }

            val summaries = algorithms.mapIndexed { idx, algo ->
                val algoRecords = records.filter { it.algorithm == algo.name }
                BenchmarkSummary(
                    algorithm = algo.name,
                    avgTimeMs = algoRecords.map { it.timeMs }.average(),
                    avgSteps = algoRecords.map { it.steps }.average(),
                    avgBacktracks = algoRecords.map { it.backtracks }.average(),
                    avgRamKb = algoRecords.map { it.ramKb }.average(),
                    successRate = algoRecords.count { it.success }.toDouble() / algoRecords.size * 100,
                    totalRuns = algoRecords.size,
                    color = colors[idx % colors.size]
                )
            }.sortedBy { it.avgTimeMs }

            _uiState.update {
                it.copy(
                    isRunning = false,
                    progress = 1f,
                    completedPuzzles = totalPuzzles,
                    currentTask = "Benchmark Complete",
                    summaries = summaries
                )
            }
        }
    }

    fun stopBenchmark() {
        benchmarkJob?.cancel()
        _uiState.update { it.copy(isRunning = false, currentTask = "Benchmark Stopped", progress = 0f) }
    }
}
