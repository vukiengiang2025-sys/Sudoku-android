package com.example.domain.algorithms

import com.example.domain.models.CellStatus
import com.example.domain.models.SolverStats
import com.example.domain.models.SudokuBoard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BacktrackingSolver : SudokuAlgorithm {
    override val name: String = "Backtracking"

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

        fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
            for (i in 0 until 9) {
                if (board[row][i] == num) return false
                if (board[i][col] == num) return false
                if (board[3 * (row / 3) + i / 3][3 * (col / 3) + i % 3] == num) return false
            }
            return true
        }

        suspend fun solveRecursively(): Boolean {
            for (row in 0 until 9) {
                for (col in 0 until 9) {
                    if (boardArr[row][col] == 0) {
                        for (num in 1..9) {
                            if (isValid(boardArr, row, col, num)) {
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
                        }
                        return false
                    }
                }
            }
            return true
        }

        val success = solveRecursively()
        val finalTime = System.currentTimeMillis() - startTime
        
        // Mark all as valid if success
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
