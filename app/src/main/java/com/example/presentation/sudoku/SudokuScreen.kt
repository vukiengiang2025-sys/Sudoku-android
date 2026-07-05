package com.example.presentation.sudoku

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.components.NumberPad
import com.example.presentation.components.SudokuGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(
    viewModel: SudokuViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sudoku Solver AI", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { viewModel.clearBoard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear Board")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Card for Controls (Glassmorphism look)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Algorithm Selection
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = uiState.currentAlgorithm.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Thuật toán (Algorithm)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                uiState.availableAlgorithms.forEach { algo ->
                                    DropdownMenuItem(
                                        text = { Text(algo.name) },
                                        onClick = {
                                            viewModel.setAlgorithm(algo)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Speed Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tốc độ (Speed):", fontWeight = FontWeight.SemiBold)
                            Row {
                                SolverSpeed.entries.forEach { speed ->
                                    FilterChip(
                                        selected = uiState.currentSpeed == speed,
                                        onClick = { viewModel.setSpeed(speed) },
                                        label = { Text(speed.name) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                SudokuGrid(
                    grid = uiState.board.grid,
                    selectedCell = uiState.selectedCell,
                    onCellClick = viewModel::selectCell,
                    modifier = Modifier.fillMaxWidth(0.95f)
                )

                // Status and Stats
                AnimatedVisibility(visible = uiState.isSolving || uiState.isSolved || uiState.noSolution) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                uiState.isSolved -> Color(0xFFE8F5E9).copy(alpha = 0.8f)
                                uiState.noSolution -> Color(0xFFFFEBEE).copy(alpha = 0.8f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = when {
                                    uiState.isSolved -> "Đã giải xong! (Solved!)"
                                    uiState.noSolution -> "Không có nghiệm! (No Solution!)"
                                    else -> "Đang giải... (Solving...)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    uiState.isSolved -> Color(0xFF2E7D32)
                                    uiState.noSolution -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bước (Steps): ${uiState.stats.steps}")
                                Text("Thời gian (Time): ${uiState.stats.timeMs}ms")
                            }
                            Text("Quay lui (Backtracks): ${uiState.stats.backtracks}")
                            
                            if (uiState.isSolving && uiState.currentAlgorithm.name != "Dancing Links") {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = uiState.stats.progress,
                                    animationSpec = tween(300),
                                    label = "progress"
                                )
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (!uiState.isSolving && !uiState.isSolved) {
                    NumberPad(
                        onNumberClick = { viewModel.inputNumber(it) },
                        onClearClick = { viewModel.inputNumber(null) }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.loadSample() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("Ví dụ (Sample)")
                        }
                        Button(
                            onClick = { viewModel.solve() },
                            modifier = Modifier.weight(2f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Giải (Solve)")
                        }
                    }
                } else if (uiState.isSolving) {
                    Button(
                        onClick = { viewModel.stopSolving() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Dừng (Stop)")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

