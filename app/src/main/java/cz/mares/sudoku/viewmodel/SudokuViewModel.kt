package cz.mares.sudoku.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.mares.sudoku.engine.Difficulty
import cz.mares.sudoku.engine.GameMode
import cz.mares.sudoku.engine.SudokuCell
import cz.mares.sudoku.engine.SudokuEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Datová třída uchovávající kompletní aktuální stav obrazovky
data class SudokuGameState(
    val grid: List<List<SudokuCell>> = emptyList(),
    val selectedRow: Int? = null,
    val selectedCol: Int? = null,
    val isNotesMode: Boolean = false,
    val timerSeconds: Int = 0,
    val isHintUsed: Boolean = false,
    val isGameOver: Boolean = false,
    val currentMode: GameMode = GameMode.CLASSIC,
    val currentDifficulty: Difficulty = Difficulty.EASY
)

class SudokuViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = SudokuEngine()

    // Trvalá paměť pro nejlepší časy (4x3)
    private val prefs = application.getSharedPreferences("SudokuBestTimes", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(SudokuGameState())
    val state: StateFlow<SudokuGameState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var isTimerRunning = false

    fun getBestTime(mode: GameMode, difficulty: Difficulty): Int {
        return prefs.getInt("${mode.name}_${difficulty.name}", 0)
    }

    fun startNewGame(mode: GameMode, difficulty: Difficulty) {
        viewModelScope.launch {
            val newGrid = engine.generateGame(mode, difficulty)
            _state.value = SudokuGameState(
                grid = newGrid,
                currentMode = mode,
                currentDifficulty = difficulty,
                isHintUsed = false,
                timerSeconds = 0,
                isGameOver = false
            )
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (isTimerRunning && !_state.value.isGameOver) {
                delay(1000L)
                if (isTimerRunning) {
                    _state.update { it.copy(timerSeconds = it.timerSeconds + 1) }
                }
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (!_state.value.isGameOver && _state.value.grid.isNotEmpty() && !isTimerRunning) {
            startTimer()
        }
    }

    fun selectCell(row: Int, col: Int) {
        if (_state.value.isGameOver) return
        _state.update { it.copy(selectedRow = row, selectedCol = col) }
    }

    fun toggleNotesMode() {
        if (_state.value.isGameOver) return
        _state.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    fun eraseCell() {
        val currentState = _state.value
        if (currentState.isGameOver) return

        val row = currentState.selectedRow ?: return
        val col = currentState.selectedCol ?: return
        val cell = currentState.grid[row][col]

        if (cell.isGiven) return

        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
        // Guma nyní bezpečně maže klasická čísla i poznámky
        newGrid[row][col] = cell.copy(value = 0, isError = false, notes = emptySet())

        _state.update { it.copy(grid = newGrid) }
    }

    fun onNumberInput(number: Int) {
        val currentState = _state.value
        if (currentState.isGameOver) return

        val row = currentState.selectedRow ?: return
        val col = currentState.selectedCol ?: return
        val cell = currentState.grid[row][col]

        if (cell.isGiven) return

        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()

        if (currentState.isNotesMode) {
            val newNotes = cell.notes.toMutableSet()
            if (newNotes.contains(number)) newNotes.remove(number) else newNotes.add(number)
            newGrid[row][col] = cell.copy(notes = newNotes, value = 0, isError = false)
        } else {
            val intGrid = Array(9) { r -> IntArray(9) { c -> currentState.grid[r][c].value } }
            intGrid[row][col] = 0

            val isValid = engine.isValid(intGrid, row, col, number, currentState.currentMode)
            newGrid[row][col] = cell.copy(value = number, notes = emptySet(), isError = !isValid)
        }

        _state.update { it.copy(grid = newGrid) }
        checkWinCondition(newGrid)
    }

    fun useHint() {
        val currentState = _state.value
        if (currentState.isHintUsed || currentState.isGameOver) return

        val row = currentState.selectedRow ?: return
        val col = currentState.selectedCol ?: return
        val cell = currentState.grid[row][col]

        if (cell.isGiven) return

        viewModelScope.launch {
            val solverGrid = Array(9) { r ->
                IntArray(9) { c ->
                    if (currentState.grid[r][c].isGiven) currentState.grid[r][c].value else 0
                }
            }

            if (solveForHint(solverGrid, currentState.currentMode)) {
                val correctNumber = solverGrid[row][col]

                val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
                newGrid[row][col] = cell.copy(value = correctNumber, isError = false, notes = emptySet())

                _state.update { it.copy(grid = newGrid, isHintUsed = true) }
                checkWinCondition(newGrid)
            }
        }
    }

    private fun checkWinCondition(grid: List<List<SudokuCell>>) {
        val isComplete = grid.flatten().all { it.value != 0 && !it.isError }
        if (isComplete) {
            _state.update { it.copy(isGameOver = true) }
            pauseTimer()

            val finalState = _state.value
            val currentBest = getBestTime(finalState.currentMode, finalState.currentDifficulty)

            if (currentBest == 0 || finalState.timerSeconds < currentBest) {
                prefs.edit().putInt(
                    "${finalState.currentMode.name}_${finalState.currentDifficulty.name}",
                    finalState.timerSeconds
                ).apply()
            }
        }
    }

    private fun solveForHint(grid: Array<IntArray>, mode: GameMode): Boolean {
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (grid[r][c] == 0) {
                    for (num in 1..9) {
                        if (engine.isValid(grid, r, c, num, mode)) {
                            grid[r][c] = num
                            if (solveForHint(grid, mode)) return true
                            grid[r][c] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
    }
}