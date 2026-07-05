package com.example.presentation.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.algorithms.BacktrackingSolver
import com.example.domain.algorithms.ConstraintPropagationSolver
import com.example.domain.algorithms.DancingLinksSolver
import com.example.domain.algorithms.HybridSolver
import com.example.domain.algorithms.MRVSolver
import com.example.domain.algorithms.SudokuAlgorithm
import com.example.domain.models.CellStatus
import com.example.domain.models.SolverStats
import com.example.domain.models.SudokuBoard
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

enum class SolverSpeed(val delayMs: Long) {
    FAST(1L),
    MEDIUM(20L),
    SLOW(150L)
}

data class SudokuUiState(
    val board: SudokuBoard = SudokuBoard(),
    val selectedCell: Pair<Int, Int>? = null,
    val isSolving: Boolean = false,
    val isPaused: Boolean = false,
    val stats: SolverStats = SolverStats(),
    val currentAlgorithm: SudokuAlgorithm = BacktrackingSolver(),
    val availableAlgorithms: List<SudokuAlgorithm> = listOf(
        BacktrackingSolver(), 
        MRVSolver(),
        ConstraintPropagationSolver(),
        DancingLinksSolver(),
        HybridSolver()
    ),
    val currentSpeed: SolverSpeed = SolverSpeed.FAST,
    val isSolved: Boolean = false,
    val noSolution: Boolean = false
)

class SudokuViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private var solverJob: Job? = null
    
    fun selectCell(row: Int, col: Int) {
        if (_uiState.value.isSolving) return
        _uiState.update { it.copy(selectedCell = Pair(row, col)) }
    }

    fun inputNumber(number: Int?) {
        val state = _uiState.value
        if (state.isSolving || state.selectedCell == null) return
        
        val (row, col) = state.selectedCell
        val currentBoard = state.board
        
        // Validation check (if it causes a conflict, we could mark it INVALID_USER_INPUT, but for now simple input is fine)
        val newBoard = currentBoard.setCell(row, col, number, CellStatus.USER_INPUT)
        
        _uiState.update { 
            it.copy(
                board = validateBoard(newBoard),
                isSolved = false,
                noSolution = false,
                stats = SolverStats() // reset stats
            )
        }
    }

    private fun validateBoard(board: SudokuBoard): SudokuBoard {
        return board.copyGrid { cell ->
            if (cell.value != null && !isValid(board.grid, cell.row, cell.col, cell.value)) {
                cell.copy(status = CellStatus.INVALID_USER_INPUT)
            } else if (cell.status == CellStatus.INVALID_USER_INPUT) {
                 cell.copy(status = CellStatus.USER_INPUT)
            } else {
                cell
            }
        }
    }
    
    private fun isValid(grid: List<List<com.example.domain.models.CellState>>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until 9) {
            if (i != col && grid[row][i].value == num) return false
            if (i != row && grid[i][col].value == num) return false
            val r = 3 * (row / 3) + i / 3
            val c = 3 * (col / 3) + i % 3
            if ((r != row || c != col) && grid[r][c].value == num) return false
        }
        return true
    }

    fun clearBoard() {
        if (_uiState.value.isSolving) return
        solverJob?.cancel()
        _uiState.update { 
            it.copy(
                board = SudokuBoard(), 
                stats = SolverStats(),
                isSolved = false,
                noSolution = false,
                selectedCell = null
            ) 
        }
    }

    fun loadSample() {
        if (_uiState.value.isSolving) return
        val sample = arrayOf(
            intArrayOf(5, 3, 0, 0, 7, 0, 0, 0, 0),
            intArrayOf(6, 0, 0, 1, 9, 5, 0, 0, 0),
            intArrayOf(0, 9, 8, 0, 0, 0, 0, 6, 0),
            intArrayOf(8, 0, 0, 0, 6, 0, 0, 0, 3),
            intArrayOf(4, 0, 0, 8, 0, 3, 0, 0, 1),
            intArrayOf(7, 0, 0, 0, 2, 0, 0, 0, 6),
            intArrayOf(0, 6, 0, 0, 0, 0, 2, 8, 0),
            intArrayOf(0, 0, 0, 4, 1, 9, 0, 0, 5),
            intArrayOf(0, 0, 0, 0, 8, 0, 0, 7, 9)
        )
        val newBoard = SudokuBoard().copyGrid { cell ->
            val v = sample[cell.row][cell.col]
            if (v != 0) cell.copy(value = v, status = CellStatus.INITIAL)
            else cell.copy(value = null, status = CellStatus.USER_INPUT)
        }
        _uiState.update { 
            it.copy(
                board = newBoard, 
                stats = SolverStats(), 
                isSolved = false, 
                noSolution = false 
            ) 
        }
    }

    fun solve() {
        val state = _uiState.value
        if (state.isSolving) return
        
        _uiState.update { it.copy(isSolving = true, isPaused = false, noSolution = false) }
        
        solverJob?.cancel()
        solverJob = viewModelScope.launch {
            val algorithm = state.currentAlgorithm
            algorithm.solve(state.board, state.currentSpeed.delayMs).collect { step ->
                _uiState.update { 
                    it.copy(
                        board = step.board,
                        stats = step.stats,
                        isSolving = !step.isFinished,
                        isSolved = step.isFinished && step.isSuccess,
                        noSolution = step.isFinished && !step.isSuccess
                    )
                }
            }
        }
    }
    
    fun stopSolving() {
        solverJob?.cancel()
        _uiState.update { it.copy(isSolving = false, isPaused = false) }
    }

    fun setAlgorithm(algorithm: SudokuAlgorithm) {
        if (_uiState.value.isSolving) return
        _uiState.update { it.copy(currentAlgorithm = algorithm) }
    }

    fun setSpeed(speed: SolverSpeed) {
        _uiState.update { it.copy(currentSpeed = speed) }
    }
}
