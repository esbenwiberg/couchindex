import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private fun String.toBuildConfigLiteral(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.couchindex.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.couchindex.app"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField(
            type = "String",
            name = "TMDB_READ_ACCESS_TOKEN",
            value = localProperties.getProperty("TMDB_READ_ACCESS_TOKEN", "").toBuildConfigLiteral(),
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin.compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
