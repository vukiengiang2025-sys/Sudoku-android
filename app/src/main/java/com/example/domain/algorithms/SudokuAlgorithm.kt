package com.example.domain.algorithms

import com.example.domain.models.SudokuBoard
import com.example.domain.models.SolverStats
import kotlinx.coroutines.flow.Flow

data class SolverStep(
    val board: SudokuBoard,
    val stats: SolverStats,
    val isFinished: Boolean = false,
    val isSuccess: Boolean = false
)

interface SudokuAlgorithm {
    val name: String
    fun solve(initialBoard: SudokuBoard, speedMs: Long): Flow<SolverStep>
}
