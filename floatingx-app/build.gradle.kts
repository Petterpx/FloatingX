plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.app"
}

dependencies {
    api(project(":floatingx-core"))
    // ViewCompat.getRootWindowInsets / WindowInsetsCompat：safe area insets
    implementation(libs.androidx.core)
    implementation(libs.kotlin.stdlib)
}
