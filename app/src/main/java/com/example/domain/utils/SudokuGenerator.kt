package com.example.domain.utils

import com.example.domain.models.CellStatus
import com.example.domain.models.SudokuBoard
import kotlin.random.Random

enum class Difficulty(val cellsToRemove: Int) {
    EASY(30),
    MEDIUM(40),
    HARD(50),
    EXPERT(55),
    EVIL(60)
}

object SudokuGenerator {
    private val baseBoard = arrayOf(
        intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        intArrayOf(4, 5, 6, 7, 8, 9, 1, 2, 3),
        intArrayOf(7, 8, 9, 1, 2, 3, 4, 5, 6),
        intArrayOf(2, 3, 1, 5, 6, 4, 8, 9, 7),
        intArrayOf(5, 6, 4, 8, 9, 7, 2, 3, 1),
        intArrayOf(8, 9, 7, 2, 3, 1, 5, 6, 4),
        intArrayOf(3, 1, 2, 6, 4, 5, 9, 7, 8),
        intArrayOf(6, 4, 5, 9, 7, 8, 3, 1, 2),
        intArrayOf(9, 7, 8, 3, 1, 2, 6, 4, 5)
    )

    fun generate(difficulty: Difficulty): SudokuBoard {
        val board = Array(9) { r -> IntArray(9) { c -> baseBoard[r][c] } }
        shuffle(board)

        var removed = 0
        while (removed < difficulty.cellsToRemove) {
            val r = Random.nextInt(9)
            val c = Random.nextInt(9)
            if (board[r][c] != 0) {
                board[r][c] = 0
                removed++
            }
        }

        return SudokuBoard().copyGrid { cell ->
            val v = board[cell.row][cell.col]
            if (v != 0) cell.copy(value = v, status = CellStatus.INITIAL)
            else cell.copy(value = null, status = CellStatus.USER_INPUT)
        }
    }

    private fun shuffle(board: Array<IntArray>) {
        val nums = (1..9).toList().shuffled()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (board[r][c] > 0) {
                    board[r][c] = nums[board[r][c] - 1]
                }
            }
        }
        for (band in 0 until 3) {
            val rows = (0..2).toList().shuffled()
            val temp0 = board[band * 3 + 0].clone()
            val temp1 = board[band * 3 + 1].clone()
            val temp2 = board[band * 3 + 2].clone()
            board[band * 3 + 0] = if (rows[0] == 0) temp0 else if (rows[0] == 1) temp1 else temp2
            board[band * 3 + 1] = if (rows[1] == 0) temp0 else if (rows[1] == 1) temp1 else temp2
            board[band * 3 + 2] = if (rows[2] == 0) temp0 else if (rows[2] == 1) temp1 else temp2
        }
        for (band in 0 until 3) {
            val cols = (0..2).toList().shuffled()
            for (r in 0 until 9) {
                val temp0 = board[r][band * 3 + 0]
                val temp1 = board[r][band * 3 + 1]
                val temp2 = board[r][band * 3 + 2]
                board[r][band * 3 + 0] = if (cols[0] == 0) temp0 else if (cols[0] == 1) temp1 else temp2
                board[r][band * 3 + 1] = if (cols[1] == 0) temp0 else if (cols[1] == 1) temp1 else temp2
                board[r][band * 3 + 2] = if (cols[2] == 0) temp0 else if (cols[2] == 1) temp1 else temp2
            }
        }
    }
}
