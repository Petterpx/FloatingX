plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.system"
}

dependencies {
    api(project(":floatingx-core"))
    // WindowInsetsCompat：把 WindowMetrics 的屏幕级 insets 换算成 safe area
    implementation(libs.androidx.core)
    implementation(libs.kotlin.stdlib)
}
