package com.example

import com.example.domain.algorithms.BacktrackingSolver
import com.example.domain.algorithms.ConstraintPropagationSolver
import com.example.domain.algorithms.DancingLinksSolver
import com.example.domain.algorithms.HybridSolver
import com.example.domain.algorithms.MRVSolver
import com.example.domain.models.CellStatus
import com.example.domain.models.SudokuBoard
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.system.measureTimeMillis

class AlgorithmBenchmarkTest {

    private fun getHardBoard(): SudokuBoard {
        val puzzle = arrayOf(
            intArrayOf(1, 0, 0, 0, 0, 7, 0, 9, 0),
            intArrayOf(0, 3, 0, 0, 2, 0, 0, 0, 8),
            intArrayOf(0, 0, 9, 6, 0, 0, 5, 0, 0),
            intArrayOf(0, 0, 5, 3, 0, 0, 9, 0, 0),
            intArrayOf(0, 1, 0, 0, 8, 0, 0, 0, 2),
            intArrayOf(6, 0, 0, 0, 0, 4, 0, 0, 0),
            intArrayOf(3, 0, 0, 0, 0, 0, 0, 1, 0),
            intArrayOf(0, 4, 0, 0, 0, 0, 0, 0, 7),
            intArrayOf(0, 0, 7, 0, 0, 0, 3, 0, 0)
        )
        return SudokuBoard().copyGrid { cell ->
            val v = puzzle[cell.row][cell.col]
            if (v != 0) cell.copy(value = v, status = CellStatus.INITIAL)
            else cell
        }
    }

    @Test
    fun benchmarkAlgorithms() = runBlocking {
        val board = getHardBoard()
        
        val algorithms = listOf(
            BacktrackingSolver(),
            MRVSolver(),
            ConstraintPropagationSolver(),
            DancingLinksSolver(),
            HybridSolver()
        )
        
        println("=== SUDOKU ALGORITHM BENCHMARK (Hard Puzzle) ===")
        
        for (algo in algorithms) {
            val time = measureTimeMillis {
                val result = algo.solve(board, 0L).last()
                println("${algo.name}:")
                println("  Solved: ${result.isSuccess}")
                println("  Steps (Nodes visited): ${result.stats.steps}")
                println("  Backtracks: ${result.stats.backtracks}")
            }
            println("  Execution Time: ${time}ms")
            println("----------------------------------------------")
        }
    }
}
