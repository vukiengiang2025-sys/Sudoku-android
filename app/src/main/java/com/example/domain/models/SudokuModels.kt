package com.example.domain.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

enum class CellStatus {
    INITIAL,
    USER_INPUT,
    SOLVER_ACTIVE,
    SOLVER_VALID,
    SOLVER_BACKTRACK,
    INVALID_USER_INPUT
}

@Immutable
data class CellState(
    val row: Int,
    val col: Int,
    val value: Int?,
    val status: CellStatus = CellStatus.USER_INPUT
)

@Immutable
data class SudokuBoard(
    val grid: List<List<CellState>> = List(9) { row ->
        List(9) { col ->
            CellState(row, col, null, CellStatus.USER_INPUT)
        }
    }
) {
    fun setCell(row: Int, col: Int, value: Int?, status: CellStatus = CellStatus.USER_INPUT): SudokuBoard {
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        newGrid[row][col] = CellState(row, col, value, status)
        return copy(grid = newGrid) // New instance of List ensures compose detects change deeply
    }

    fun copyGrid(
        transform: (CellState) -> CellState = { it }
    ): SudokuBoard {
        val newGrid = grid.map { row ->
            row.map { transform(it) }
        }
        return copy(grid = newGrid)
    }
}

@Stable
data class SolverStats(
    val steps: Int = 0,
    val timeMs: Long = 0,
    val backtracks: Int = 0,
    val progress: Float = 0f
)
