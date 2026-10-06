package cz.mares.sudoku.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.mares.sudoku.engine.Difficulty
import cz.mares.sudoku.engine.GameMode
import cz.mares.sudoku.engine.SudokuCell
import cz.mares.sudoku.engine.SudokuEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SudokuGameState(
    val grid: List<List<SudokuCell>> = emptyList(),
    val selectedRow: Int? = null,
    val selectedCol: Int? = null,
    val isNotesMode: Boolean = false,
    val timerSeconds: Int = 0,
    val isHintUsed: Boolean = false,
    val hasMadeMistake: Boolean = false,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val isMainMenu: Boolean = true, // PŘIDÁNO: Určuje, zda jsme v hlavním menu
    val currentMode: GameMode = GameMode.CLASSIC,
    val currentDifficulty: Difficulty = Difficulty.EASY
)

class SudokuViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = SudokuEngine()
    private val prefs = application.getSharedPreferences("SudokuBestTimes", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(SudokuGameState())
    val state: StateFlow<SudokuGameState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var isTimerRunning = false

    init {
        loadGameStateFromPrefs()
    }

    fun getBestTime(mode: GameMode, difficulty: Difficulty): Int {
        return prefs.getInt("${mode.name}_${difficulty.name}", 0)
    }

    // PŘIDÁNO: Návrat do menu z rozehrané hry
    fun returnToMainMenu() {
        pauseTimer()
        _state.update { it.copy(isMainMenu = true) }
    }

    // PŘIDÁNO: Pokračování z menu do rozehrané hry
    fun resumeGameFromMenu() {
        _state.update { it.copy(isMainMenu = false, isPaused = false) }
        startTimer()
    }

    fun startNewGame(mode: GameMode, difficulty: Difficulty) {
        pauseTimer()
        prefs.edit().remove("saved_grid").apply()

        // Okamžitě opouštíme menu a jdeme do načítání
        _state.update { it.copy(grid = emptyList(), isGameOver = false, isPaused = false, isMainMenu = false) }

        viewModelScope.launch {
            val newGrid = withContext(Dispatchers.Default) {
                engine.generateGame(mode, difficulty)
            }

            _state.value = SudokuGameState(
                grid = newGrid,
                currentMode = mode,
                currentDifficulty = difficulty,
                isHintUsed = false,
                hasMadeMistake = false,
                timerSeconds = 0,
                isGameOver = false,
                isPaused = false,
                isMainMenu = false
            )
            saveGameStateToPrefs()
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (isTimerRunning && !_state.value.isGameOver && !_state.value.isPaused && !_state.value.isMainMenu) {
                delay(1000L)
                if (isTimerRunning) {
                    _state.update { it.copy(timerSeconds = it.timerSeconds + 1) }
                    if (_state.value.timerSeconds % 10 == 0) saveGameStateToPrefs()
                }
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        timerJob?.cancel()
        saveGameStateToPrefs()
    }

    fun resumeTimer() {
        if (!_state.value.isGameOver && _state.value.grid.isNotEmpty() && !_state.value.isPaused && !_state.value.isMainMenu) {
            startTimer()
        }
    }

    fun togglePause() {
        val currentState = _state.value
        if (currentState.isGameOver || currentState.grid.isEmpty() || currentState.isMainMenu) return

        if (currentState.isPaused) {
            _state.update { it.copy(isPaused = false) }
            startTimer()
        } else {
            pauseTimer()
            _state.update { it.copy(isPaused = true) }
        }
    }

    fun selectCell(row: Int, col: Int) {
        if (_state.value.isGameOver || _state.value.isPaused || _state.value.isMainMenu) return
        _state.update { it.copy(selectedRow = row, selectedCol = col) }
    }

    fun toggleNotesMode() {
        if (_state.value.isGameOver || _state.value.isPaused || _state.value.isMainMenu) return
        _state.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    fun eraseCell() {
        val currentState = _state.value
        if (currentState.isGameOver || currentState.isPaused || currentState.isMainMenu) return

        val row = currentState.selectedRow ?: return
        val col = currentState.selectedCol ?: return
        val cell = currentState.grid[row][col]

        if (cell.isGiven) return

        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
        newGrid[row][col] = cell.copy(value = 0, isError = false, notes = emptySet())

        _state.update { it.copy(grid = newGrid) }
        saveGameStateToPrefs()
    }

    fun onNumberInput(number: Int) {
        val currentState = _state.value
        if (currentState.isGameOver || currentState.isPaused || currentState.isMainMenu) return

        val row = currentState.selectedRow ?: return
        val col = currentState.selectedCol ?: return
        val cell = currentState.grid[row][col]

        if (cell.isGiven) return

        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
        var mistakeMadeNow = false

        if (currentState.isNotesMode) {
            val newNotes = cell.notes.toMutableSet()
            if (newNotes.contains(number)) {
                newNotes.remove(number)
            } else if (newNotes.size < 2) {
                newNotes.add(number)
            }
            newGrid[row][col] = cell.copy(notes = newNotes, value = 0, isError = false)
        } else {
            val intGrid = Array(9) { r -> IntArray(9) { c -> currentState.grid[r][c].value } }
            intGrid[row][col] = number

            for (r in 0 until 9) {
                for (c in 0 until 9) {
                    val currentCell = newGrid[r][c]
                    if (!currentCell.isGiven && currentCell.value != 0) {
                        val tempVal = intGrid[r][c]
                        intGrid[r][c] = 0
                        val isValid = engine.isValid(intGrid, r, c, tempVal, currentState.currentMode)
                        intGrid[r][c] = tempVal

                        if (!isValid) mistakeMadeNow = true
                        newGrid[r][c] = currentCell.copy(isError = !isValid)
                    }
                }
            }

            val isValidThisMove = engine.isValid(intGrid.apply { this[row][col] = 0 }, row, col, number, currentState.currentMode)
            if (!isValidThisMove) mistakeMadeNow = true
            newGrid[row][col] = cell.copy(value = number, notes = emptySet(), isError = !isValidThisMove)
        }

        _state.update { it.copy(
            grid = newGrid,
            hasMadeMistake = it.hasMadeMistake || mistakeMadeNow
        ) }
        saveGameStateToPrefs()
        checkWinCondition(newGrid)
    }

    fun useHint() {
        val currentState = _state.value
        if (currentState.isHintUsed || currentState.isGameOver || currentState.isPaused || currentState.isMainMenu) return

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

            val solved = withContext(Dispatchers.Default) {
                solveForHint(solverGrid, currentState.currentMode)
            }

            if (solved) {
                val correctNumber = solverGrid[row][col]
                val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
                newGrid[row][col] = cell.copy(value = correctNumber, isError = false, notes = emptySet())

                _state.update { it.copy(grid = newGrid, isHintUsed = true) }
                saveGameStateToPrefs()
                checkWinCondition(newGrid)
            }
        }
    }

    private fun checkWinCondition(grid: List<List<SudokuCell>>) {
        val isComplete = grid.flatten().all { it.value != 0 && !it.isError }
        if (isComplete) {
            _state.update { it.copy(isGameOver = true) }
            pauseTimer()
            prefs.edit().remove("saved_grid").apply()

            val finalState = _state.value

            if (!finalState.hasMadeMistake) {
                val currentBest = getBestTime(finalState.currentMode, finalState.currentDifficulty)
                if (currentBest == 0 || finalState.timerSeconds < currentBest) {
                    prefs.edit().putInt(
                        "${finalState.currentMode.name}_${finalState.currentDifficulty.name}",
                        finalState.timerSeconds
                    ).apply()
                }
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

    private fun saveGameStateToPrefs() {
        val state = _state.value
        if (state.grid.isEmpty() || state.isGameOver) return

        val gridString = state.grid.flatten().joinToString(";") { cell ->
            "${cell.row},${cell.col},${cell.value},${cell.isGiven},${cell.isError},${cell.notes.joinToString("-")}"
        }

        prefs.edit()
            .putString("saved_grid", gridString)
            .putString("saved_mode", state.currentMode.name)
            .putString("saved_diff", state.currentDifficulty.name)
            .putInt("saved_time", state.timerSeconds)
            .putBoolean("saved_hint", state.isHintUsed)
            .putBoolean("saved_mistake", state.hasMadeMistake)
            .apply()
    }

    private fun loadGameStateFromPrefs() {
        val gridString = prefs.getString("saved_grid", null) ?: return

        try {
            val cells = gridString.split(";").map { cellData ->
                val parts = cellData.split(",")
                val notesString = parts[5]
                SudokuCell(
                    row = parts[0].toInt(),
                    col = parts[1].toInt(),
                    value = parts[2].toInt(),
                    isGiven = parts[3].toBoolean(),
                    isError = parts[4].toBoolean(),
                    notes = if (notesString.isEmpty()) emptySet() else notesString.split("-").map { it.toInt() }.toSet()
                )
            }
            val loadedGrid = cells.chunked(9)
            val mode = GameMode.valueOf(prefs.getString("saved_mode", GameMode.CLASSIC.name)!!)
            val diff = Difficulty.valueOf(prefs.getString("saved_diff", Difficulty.EASY.name)!!)
            val time = prefs.getInt("saved_time", 0)
            val hint = prefs.getBoolean("saved_hint", false)
            val mistake = prefs.getBoolean("saved_mistake", false)

            _state.value = SudokuGameState(
                grid = loadedGrid,
                currentMode = mode,
                currentDifficulty = diff,
                timerSeconds = time,
                isHintUsed = hint,
                hasMadeMistake = mistake,
                isPaused = true,
                isMainMenu = true, // PŘIDÁNO: Při startu vždy zobrazíme menu!
                isGameOver = false
            )
        } catch (e: Exception) {
            prefs.edit().remove("saved_grid").apply()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
    }
}