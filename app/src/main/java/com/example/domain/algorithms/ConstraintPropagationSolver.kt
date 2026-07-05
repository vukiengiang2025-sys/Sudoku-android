package com.example.domain.algorithms

import com.example.domain.models.CellStatus
import com.example.domain.models.SolverStats
import com.example.domain.models.SudokuBoard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ConstraintPropagationSolver : SudokuAlgorithm {
    override val name: String = "Constraint Propagation"

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
        
        suspend fun propagate(): Boolean {
            var changed = true
            while (changed) {
                changed = false
                for (r in 0 until 9) {
                    for (c in 0 until 9) {
                        if (boardArr[r][c] == 0) {
                            val options = getValidValues(r, c)
                            if (options.isEmpty()) return false // Conflict
                            if (options.size == 1) {
                                boardArr[r][c] = options[0]
                                changed = true
                                emitStep(r, c, CellStatus.SOLVER_ACTIVE)
                            }
                        }
                    }
                }
            }
            return true
        }

        fun findEmptyCell(): Pair<Int, Int>? {
            var minOptions = 10
            var bestRow = -1
            var bestCol = -1
            for (r in 0 until 9) {
                for (c in 0 until 9) {
                    if (boardArr[r][c] == 0) {
                        val options = getValidValues(r, c).size
                        if (options < minOptions) {
                            minOptions = options
                            bestRow = r
                            bestCol = c
                            if (minOptions <= 1) return Pair(bestRow, bestCol)
                        }
                    }
                }
            }
            return if (bestRow != -1) Pair(bestRow, bestCol) else null
        }

        suspend fun solveRecursively(): Boolean {
            val cell = findEmptyCell()
            if (cell == null) return true // Solved

            val (row, col) = cell
            val options = getValidValues(row, col)

            for (num in options) {
                boardArr[row][col] = num
                emitStep(row, col, CellStatus.SOLVER_ACTIVE)

                // Clone state before propagation
                val stateSnapshot = Array(9) { r -> IntArray(9) { c -> boardArr[r][c] } }

                if (propagate() && solveRecursively()) {
                    emitStep(row, col, CellStatus.SOLVER_VALID)
                    return true
                } else {
                    // Backtrack to snapshot
                    for (r in 0 until 9) {
                        for (c in 0 until 9) {
                            if (boardArr[r][c] != stateSnapshot[r][c]) {
                                boardArr[r][c] = stateSnapshot[r][c]
                                emitStep(r, c, CellStatus.USER_INPUT)
                            }
                        }
                    }
                    boardArr[row][col] = 0
                    backtracks++
                    emitStep(row, col, CellStatus.SOLVER_BACKTRACK)
                }
            }
            return false
        }

        val success = propagate() && solveRecursively()
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
