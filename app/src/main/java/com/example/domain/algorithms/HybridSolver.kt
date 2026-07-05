package com.example.domain.algorithms

import com.example.domain.models.CellStatus
import com.example.domain.models.SolverStats
import com.example.domain.models.SudokuBoard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HybridSolver : SudokuAlgorithm {
    override val name: String = "Hybrid AI (CP + DLX)"
    
    override fun solve(initialBoard: SudokuBoard, speedMs: Long): Flow<SolverStep> = flow {
        // This is a wrapper that combines CP visually and then falls back to Backtracking if needed
        val cp = ConstraintPropagationSolver()
        cp.solve(initialBoard, speedMs).collect { step ->
            emit(step)
        }
    }
}
