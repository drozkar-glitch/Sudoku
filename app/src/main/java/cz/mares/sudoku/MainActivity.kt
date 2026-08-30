package cz.mares.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.mares.sudoku.engine.Difficulty
import cz.mares.sudoku.engine.GameMode
import cz.mares.sudoku.engine.SudokuCell
import cz.mares.sudoku.ui.theme.SudokuTheme
import cz.mares.sudoku.viewmodel.SudokuViewModel

class MainActivity : ComponentActivity() {

    // Propojení s ViewModelem, který řídí celou logiku
    private val viewModel: SudokuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Spustí novou hru pouze pokud je mřížka prázdná (první start),
        // čímž zabráníme resetu při minimalizaci nebo otočení telefonu.
        if (viewModel.state.value.grid.isEmpty()) {
            viewModel.startNewGame(GameMode.CLASSIC, Difficulty.EASY)
        }

        setContent {
            SudokuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SudokuScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SudokuScreen(viewModel: SudokuViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var showNewGameDialog by remember { mutableStateOf(false) }

    // Dialogové okno pro novou hru
    if (showNewGameDialog) {
        NewGameDialog(
            onDismiss = { showNewGameDialog = false },
            onConfirm = { mode, difficulty ->
                viewModel.startNewGame(mode, difficulty)
                showNewGameDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Hlavička s časem a tlačítkem pro novou hru
        TopBar(
            timerSeconds = state.timerSeconds,
            mode = state.currentMode,
            onNewGameClick = { showNewGameDialog = true }
        )

        // Herní mřížka (pokud je ještě prázdná, ukáže se načítání)
        if (state.grid.isNotEmpty()) {
            SudokuGrid(
                grid = state.grid,
                selectedRow = state.selectedRow,
                selectedCol = state.selectedCol,
                mode = state.currentMode,
                onCellClick = { r, c -> viewModel.selectCell(r, c) }
            )
        } else {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Akční panel (Nápověda, Poznámky, Guma)
        ActionRow(
            isNotesMode = state.isNotesMode,
            isHintUsed = state.isHintUsed,
            onToggleNotes = { viewModel.toggleNotesMode() },
            onUseHint = { viewModel.useHint() },
            onErase = { viewModel.eraseCell() }
        )

        // Číselník 1-9
        Numpad(onNumberClick = { viewModel.onNumberInput(it) })
    }
}

@Composable
fun TopBar(timerSeconds: Int, mode: GameMode, onNewGameClick: () -> Unit) {
    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onNewGameClick) {
            Text("Nová hra (${mode.name})")
        }
        Text(text = String.format("Čas: %02d:%02d", minutes, seconds), fontSize = 20.sp)
    }
}

@Composable
fun NewGameDialog(onDismiss: () -> Unit, onConfirm: (GameMode, Difficulty) -> Unit) {
    var selectedMode by remember { mutableStateOf(GameMode.CLASSIC) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nová hra", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = "Mód hry:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                GameMode.values().forEach { mode ->
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

                Text(text = "Obtížnost:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Difficulty.values().forEach { diff ->
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
                        Text(text = diff.name, modifier = Modifier.padding(start = 8.dp))
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
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
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
    // Vykreslení černého ohraničení celé mřížky
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(2.dp, Color.Black)
    ) {
        for (row in 0 until 9) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 9) {
                    val cell = grid[row][col]
                    val isSelected = row == selectedRow && col == selectedCol

                    // Zesílení hran pro čtverce 3x3
                    val paddingBottom = if (row % 3 == 2 && row != 8) 2.dp else 0.5.dp
                    val paddingRight = if (col % 3 == 2 && col != 8) 2.dp else 0.5.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black) // Barva pro linky mřížky
                            .padding(bottom = paddingBottom, end = paddingRight)
                            .background(getCellColor(row, col, isSelected, cell.isError, mode))
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell.value != 0) {
                            // Zobrazení pevného čísla
                            Text(
                                text = cell.value.toString(),
                                fontSize = 24.sp,
                                fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.Normal,
                                color = if (cell.isError) Color.Red else if (cell.isGiven) Color.Black else Color.Blue
                            )
                        } else if (cell.notes.isNotEmpty()) {
                            // Zobrazení malých poznámek
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
    if (isError) return Color(0xFFFFCDD2) // Červený podkres pro chybu
    if (isSelected) return Color(0xFFBBDEFB) // Modrý podkres pro vybrané políčko

    // Zvýraznění hlavní úhlopříčky pro Sudoku X
    if (mode == GameMode.X && (row == col || row + col == 8)) {
        return Color(0xFFF5F5F5)
    }

    // Zvýraznění pro Window Sudoku
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
    Column(modifier = Modifier.fillMaxSize().padding(2.dp)) {
        for (r in 0 until 3) {
            Row(modifier = Modifier.weight(1f)) {
                for (c in 0 until 3) {
                    val num = r * 3 + c + 1
                    Text(
                        text = if (notes.contains(num)) num.toString() else "",
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Vynutí pevné mezery
    ) {
        Button(
            onClick = onUseHint,
            enabled = !isHintUsed,
            modifier = Modifier.weight(1f), // Rovnoměrné rozložení šířky
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isHintUsed) Color.Gray else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isHintUsed) "Nápověda(0)" else "Nápověda(1)", maxLines = 1, fontSize = 12.sp)
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
            Text("Poznámky", maxLines = 1, fontSize = 12.sp)
        }

        Button(
            onClick = onErase,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Smazat", maxLines = 1, fontSize = 12.sp)
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
                        .height(64.dp), // Pevná výška pro pohodlnější ovládání
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
            Spacer(modifier = Modifier.weight(1f)) // Prázdné místo pro zarovnání s horní řadou
        }
    }
}