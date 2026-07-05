package com.example.domain.algorithms

import com.example.domain.models.CellStatus
import com.example.domain.models.SolverStats
import com.example.domain.models.SudokuBoard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MRVSolver : SudokuAlgorithm {
    override val name: String = "Minimum Remaining Values"

    override fun solve(initialBoard: SudokuBoard, speedMs: Long): Flow<SolverStep> = flow {
        val boardArr = Array(9) { r -> IntArray(9) { c -> initialBoard.grid[r][c].value ?: 0 } }
        val startTime = System.currentTimeMillis()
        var steps = 0
        var backtracks = 0

        var currentBoard = initialBoard.copyGrid { cell ->
            if (cell.value != null) cell.copy(status = CellStatus.INITIAL)
            else cell.copy(status = CellStatus.USER_INPUT)
        }

        suspend fun emitStep(r: Int, c: Int, status: CellStatus) {
            if (currentBoard.grid[r][c].status != CellStatus.INITIAL) {
                currentBoard = currentBoard.setCell(r, c, boardArr[r][c].takeIf { it != 0 }, status)
            }
            steps++
            val time = System.currentTimeMillis() - startTime
            emit(SolverStep(currentBoard, SolverStats(steps, time, backtracks, 0f)))
            if (speedMs > 0) delay(speedMs)
        }

        fun getValidValues(row: Int, col: Int): List<Int> {
            val valid = BooleanArray(10) { true }
            for (i in 0 until 9) {
                valid[boardArr[row][i]] = false
                valid[boardArr[i][col]] = false
                valid[boardArr[3 * (row / 3) + i / 3][3 * (col / 3) + i % 3]] = false
            }
            return (1..9).filter { valid[it] }
        }

        fun findBestEmptyCell(): Pair<Int, Int>? {
            var minOptions = 10
            var bestRow = -1
            var bestCol = -1
            for (r in 0 until 9) {
                for (c in 0 until 9) {
                    if (boardArr[r][c] == 0) {
                        val optionsCount = getValidValues(r, c).size
                        if (optionsCount < minOptions) {
                            minOptions = optionsCount
                            bestRow = r
                            bestCol = c
                            if (minOptions <= 1) return Pair(bestRow, bestCol)
                        }
                    }
                }
            }
            if (bestRow == -1) return null
            return Pair(bestRow, bestCol)
        }

        suspend fun solveRecursively(): Boolean {
            val cell = findBestEmptyCell()
            if (cell == null) return true // Solved

            val (row, col) = cell
            val options = getValidValues(row, col)
            
            for (num in options) {
                boardArr[row][col] = num
                emitStep(row, col, CellStatus.SOLVER_ACTIVE)
                
                if (solveRecursively()) {
                    emitStep(row, col, CellStatus.SOLVER_VALID)
                    return true
                } else {
                    boardArr[row][col] = 0
                    backtracks++
                    emitStep(row, col, CellStatus.SOLVER_BACKTRACK)
                }
            }
            return false
        }

        val success = solveRecursively()
        val finalTime = System.currentTimeMillis() - startTime
        
        if (success) {
            currentBoard = currentBoard.copyGrid { cell ->
                if (cell.status == CellStatus.SOLVER_VALID || cell.status == CellStatus.SOLVER_ACTIVE) {
                    cell.copy(status = CellStatus.SOLVER_VALID)
                } else cell
            }
        }
        
        emit(SolverStep(currentBoard, SolverStats(steps, finalTime, backtracks, 1f), true, success))
    }
}
