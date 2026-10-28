// Konfigurace multiplatformniho projektu Sudoku (cz.mares.sudoku) - Pavel Mares
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sudoku"
include(":composeApp")
