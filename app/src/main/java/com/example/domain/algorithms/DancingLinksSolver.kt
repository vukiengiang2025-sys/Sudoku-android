package com.example.domain.algorithms

import com.example.domain.models.CellStatus
import com.example.domain.models.SolverStats
import com.example.domain.models.SudokuBoard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

open class DLXNode {
    var left: DLXNode = this
    var right: DLXNode = this
    var up: DLXNode = this
    var down: DLXNode = this
    var col: ColumnNode? = null
    var rowInfo: IntArray? = null // [row, col, num]
}

class ColumnNode(val id: Int) : DLXNode() {
    var size: Int = 0
    init { col = this }
}

class DancingLinksSolver : SudokuAlgorithm {
    override val name: String = "Dancing Links (Algorithm X)"

    override fun solve(initialBoard: SudokuBoard, speedMs: Long): Flow<SolverStep> = flow {
        val startTime = System.currentTimeMillis()
        var steps = 0
        var backtracks = 0

        val grid = Array(9) { r -> IntArray(9) { c -> initialBoard.grid[r][c].value ?: 0 } }
        var currentBoard = initialBoard.copyGrid { cell ->
            if (cell.value != null) cell.copy(status = CellStatus.INITIAL)
            else cell.copy(status = CellStatus.USER_INPUT)
        }

        suspend fun emitStep(r: Int, c: Int, status: CellStatus, isBacktrack: Boolean = false) {
            if (currentBoard.grid[r][c].status != CellStatus.INITIAL) {
                currentBoard = currentBoard.setCell(r, c, grid[r][c].takeIf { it != 0 }, status)
            }
            if (isBacktrack) backtracks++
            steps++
            val time = System.currentTimeMillis() - startTime
            emit(SolverStep(currentBoard, SolverStats(steps, time, backtracks, 0f)))
            if (speedMs > 0) delay(speedMs)
        }

        val root = ColumnNode(-1)
        val cols = Array(324) { ColumnNode(it) }
        
        // Link columns
        for (i in 0 until 324) {
            cols[i].right = if (i == 323) root else cols[i + 1]
            cols[i].left = if (i == 0) root else cols[i - 1]
        }
        root.right = cols[0]
        root.left = cols[323]

        // Create rows
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val value = grid[r][c]
                val nums = if (value == 0) 1..9 else value..value
                
                for (num in nums) {
                    val boxIdx = (r / 3) * 3 + c / 3
                    val c1 = r * 9 + c
                    val c2 = 81 + r * 9 + (num - 1)
                    val c3 = 162 + c * 9 + (num - 1)
                    val c4 = 243 + boxIdx * 9 + (num - 1)
                    
                    val rowIndices = intArrayOf(c1, c2, c3, c4)
                    var prevNode: DLXNode? = null
                    var firstNode: DLXNode? = null
                    
                    for (colIdx in rowIndices) {
                        val colNode = cols[colIdx]
                        val node = DLXNode()
                        node.rowInfo = intArrayOf(r, c, num)
                        node.col = colNode
                        
                        node.down = colNode
                        node.up = colNode.up
                        colNode.up.down = node
                        colNode.up = node
                        
                        if (prevNode != null) {
                            node.left = prevNode
                            node.right = firstNode!!
                            prevNode.right = node
                            firstNode.left = node
                        } else {
                            firstNode = node
                        }
                        prevNode = node
                        colNode.size++
                    }
                }
            }
        }

        fun cover(c: ColumnNode) {
            c.right.left = c.left
            c.left.right = c.right
            var i = c.down
            while (i != c) {
                var j = i.right
                while (j != i) {
                    j.down.up = j.up
                    j.up.down = j.down
                    j.col!!.size--
                    j = j.right
                }
                i = i.down
            }
        }

        fun uncover(c: ColumnNode) {
            var i = c.up
            while (i != c) {
                var j = i.left
                while (j != i) {
                    j.col!!.size++
                    j.down.up = j
                    j.up.down = j
                    j = j.left
                }
                i = i.up
            }
            c.right.left = c
            c.left.right = c
        }

        var isSolved = false

        suspend fun search() {
            if (root.right == root) {
                isSolved = true
                return
            }

            var c = root.right as ColumnNode
            var minSize = c.size
            var j = c.right
            while (j != root) {
                val col = j as ColumnNode
                if (col.size < minSize) {
                    minSize = col.size
                    c = col
                }
                j = j.right
            }

            if (minSize == 0) return

            cover(c)
            var rNode = c.down
            while (rNode != c) {
                val info = rNode.rowInfo!!
                val (rIdx, cIdx, num) = info
                
                val isGiven = initialBoard.grid[rIdx][cIdx].status == CellStatus.INITIAL
                if (!isGiven) {
                    grid[rIdx][cIdx] = num
                    emitStep(rIdx, cIdx, CellStatus.SOLVER_ACTIVE)
                }

                var jNode = rNode.right
                while (jNode != rNode) {
                    cover(jNode.col!!)
                    jNode = jNode.right
                }

                search()
                if (isSolved) return

                jNode = rNode.left
                while (jNode != rNode) {
                    uncover(jNode.col!!)
                    jNode = jNode.left
                }

                if (!isGiven) {
                    grid[rIdx][cIdx] = 0
                    emitStep(rIdx, cIdx, CellStatus.SOLVER_BACKTRACK, true)
                }

                rNode = rNode.down
            }
            uncover(c)
        }

        search()
        
        val finalTime = System.currentTimeMillis() - startTime
        
        if (isSolved) {
            currentBoard = currentBoard.copyGrid { cell ->
                if (cell.status == CellStatus.SOLVER_VALID || cell.status == CellStatus.SOLVER_ACTIVE) {
                    cell.copy(status = CellStatus.SOLVER_VALID)
                } else cell
            }
        }

        emit(SolverStep(currentBoard, SolverStats(steps, finalTime, backtracks, 1f), true, isSolved))
    }
}
