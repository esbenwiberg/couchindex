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

private fun releaseCredential(name: String): String? =
    providers.environmentVariable(name).orNull
        ?.takeIf(String::isNotBlank)
        ?: localProperties.getProperty(name)?.takeIf(String::isNotBlank)

val releaseSigningCredentials = mapOf(
    "storeFile" to releaseCredential("COUCHINDEX_UPLOAD_STORE_FILE"),
    "storePassword" to releaseCredential("COUCHINDEX_UPLOAD_STORE_PASSWORD"),
    "keyAlias" to releaseCredential("COUCHINDEX_UPLOAD_KEY_ALIAS"),
    "keyPassword" to releaseCredential("COUCHINDEX_UPLOAD_KEY_PASSWORD"),
)
val configuredReleaseCredentialCount = releaseSigningCredentials.values.count { it != null }
check(configuredReleaseCredentialCount == 0 || configuredReleaseCredentialCount == releaseSigningCredentials.size) {
    "Release signing is partially configured. Supply all COUCHINDEX_UPLOAD_* values or none."
}
val releaseSigningConfigured = configuredReleaseCredentialCount == releaseSigningCredentials.size

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

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(checkNotNull(releaseSigningCredentials["storeFile"]))
                storePassword = releaseSigningCredentials["storePassword"]
                keyAlias = releaseSigningCredentials["keyAlias"]
                keyPassword = releaseSigningCredentials["keyPassword"]
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.register("releaseSigningStatus") {
    group = "verification"
    description = "Reports whether upload-key signing is configured without printing credentials."
    doLast {
        println(if (releaseSigningConfigured) "Release signing configured." else "Release signing not configured.")
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
    implementation(libs.androidx.tvprovider)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}
