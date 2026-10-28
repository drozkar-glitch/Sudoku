package cz.mares.sudoku

import androidx.compose.runtime.Composable

// Multiplatformni uloziste pro ukladani rozehrane hry a rekordu Sudoku
expect object SudokuStorage {
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

// Multiplatformni obsluha tlacitka Zpet a ukonceni aplikace
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

expect fun exitApplication()

// Pomocna funkce pro formatovani casu MM:SS bez zavislosti na java.lang.String.format
fun formatMinutesSeconds(totalSeconds: Int): String {
    val minutes = (totalSeconds / 60).toString().padStart(2, '0')
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
