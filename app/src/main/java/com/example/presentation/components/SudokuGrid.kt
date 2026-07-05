package com.example.presentation.components
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.alpha

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.CellState
import com.example.domain.models.CellStatus
import kotlinx.coroutines.delay

@Composable
fun SudokuGrid(
    grid: List<List<CellState>>,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    isSolving: Boolean,
    modifier: Modifier = Modifier,
    onNumberInput: (Int?) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    
    // Hidden text field focus and state
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var textValue by remember { mutableStateOf("") }
    
    // Request focus whenever a cell is selected and not solving
    LaunchedEffect(selectedCell, isSolving) {
        if (selectedCell != null && !isSolving) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failure
            }
        }
    }

    // "Scanner" effect variables
    var scannerY by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isSolving) {
        if (isSolving) {
            while (true) {
                scannerY = 0f
                while (scannerY < 1f) {
                    delay(16)
                    scannerY += 0.015f
                }
            }
        } else {
            scannerY = -1f
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(16.dp)
            }
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        // Hidden text field for capturing keyboard input
        androidx.compose.foundation.text.BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                if (newValue.isNotEmpty()) {
                    val lastChar = newValue.last()
                    if (lastChar in '1'..'9') {
                        onNumberInput(lastChar.toString().toInt())
                    } else if (lastChar == '0' || lastChar.isWhitespace()) {
                        onNumberInput(null)
                    }
                    textValue = "" // Reset after processing
                }
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            for (blockRow in 0 until 3) {
                Row(modifier = Modifier.weight(1f)) {
                    for (blockCol in 0 until 3) {
                        SudokuBlock(
                            grid = grid,
                            blockRow = blockRow,
                            blockCol = blockCol,
                            selectedCell = selectedCell,
                            onCellClick = { r, c ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCellClick(r, c)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(1.5.dp)
                        )
                    }
                }
            }
        }
        
        // Scanner effect overlay
        if (isSolving && scannerY >= 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val yPos = scannerY * size.height
                drawLine(
                    color = Color.Cyan.copy(alpha = 0.5f),
                    start = Offset(0f, yPos),
                    end = Offset(size.width, yPos),
                    strokeWidth = 4.dp.toPx()
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Cyan.copy(alpha = 0.2f), Color.Transparent),
                        startY = yPos - 20.dp.toPx(),
                        endY = yPos + 20.dp.toPx()
                    ),
                    topLeft = Offset(0f, yPos - 20.dp.toPx()),
                    size = Size(size.width, 40.dp.toPx())
                )
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
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
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
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.status) {
        if (state.status == CellStatus.INVALID_USER_INPUT || state.status == CellStatus.SOLVER_BACKTRACK) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val targetBackgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        state.status == CellStatus.SOLVER_ACTIVE -> Color(0xFFFFD54F) // Yellow/Gold
        state.status == CellStatus.SOLVER_VALID -> Color(0xFF4CAF50).copy(alpha = 0.5f) // Greenish
        state.status == CellStatus.SOLVER_BACKTRACK -> Color(0xFFE53935).copy(alpha = 0.5f) // Reddish
        state.status == CellStatus.INITIAL -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 250),
        label = "cellColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else if (state.status == CellStatus.SOLVER_ACTIVE) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cellScale"
    )

    val textColor = when (state.status) {
        CellStatus.INITIAL -> MaterialTheme.colorScheme.onSurface
        CellStatus.USER_INPUT -> MaterialTheme.colorScheme.primary
        CellStatus.INVALID_USER_INPUT -> MaterialTheme.colorScheme.error
        CellStatus.SOLVER_VALID -> Color.White
        CellStatus.SOLVER_BACKTRACK -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val animatedTextColor by animateColorAsState(targetValue = textColor, label = "textColor")

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(
                width = if (isSelected || state.status == CellStatus.INVALID_USER_INPUT) 2.dp else 0.dp,
                color = when {
                    state.status == CellStatus.INVALID_USER_INPUT -> MaterialTheme.colorScheme.error
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(4.dp)
            )
            .let {
                if (isSelected) {
                    it.shadow(8.dp, RoundedCornerShape(4.dp), ambientColor = MaterialTheme.colorScheme.primary)
                } else it
            }
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (state.value != null) {
            Text(
                text = state.value.toString(),
                color = animatedTextColor,
                fontSize = 22.sp,
                fontWeight = if (state.status == CellStatus.INITIAL) FontWeight.ExtraBold else FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    this.scaleX = if (state.value != null) 1f else 0f
                    this.scaleY = if (state.value != null) 1f else 0f
                }
            )
        }
    }
}
