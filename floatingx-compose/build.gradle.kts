plugins {
    id("floatingx.library")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.petterp.floatingx.compose"
    // compose 1.11 / lifecycle 2.10 的 AAR 要求 minSdk 23（spec §1.1）
    defaultConfig.minSdk = 23
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":floatingx-core"))
    // 公开 API 带 @Composable 与 CompositionLocal，必须 api 暴露
    api(libs.compose.ui.versioned)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.savedstate)
    implementation(libs.coroutines.android)
    implementation(libs.kotlin.stdlib)

    // 测试里的组合内容只用 foundation 的 Box/size
    testImplementation("androidx.compose.foundation:foundation:1.11.4")
    // 只为断言组合里的 LocalViewModelStoreOwner；产物不依赖它
    testImplementation("androidx.lifecycle:lifecycle-viewmodel-compose:${libs.versions.lifecycle.get()}")
}
