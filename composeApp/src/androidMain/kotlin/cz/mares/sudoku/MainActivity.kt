package cz.mares.sudoku

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.mares.sudoku.viewmodel.SudokuViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: SudokuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidAppContext.init(this)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel = SudokuViewModel()

        setContent {
            App(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidAppContext.init(this)
    }

    override fun onPause() {
        super.onPause()
        if (::viewModel.isInitialized) {
            viewModel.onAppPaused()
        }
    }
}
