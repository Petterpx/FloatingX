import com.android.build.api.dsl.LibraryExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * FloatingX 库模块统一约定：
 * - minSdk 21 / compileSdk 36 / Java 17
 * - explicitApi
 * - maven-publish 坐标 io.github.petterpx:<module-name>
 * - Robolectric 单测配置
 */
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
    defaultConfig {
        minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = false
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

extensions.configure<KotlinAndroidProjectExtension> {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

extensions.configure<MavenPublishBaseExtension> {
    val versionName = findProperty("versionName")?.toString()?.takeIf { it.isNotBlank() } ?: "3.0.0-SNAPSHOT"
    val isPublish = findProperty("isPublish")?.toString()?.toBoolean() ?: false
    if (isPublish) signAllPublications()
    coordinates("io.github.petterpx", project.name, versionName)
}

dependencies {
    "testImplementation"(libs.findLibrary("junit").get())
    "testImplementation"(libs.findLibrary("robolectric").get())
    "testImplementation"(libs.findLibrary("androidx-test-core").get())
    "androidTestImplementation"(libs.findLibrary("androidx-test-ext-junit").get())
    "androidTestImplementation"(libs.findLibrary("androidx-test-runner").get())
}
