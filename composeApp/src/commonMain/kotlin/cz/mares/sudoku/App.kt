package cz.mares.sudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.mares.sudoku.engine.Difficulty
import cz.mares.sudoku.engine.GameMode
import cz.mares.sudoku.engine.SudokuCell
import cz.mares.sudoku.viewmodel.SudokuViewModel

private val JIGSAW_LAYOUT = arrayOf(
    intArrayOf(0, 0, 0, 0, 1, 1, 1, 2, 2),
    intArrayOf(0, 0, 0, 0, 1, 1, 1, 2, 2),
    intArrayOf(0, 3, 3, 3, 1, 1, 1, 2, 2),
    intArrayOf(3, 3, 3, 3, 4, 4, 4, 2, 2),
    intArrayOf(3, 3, 5, 5, 4, 4, 4, 2, 7),
    intArrayOf(6, 6, 5, 5, 4, 4, 4, 7, 7),
    intArrayOf(6, 6, 5, 5, 5, 8, 8, 7, 7),
    intArrayOf(6, 6, 5, 5, 8, 8, 8, 7, 7),
    intArrayOf(6, 6, 6, 8, 8, 8, 8, 7, 7)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun App(viewModel: SudokuViewModel = remember { SudokuViewModel() }) {
    SudokuTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) { innerPadding ->
            val state by viewModel.state.collectAsState()

            if (state.isMainMenu) {
                MainMenuScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                SudokuScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun MainMenuScreen(viewModel: SudokuViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var showNewGameDialog by remember { mutableStateOf(false) }

    if (showNewGameDialog) {
        NewGameDialog(
            onDismiss = { showNewGameDialog = false },
            onConfirm = { mode, difficulty ->
                viewModel.startNewGame(mode, difficulty)
                showNewGameDialog = false
            },
            getBestTime = { mode, diff -> viewModel.getBestTime(mode, diff) }
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SUDOKU",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 64.dp)
        )

        if (state.grid.isNotEmpty() && !state.isGameOver) {
            Button(
                onClick = { viewModel.resumeGameFromMenu() },
                modifier = Modifier.fillMaxWidth(0.6f).height(60.dp)
            ) {
                Text("Pokračovat", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { showNewGameDialog = true },
            modifier = Modifier.fillMaxWidth(0.6f).height(60.dp)
        ) {
            Text("Nová hra", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { exitApplication() },
            modifier = Modifier.fillMaxWidth(0.6f).height(60.dp)
        ) {
            Text("Ukončit", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "© 2026 Created by Pavel Mareš",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SudokuScreen(viewModel: SudokuViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var showNewGameDialog by remember { mutableStateOf(false) }

    PlatformBackHandler {
        viewModel.returnToMainMenu()
    }

    if (state.isGameOver && state.grid.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = "Výborně!", fontWeight = FontWeight.Bold) },
            text = {
                Text("Hru jsi úspěšně dokončil.\nVýsledný čas: ${formatMinutesSeconds(state.timerSeconds)}")
            },
            confirmButton = {
                Button(onClick = { showNewGameDialog = true }) {
                    Text("Nová hra")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.returnToMainMenu() }) {
                    Text("Do menu")
                }
            }
        )
    }

    if (showNewGameDialog) {
        NewGameDialog(
            onDismiss = { showNewGameDialog = false },
            onConfirm = { mode, difficulty ->
                viewModel.startNewGame(mode, difficulty)
                showNewGameDialog = false
            },
            getBestTime = { mode, diff -> viewModel.getBestTime(mode, diff) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(
            timerSeconds = state.timerSeconds,
            mode = state.currentMode,
            isPaused = state.isPaused,
            onMenuClick = { viewModel.returnToMainMenu() },
            onPauseClick = { viewModel.togglePause() }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (state.isPaused) {
                Button(onClick = { viewModel.togglePause() }, modifier = Modifier.padding(32.dp)) {
                    Text("Hra pozastavena\nPOKRAČOVAT", textAlign = TextAlign.Center, fontSize = 20.sp)
                }
            } else if (state.grid.isNotEmpty()) {
                SudokuGrid(
                    grid = state.grid,
                    selectedRow = state.selectedRow,
                    selectedCol = state.selectedCol,
                    mode = state.currentMode,
                    onCellClick = { r, c -> viewModel.selectCell(r, c) }
                )
            } else {
                CircularProgressIndicator()
            }
        }

        ActionRow(
            isNotesMode = state.isNotesMode,
            isHintUsed = state.isHintUsed,
            onToggleNotes = { viewModel.toggleNotesMode() },
            onUseHint = { viewModel.useHint() },
            onErase = { viewModel.eraseCell() }
        )

        Numpad(onNumberClick = { viewModel.onNumberInput(it) })

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "© 2026 Created by Pavel Mareš",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TopBar(
    timerSeconds: Int,
    mode: GameMode,
    isPaused: Boolean,
    onMenuClick: () -> Unit,
    onPauseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onMenuClick) {
            Text("Ukončit")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Čas: ${formatMinutesSeconds(timerSeconds)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onPauseClick,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Text(if (isPaused) "▶" else "⏸", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun NewGameDialog(
    onDismiss: () -> Unit,
    onConfirm: (GameMode, Difficulty) -> Unit,
    getBestTime: (GameMode, Difficulty) -> Int
) {
    var selectedMode by remember { mutableStateOf(GameMode.CLASSIC) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nová hra", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = "Mód hry:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                GameMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = mode }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                        Text(text = mode.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Obtížnost a rekordy:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Difficulty.entries.forEach { diff ->
                    val bestSecs = getBestTime(selectedMode, diff)
                    val timeStr = if (bestSecs > 0) formatMinutesSeconds(bestSecs) else "--:--"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDifficulty = diff }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedDifficulty == diff,
                            onClick = { selectedDifficulty = diff }
                        )
                        Text(
                            text = "${diff.name} (Nejlepší: $timeStr)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMode, selectedDifficulty) }) {
                Text("Spustit")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { exitApplication() }) {
                    Text("Ukončit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Zrušit")
                }
            }
        }
    )
}

@Composable
fun SudokuGrid(
    grid: List<List<SudokuCell>>,
    selectedRow: Int?,
    selectedCol: Int?,
    mode: GameMode,
    onCellClick: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .border(2.dp, Color.Black)
    ) {
        for (row in 0 until 9) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 9) {
                    val cell = grid[row][col]
                    val isSelected = row == selectedRow && col == selectedCol

                    val isThickBottom = if (row == 8) false else {
                        if (mode == GameMode.JIGSAW) {
                            JIGSAW_LAYOUT[row][col] != JIGSAW_LAYOUT[row + 1][col]
                        } else {
                            row % 3 == 2
                        }
                    }

                    val isThickRight = if (col == 8) false else {
                        if (mode == GameMode.JIGSAW) {
                            JIGSAW_LAYOUT[row][col] != JIGSAW_LAYOUT[row][col + 1]
                        } else {
                            col % 3 == 2
                        }
                    }

                    val paddingBottom = if (isThickBottom) 2.dp else 0.5.dp
                    val paddingRight = if (isThickRight) 2.dp else 0.5.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black)
                            .padding(bottom = paddingBottom, end = paddingRight)
                            .background(getCellColor(row, col, isSelected, cell.isError, mode))
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell.value != 0) {
                            Text(
                                text = cell.value.toString(),
                                fontSize = 24.sp,
                                fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.Normal,
                                color = if (cell.isError) Color.Red else if (cell.isGiven) Color.Black else Color.Blue
                            )
                        } else if (cell.notes.isNotEmpty()) {
                            NotesGrid(cell.notes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getCellColor(row: Int, col: Int, isSelected: Boolean, isError: Boolean, mode: GameMode): Color {
    if (isError) return Color(0xFFFFCDD2)
    if (isSelected) return Color(0xFFBBDEFB)

    if (mode == GameMode.X && (row == col || row + col == 8)) {
        return Color(0xFFF5F5F5)
    }
    if (mode == GameMode.WINDOW) {
        val inWindow = (row in 1..3 && col in 1..3) ||
                (row in 1..3 && col in 5..7) ||
                (row in 5..7 && col in 1..3) ||
                (row in 5..7 && col in 5..7)
        if (inWindow) return Color(0xFFF0F4C3)
    }
    return Color.White
}

@Composable
fun NotesGrid(notes: Set<Int>) {
    Box(
        modifier = Modifier.fillMaxSize().padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = notes.sorted().joinToString(" "),
            fontSize = 14.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun ActionRow(
    isNotesMode: Boolean,
    isHintUsed: Boolean,
    onToggleNotes: () -> Unit,
    onUseHint: () -> Unit,
    onErase: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onUseHint,
            enabled = !isHintUsed,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isHintUsed) Color.Gray else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isHintUsed) "Nápověda(0)" else "Nápověda(1)",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
        Button(
            onClick = onToggleNotes,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isNotesMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isNotesMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Poznámky", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
        Button(
            onClick = onErase,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Smazat", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
    }
}

@Composable
fun Numpad(onNumberClick: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..5) {
                Button(
                    onClick = { onNumberClick(i) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(i.toString(), fontSize = 24.sp)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 6..9) {
                Button(
                    onClick = { onNumberClick(i) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(i.toString(), fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
