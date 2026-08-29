package cz.mares.sudoku.engine

import kotlin.random.Random

// 1. DATOVÉ MODELY
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

// 2. HERNÍ ENGINE
class SudokuEngine {

    // Univerzální rozložení pro Jigsaw mód (9 regionů, každý má přesně 9 políček)
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

    /**
     * Vygeneruje novou hru s ohledem na zvolený mód a obtížnost.
     * Vrátí 2D pole SudokuCell připravené pro UI.
     */
    fun generateGame(mode: GameMode, difficulty: Difficulty): List<List<SudokuCell>> {
        val grid = Array(9) { IntArray(9) }

        // Krok 1: Naplnění mřížky správnými čísly
        fillGrid(grid, mode)

        // Krok 2: Odebírání čísel podle obtížnosti (Varianta A)
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
            grid[r][c] = 0 // Zkusíme odebrat

            // Krok 3: Kontrola, zda existuje STÁLE jen jedno řešení
            if (countSolutions(grid, mode, limit = 2) == 1) {
                cellsToRemove-- // Pokud ano, odebrání platí
            } else {
                grid[r][c] = temp // Pokud ne, musíme číslo vrátit
            }
        }

        // Krok 4: Mapování do datové třídy pro UI
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
     * Algoritmus Backtracking pro rychlé nalezení jedné validní plné mřížky.
     */
    private fun fillGrid(grid: Array<IntArray>, mode: GameMode): Boolean {
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (grid[r][c] == 0) {
                    val numbers = (1..9).shuffled()
                    for (num in numbers) {
                        if (isValid(grid, r, c, num, mode)) {
                            grid[r][c] = num
                            if (fillGrid(grid, mode)) return true
                            grid[r][c] = 0 // Backtrack
                        }
                    }
                    return false // Slepá ulička
                }
            }
        }
        return true // Mřížka je plná
    }

    /**
     * Spočítá počet možných řešení. Slouží k ověření, že uživatel nemusí hádat.
     */
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

        if (!isEmpty) return 1 // Žádné prázdné místo = 1 řešení nalezeno

        var count = 0
        for (num in 1..9) {
            if (isValid(grid, emptyRow, emptyCol, num, mode)) {
                grid[emptyRow][emptyCol] = num
                count += countSolutions(grid, mode, limit)
                grid[emptyRow][emptyCol] = 0 // Backtrack
                if (count >= limit) return count // Zrychlení výpočtu, 2 řešení znamenají neplatné Sudoku
            }
        }
        return count
    }

    /**
     * UNIVERZÁLNÍ VALIDÁTOR - Zde se aplikují pravidla pro všechny verze hry.
     */
    fun isValid(grid: Array<IntArray>, row: Int, col: Int, num: Int, mode: GameMode): Boolean {
        // 1. Zkouška řádku a sloupce (platí pro všechny módy)
        for (i in 0 until 9) {
            if (grid[row][i] == num) return false
            if (grid[i][col] == num) return false
        }

        // 2. Zkouška bloků / regionů
        if (mode == GameMode.JIGSAW) {
            // Kontrola nepravidelného tvaru
            val regionId = jigsawLayout[row][col]
            for (r in 0 until 9) {
                for (c in 0 until 9) {
                    if (jigsawLayout[r][c] == regionId && grid[r][c] == num) return false
                }
            }
        } else {
            // Klasický čtverec 3x3 (Klasika, X, Window)
            val startRow = row - row % 3
            val startCol = col - col % 3
            for (r in 0 until 3) {
                for (c in 0 until 3) {
                    if (grid[startRow + r][startCol + c] == num) return false
                }
            }
        }

        // 3. Dodatečná zkouška pro Sudoku X (Úhlopříčky)
        if (mode == GameMode.X) {
            if (row == col) {
                for (i in 0 until 9) if (grid[i][i] == num) return false
            }
            if (row + col == 8) {
                for (i in 0 until 9) if (grid[i][8 - i] == num) return false
            }
        }

        // 4. Dodatečná zkouška pro Window Sudoku (4 vnitřní okenice 3x3)
        if (mode == GameMode.WINDOW) {
            // První okno
            if (row in 1..3 && col in 1..3) {
                for (r in 1..3) for (c in 1..3) if (grid[r][c] == num) return false
            }
            // Druhé okno
            if (row in 1..3 && col in 5..7) {
                for (r in 1..3) for (c in 5..7) if (grid[r][c] == num) return false
            }
            // Třetí okno
            if (row in 5..7 && col in 1..3) {
                for (r in 5..7) for (c in 1..3) if (grid[r][c] == num) return false
            }
            // Čtvrté okno
            if (row in 5..7 && col in 5..7) {
                for (r in 5..7) for (c in 5..7) if (grid[r][c] == num) return false
            }
        }

        return true // Políčko vyhovuje všem aktivním pravidlům
    }
}