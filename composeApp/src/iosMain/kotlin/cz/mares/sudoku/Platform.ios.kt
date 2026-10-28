package cz.mares.sudoku

import androidx.compose.runtime.Composable
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.darwin.NSObject

actual object SudokuStorage {
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual fun getInt(key: String, defaultValue: Int): Int {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.integerForKey(key).toInt()
    }

    actual fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    actual fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.boolForKey(key)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Na iOS se pro navrat do menu pouziva tlacitko Ukoncit v horni liste
}

actual fun exitApplication() {
    // Na iOS Apple nepodporuje programove ukonceni procesu, tlacitko je bezpecne osetreno
}
