package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.CellState
import com.example.domain.models.CellStatus

@Composable
fun SudokuGrid(
    grid: List<List<CellState>>,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .padding(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (blockRow in 0 until 3) {
                Row(modifier = Modifier.weight(1f)) {
                    for (blockCol in 0 until 3) {
                        SudokuBlock(
                            grid = grid,
                            blockRow = blockRow,
                            blockCol = blockCol,
                            selectedCell = selectedCell,
                            onCellClick = onCellClick,
                            modifier = Modifier
                                .weight(1f)
                                .padding(1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SudokuBlock(
    grid: List<List<CellState>>,
    blockRow: Int,
    blockCol: Int,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until 3) {
                Row(modifier = Modifier.weight(1f)) {
                    for (c in 0 until 3) {
                        val row = blockRow * 3 + r
                        val col = blockCol * 3 + c
                        val cellState = grid[row][col]
                        val isSelected = selectedCell?.first == row && selectedCell?.second == col
                        
                        SudokuCell(
                            state = cellState,
                            isSelected = isSelected,
                            onClick = { onCellClick(row, col) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(0.5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SudokuCell(
    state: CellState,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBackgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        state.status == CellStatus.SOLVER_ACTIVE -> Color(0xFFFFD54F) // Yellow
        state.status == CellStatus.SOLVER_VALID -> Color(0xFF81C784) // Green
        state.status == CellStatus.SOLVER_BACKTRACK -> Color(0xFFE57373) // Red
        else -> MaterialTheme.colorScheme.surface
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 200),
        label = "cellColor"
    )

    val textColor = when (state.status) {
        CellStatus.INITIAL -> MaterialTheme.colorScheme.onSurface
        CellStatus.USER_INPUT -> MaterialTheme.colorScheme.primary
        CellStatus.INVALID_USER_INPUT -> MaterialTheme.colorScheme.error
        else -> Color.Black
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .border(
                width = if (state.status == CellStatus.INVALID_USER_INPUT) 2.dp else 0.5.dp,
                color = if (state.status == CellStatus.INVALID_USER_INPUT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (state.value != null) {
            Text(
                text = state.value.toString(),
                color = textColor,
                fontSize = 20.sp,
                fontWeight = if (state.status == CellStatus.INITIAL) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
