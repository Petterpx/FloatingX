plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.core"
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.kotlin.stdlib)
}
