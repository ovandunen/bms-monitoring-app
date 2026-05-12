import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader(Charsets.UTF_8)?.use { load(it) }
}
val mapTilerKey: String = localProperties.getProperty("maptiler.key") ?: ""

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

kotlin {
    jvmToolchain(17)

    androidTarget()

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.materialIconsExtended)
        }
        androidMain.dependencies {
            implementation(libs.maplibre.compose.android)
            implementation(libs.geckoview)
            implementation(libs.activity.compose)
            api(libs.media3.exoplayer)
            api(libs.media3.exoplayer.hls)
            api(libs.media3.session)
            api(libs.media3.ui)
            implementation(libs.glide)
            implementation(libs.androidx.media)
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.fleet.ecocar"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("src/androidMain/consumer-rules.pro")
        buildConfigField("String", "MAPTILER_KEY", "\"${mapTilerKey.escapeForBuildConfig()}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

compose.desktop {
    application {
        mainClass = "com.fleet.ecocar.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "EcoCar GUI"
            packageVersion = "1.0.0"
        }
    }
}
