package com.example.domain.utils

object SudokuSolverUtil {
    fun solve(board: Array<IntArray>): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) {
                    for (num in 1..9) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num
                            if (solve(board)) {
                                return true
                            }
                            board[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until 9) {
            if (board[row][i] == num) return false
            if (board[i][col] == num) return false
            if (board[3 * (row / 3) + i / 3][3 * (col / 3) + i % 3] == num) return false
        }
        return true
    }
}
