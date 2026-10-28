package cz.mares.sudoku

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import cz.mares.sudoku.viewmodel.SudokuViewModel
import platform.UIKit.UIApplication

fun MainViewController() = ComposeUIViewController {
    UIApplication.sharedApplication.idleTimerDisabled = true
    val viewModel = remember { SudokuViewModel() }
    App(viewModel = viewModel)
}
