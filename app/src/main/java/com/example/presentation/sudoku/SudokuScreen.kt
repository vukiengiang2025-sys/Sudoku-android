package com.example.presentation.sudoku

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.components.NumberPad
import com.example.presentation.components.SudokuGrid
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SudokuScreen(viewModel: SudokuViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Win animation state
    var showWinPulse by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isSolved) {
        if (uiState.isSolved) {
            showWinPulse = true
            delay(500)
            showWinPulse = false
        }
    }

    val gridScale by animateFloatAsState(
        targetValue = if (showWinPulse) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "gridScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Sudoku Solver", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadSample() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Load Sample")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = uiState.currentAlgorithm.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("AI Core") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
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
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Engine Speed", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SolverSpeed.entries.forEach { speed ->
                                    FilterChip(
                                        selected = uiState.currentSpeed == speed,
                                        onClick = { viewModel.setSpeed(speed) },
                                        label = { Text(speed.name) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SudokuGrid(
                    grid = uiState.board.grid,
                    selectedCell = uiState.selectedCell,
                    onCellClick = viewModel::selectCell,
                    isSolving = uiState.isSolving,
                    modifier = Modifier.fillMaxWidth(0.95f).scale(gridScale)
                )
            }

            item {
                AnimatedVisibility(
                    visible = uiState.isSolving || uiState.isSolved || uiState.noSolution,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                uiState.isSolved -> Color(0xFFE8F5E9)
                                uiState.noSolution -> Color(0xFFFFEBEE)
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.isSolving) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else if (uiState.isSolved) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                }
                                Text(
                                    text = when {
                                        uiState.isSolved -> "Mission Accomplished!"
                                        uiState.noSolution -> "System Failure: No Solution"
                                        else -> "AI Processing..."
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        uiState.isSolved -> Color(0xFF2E7D32)
                                        uiState.noSolution -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatBadge("Steps", "${uiState.stats.steps}")
                                StatBadge("Time", "${uiState.stats.timeMs}ms")
                                StatBadge("Backtracks", "${uiState.stats.backtracks}")
                            }
                            
                            if (uiState.isSolving && uiState.currentAlgorithm.name != "Dancing Links") {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = uiState.stats.progress,
                                    animationSpec = tween(300),
                                    label = "progress"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                AnimatedContent(
                    targetState = uiState.isSolving,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "controls"
                ) { isSolving ->
                    if (!isSolving && !uiState.isSolved) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            NumberPad(
                                onNumberClick = { viewModel.inputNumber(it) },
                                onClearClick = { viewModel.inputNumber(null) }
                            )
                            
                            Button(
                                onClick = { viewModel.solve() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("INITIALIZE SOLVER", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    } else if (isSolving) {
                        Button(
                            onClick = { viewModel.stopSolving() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("HALT PROCESS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    } else if (uiState.isSolved || uiState.noSolution) {
                         Button(
                            onClick = { viewModel.clearBoard() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("RESET BOARD", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatBadge(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}
