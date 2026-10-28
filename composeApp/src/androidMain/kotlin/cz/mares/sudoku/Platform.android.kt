package cz.mares.sudoku

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import java.lang.ref.WeakReference

internal object AndroidAppContext {
    private var prefs: SharedPreferences? = null
    var currentActivity: WeakReference<Activity>? = null

    fun init(activity: Activity) {
        currentActivity = WeakReference(activity)
        if (prefs == null) {
            prefs = activity.applicationContext.getSharedPreferences("SudokuBestTimes", Context.MODE_PRIVATE)
        }
    }

    fun getPrefs(): SharedPreferences? = prefs
}

actual object SudokuStorage {
    actual fun getInt(key: String, defaultValue: Int): Int {
        return AndroidAppContext.getPrefs()?.getInt(key, defaultValue) ?: defaultValue
    }

    actual fun putInt(key: String, value: Int) {
        AndroidAppContext.getPrefs()?.edit()?.putInt(key, value)?.apply()
    }

    actual fun getString(key: String): String? {
        return AndroidAppContext.getPrefs()?.getString(key, null)
    }

    actual fun putString(key: String, value: String) {
        AndroidAppContext.getPrefs()?.edit()?.putString(key, value)?.apply()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return AndroidAppContext.getPrefs()?.getBoolean(key, defaultValue) ?: defaultValue
    }

    actual fun putBoolean(key: String, value: Boolean) {
        AndroidAppContext.getPrefs()?.edit()?.putBoolean(key, value)?.apply()
    }

    actual fun remove(key: String) {
        AndroidAppContext.getPrefs()?.edit()?.remove(key)?.apply()
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

actual fun exitApplication() {
    AndroidAppContext.currentActivity?.get()?.finish()
}
