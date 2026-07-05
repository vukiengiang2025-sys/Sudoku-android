package com.example

import com.example.domain.algorithms.BacktrackingSolver
import com.example.domain.algorithms.ConstraintPropagationSolver
import com.example.domain.algorithms.DancingLinksSolver
import com.example.domain.algorithms.HybridSolver
import com.example.domain.algorithms.MRVSolver
import com.example.domain.models.CellStatus
import com.example.presentation.sudoku.SolverSpeed
import com.example.presentation.sudoku.SudokuViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SudokuViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SudokuViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SudokuViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is empty`() {
        val state = viewModel.uiState.value
        assertEquals(5, state.availableAlgorithms.size)
        assertTrue(state.availableAlgorithms[0] is BacktrackingSolver)
        assertTrue(state.availableAlgorithms[1] is MRVSolver)
        assertTrue(state.availableAlgorithms[2] is ConstraintPropagationSolver)
        assertTrue(state.availableAlgorithms[3] is DancingLinksSolver)
        assertTrue(state.availableAlgorithms[4] is HybridSolver)
        
        // Check grid is empty
        state.board.grid.forEach { row ->
            row.forEach { cell ->
                assertNull(cell.value)
            }
        }
    }

    @Test
    fun `test load sample updates board`() {
        viewModel.loadSample()
        val state = viewModel.uiState.value
        assertNotNull(state.board.grid[0][0].value)
        assertEquals(5, state.board.grid[0][0].value)
        assertEquals(CellStatus.INITIAL, state.board.grid[0][0].status)
    }

    @Test
    fun `test valid input updates board`() {
        viewModel.selectCell(0, 0)
        viewModel.inputNumber(5)
        
        val state = viewModel.uiState.value
        assertEquals(5, state.board.grid[0][0].value)
        assertEquals(CellStatus.USER_INPUT, state.board.grid[0][0].status)
    }

    @Test
    fun `test invalid input marks cell as conflict`() {
        viewModel.selectCell(0, 0)
        viewModel.inputNumber(5)
        
        viewModel.selectCell(0, 1)
        viewModel.inputNumber(5) // Conflict in row
        
        val state = viewModel.uiState.value
        assertEquals(CellStatus.INVALID_USER_INPUT, state.board.grid[0][1].status)
    }
}
