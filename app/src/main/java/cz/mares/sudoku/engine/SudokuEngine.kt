package cz.mares.sudoku.engine

import kotlin.random.Random

enum class GameMode {
    CLASSIC, X, JIGSAW, WINDOW
}

enum class Difficulty(val clues: Int) {
    EASY(40),
    MEDIUM(32),
    HARD(25)
}

data class SudokuCell(
    val row: Int,
    val col: Int,
    val value: Int = 0,
    val isGiven: Boolean = false,
    val notes: Set<Int> = emptySet(),
    val isError: Boolean = false,
    val isSelected: Boolean = false
)

class SudokuEngine {

    private val jigsawLayout = arrayOf(
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

    private val jigsawRegions: Array<Array<Pair<Int, Int>>> = Array(9) { emptyArray() }

    init {
        val tempRegions = Array(9) { mutableListOf<Pair<Int, Int>>() }
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                tempRegions[jigsawLayout[r][c]].add(Pair(r, c))
            }
        }
        for (i in 0 until 9) {
            jigsawRegions[i] = tempRegions[i].toTypedArray()
        }
    }

    fun generateGame(mode: GameMode, difficulty: Difficulty): List<List<SudokuCell>> {
        val grid = Array(9) { IntArray(9) }

        fillGrid(grid, mode)

        var cellsToRemove = 81 - difficulty.clues
        val positions = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                positions.add(Pair(r, c))
            }
        }
        positions.shuffle(Random(System.currentTimeMillis()))

        for (pos in positions) {
            if (cellsToRemove <= 0) break
            val r = pos.first
            val c = pos.second

            val temp = grid[r][c]
            grid[r][c] = 0

            // Zrychlený validátor nyní dokáže tuto zkoušku udělat mrknutím oka
            if (countSolutions(grid, mode, limit = 2) == 1) {
                cellsToRemove--
            } else {
                grid[r][c] = temp
            }
        }

        return grid.mapIndexed { rowIndex, row ->
            row.mapIndexed { colIndex, value ->
                SudokuCell(
                    row = rowIndex,
                    col = colIndex,
                    value = value,
                    isGiven = value != 0
                )
            }
        }
    }

    /**
     * MRV algoritmus: Najde to prázdné políčko, které má NEJMÉNĚ možných platných čísel.
     * Zabrání tomu, aby se počítač zacyklil u složitých nepravidelných tvarů.
     */
    private fun findBestEmptyCell(grid: Array<IntArray>, mode: GameMode): Pair<Int, Int>? {
        var bestRow = -1
        var bestCol = -1
        var minOptions = 10

        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (grid[r][c] == 0) {
                    var options = 0
                    for (num in 1..9) {
                        if (isValid(grid, r, c, num, mode)) options++
                    }

                    // Brutální urychlení: Pokud do buňky nejde nic napsat, je to slepá ulička, rovnou to ukončí
                    if (options == 0) return Pair(-1, -1)

                    if (options < minOptions) {
                        minOptions = options
                        bestRow = r
                        bestCol = c
                        // Pokud najdeme buňku s jedinou možností, nehledáme dál
                        if (minOptions == 1) return Pair(bestRow, bestCol)
                    }
                }
            }
        }
        if (bestRow == -1) return null // Všechna políčka jsou vyplněná
        return Pair(bestRow, bestCol)
    }

    private fun fillGrid(grid: Array<IntArray>, mode: GameMode): Boolean {
        val bestCell = findBestEmptyCell(grid, mode)
        if (bestCell == null) return true // Mřížka je plná
        if (bestCell.first == -1) return false // Hra narazila na neřešitelnou slepou uličku

        val r = bestCell.first
        val c = bestCell.second

        val numbers = (1..9).shuffled()
        for (num in numbers) {
            if (isValid(grid, r, c, num, mode)) {
                grid[r][c] = num
                if (fillGrid(grid, mode)) return true
                grid[r][c] = 0 // Backtrack
            }
        }
        return false
    }

    private fun countSolutions(grid: Array<IntArray>, mode: GameMode, limit: Int): Int {
        val bestCell = findBestEmptyCell(grid, mode)
        if (bestCell == null) return 1 // Žádné volné místo = 1 řešení
        if (bestCell.first == -1) return 0 // Slepá ulička

        val r = bestCell.first
        val c = bestCell.second
        var count = 0

        for (num in 1..9) {
            if (isValid(grid, r, c, num, mode)) {
                grid[r][c] = num
                count += countSolutions(grid, mode, limit)
                grid[r][c] = 0 // Backtrack
                if (count >= limit) return count
            }
        }
        return count
    }

    fun isValid(grid: Array<IntArray>, row: Int, col: Int, num: Int, mode: GameMode): Boolean {
        for (i in 0 until 9) {
            if (grid[row][i] == num) return false
            if (grid[i][col] == num) return false
        }

        if (mode == GameMode.JIGSAW) {
            val regionId = jigsawLayout[row][col]
            for (cell in jigsawRegions[regionId]) {
                if (grid[cell.first][cell.second] == num) return false
            }
        } else {
            val startRow = row - row % 3
            val startCol = col - col % 3
            for (r in 0 until 3) {
                for (c in 0 until 3) {
                    if (grid[startRow + r][startCol + c] == num) return false
                }
            }
        }

        if (mode == GameMode.X) {
            if (row == col) {
                for (i in 0 until 9) if (grid[i][i] == num) return false
            }
            if (row + col == 8) {
                for (i in 0 until 9) if (grid[i][8 - i] == num) return false
            }
        }

        if (mode == GameMode.WINDOW) {
            if (row in 1..3 && col in 1..3) {
                for (r in 1..3) for (c in 1..3) if (grid[r][c] == num) return false
            }
            if (row in 1..3 && col in 5..7) {
                for (r in 1..3) for (c in 5..7) if (grid[r][c] == num) return false
            }
            if (row in 5..7 && col in 1..3) {
                for (r in 5..7) for (c in 1..3) if (grid[r][c] == num) return false
            }
            if (row in 5..7 && col in 5..7) {
                for (r in 5..7) for (c in 5..7) if (grid[r][c] == num) return false
            }
        }

        return true
    }
}