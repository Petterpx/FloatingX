plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.system"
}

dependencies {
    api(project(":floatingx-core"))
    // WindowInsetsCompat / ViewCompat：系统窗口的 safe area
    implementation(libs.androidx.core)
    implementation(libs.kotlin.stdlib)
}
