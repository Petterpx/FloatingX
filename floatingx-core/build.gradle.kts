plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.core"
}

dependencies {
    // 公开 API 上出现了 @IdRes/@LayoutRes 等注解，必须 api 暴露给使用方
    api(libs.androidx.annotation)
    implementation(libs.androidx.core)
    implementation(libs.kotlin.stdlib)
}
