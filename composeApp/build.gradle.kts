import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.2")
        }
    }
}

android {
    namespace = "cz.mares.sudoku"
    compileSdk = 36

    defaultConfig {
        applicationId = "cz.mares.sudoku"
        minSdk = 24
        targetSdk = 36
        val ciBuildNumber = System.getenv("PROJECT_BUILD_NUMBER")?.toIntOrNull() ?: 1
        versionCode = ciBuildNumber
        versionName = "1.0.$ciBuildNumber"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("CM_KEYSTORE_PATH")
            if (!keystorePath.isNullOrEmpty()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("CM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CM_KEY_ALIAS")
                keyPassword = System.getenv("CM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            val keystorePath = System.getenv("CM_KEYSTORE_PATH")
            if (!keystorePath.isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Automaticke generovani 1024x1024 RGB PNG ikony Sudoku pro iOS bez alfa kanalu
val generateIosIcon = tasks.register("generateIosIcon") {
    doLast {
        val iconFile = rootProject.file("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png")
        iconFile.parentFile.mkdirs()

        val width = 1024
        val height = 1024
        val rawRows = ByteArrayOutputStream(height * (1 + width * 3))

        fun insideRoundedRect(px: Double, py: Double, x: Double, y: Double, w: Double, h: Double, r: Double): Boolean {
            if (px < x || px > x + w || py < y || py > y + h) return false
            val cx = when {
                px < x + r -> x + r
                px > x + w - r -> x + w - r
                else -> px
            }
            val cy = when {
                py < y + r -> y + r
                py > y + h - r -> y + h - r
                else -> py
            }
            val dx = px - cx
            val dy = py - cy
            return (dx * dx + dy * dy) <= (r * r)
        }

        fun insideCircle(px: Double, py: Double, cx: Double, cy: Double, r: Double): Boolean {
            val dx = px - cx
            val dy = py - cy
            return (dx * dx + dy * dy) <= (r * r)
        }

        val scale = 1024.0 / 108.0
        for (y in 0 until height) {
            rawRows.write(0) // PNG filter None
            val vy = y / scale
            for (x in 0 until width) {
                val vx = x / scale
                // Vychozi svetle pozadi #F8F9FA
                var rgb = 0xF8F9FA

                // Jemny stin pod deskou
                if (insideRoundedRect(vx, vy, 29.0, 31.0, 50.0, 50.0, 6.0)) {
                    rgb = 0xD2D6DC
                }

                // Hlavni tmavy podklad mrizky #1E293B
                if (insideRoundedRect(vx, vy, 29.0, 29.0, 50.0, 50.0, 6.0)) {
                    rgb = 0x1E293B
                }

                // Radek 1
                if (insideRoundedRect(vx, vy, 31.0, 31.0, 14.0, 14.0, 2.0)) rgb = 0xD1FAE5
                if (insideRoundedRect(vx, vy, 47.0, 31.0, 14.0, 14.0, 2.0)) rgb = 0xFFFFFF
                if (insideRoundedRect(vx, vy, 63.0, 31.0, 14.0, 14.0, 2.0)) rgb = 0xFEF3C7

                // Radek 2
                if (insideRoundedRect(vx, vy, 31.0, 47.0, 14.0, 14.0, 2.0)) rgb = 0xFFFFFF
                if (insideRoundedRect(vx, vy, 47.0, 47.0, 14.0, 14.0, 2.0)) rgb = 0xDBEAFE
                if (insideRoundedRect(vx, vy, 63.0, 47.0, 14.0, 14.0, 2.0)) rgb = 0xFFFFFF

                // Radek 3
                if (insideRoundedRect(vx, vy, 31.0, 63.0, 14.0, 14.0, 2.0)) rgb = 0xFEF3C7
                if (insideRoundedRect(vx, vy, 47.0, 63.0, 14.0, 14.0, 2.0)) rgb = 0xFFFFFF
                if (insideRoundedRect(vx, vy, 63.0, 63.0, 14.0, 14.0, 2.0)) rgb = 0xFEE2E2

                // Jigsaw puzzle vyrezy na cervene dlazdici
                if (insideCircle(vx, vy, 63.0, 70.0, 3.0)) rgb = 0xFEE2E2
                if (insideCircle(vx, vy, 70.0, 63.0, 3.0)) rgb = 0x1E293B

                rawRows.write((rgb shr 16) and 0xFF)
                rawRows.write((rgb shr 8) and 0xFF)
                rawRows.write(rgb and 0xFF)
            }
        }

        val compressedData = ByteArrayOutputStream()
        DeflaterOutputStream(compressedData, Deflater(Deflater.DEFAULT_COMPRESSION)).use {
            it.write(rawRows.toByteArray())
        }

        val pngBytes = ByteArrayOutputStream()
        val dataOut = DataOutputStream(pngBytes)
        dataOut.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        fun writePngChunk(chunkType: String, payload: ByteArray) {
            dataOut.writeInt(payload.size)
            val typeBytes = chunkType.toByteArray(Charsets.US_ASCII)
            dataOut.write(typeBytes)
            dataOut.write(payload)
            val crc = CRC32()
            crc.update(typeBytes)
            crc.update(payload)
            dataOut.writeInt(crc.value.toInt())
        }

        val ihdrBuf = ByteArrayOutputStream()
        DataOutputStream(ihdrBuf).apply {
            writeInt(width)
            writeInt(height)
            writeByte(8) // 8 bitu na kanal
            writeByte(2) // colorType = 2 (Truecolor RGB bez alfy)
            writeByte(0)
            writeByte(0)
            writeByte(0)
        }
        writePngChunk("IHDR", ihdrBuf.toByteArray())
        writePngChunk("IDAT", compressedData.toByteArray())
        writePngChunk("IEND", ByteArray(0))

        iconFile.writeBytes(pngBytes.toByteArray())
    }
}

tasks.matching { it.name.startsWith("link") && it.name.contains("FrameworkIos") }.configureEach {
    dependsOn(generateIosIcon)
}
