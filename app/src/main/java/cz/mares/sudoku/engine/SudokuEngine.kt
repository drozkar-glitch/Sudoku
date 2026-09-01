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

    // ZRYCHLENÍ JIGSAW: Předpočítaná mapa souřadnic pro každý z 9 regionů
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

    private fun fillGrid(grid: Array<IntArray>, mode: GameMode): Boolean {
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (grid[r][c] == 0) {
                    val numbers = (1..9).shuffled()
                    for (num in numbers) {
                        if (isValid(grid, r, c, num, mode)) {
                            grid[r][c] = num
                            if (fillGrid(grid, mode)) return true
                            grid[r][c] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun countSolutions(grid: Array<IntArray>, mode: GameMode, limit: Int): Int {
        var emptyRow = -1
        var emptyCol = -1
        var isEmpty = false

        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (grid[i][j] == 0) {
                    emptyRow = i
                    emptyCol = j
                    isEmpty = true
                    break
                }
            }
            if (isEmpty) break
        }

        if (!isEmpty) return 1

        var count = 0
        for (num in 1..9) {
            if (isValid(grid, emptyRow, emptyCol, num, mode)) {
                grid[emptyRow][emptyCol] = num
                count += countSolutions(grid, mode, limit)
                grid[emptyRow][emptyCol] = 0
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
            // ZRYCHLENÉ HLEDÁNÍ: Místo 81 políček prochází přesně jen těch 9 v daném tvaru
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