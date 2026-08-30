plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.scope"
}

dependencies {
    api(project(":floatingx-core"))
    implementation(libs.kotlin.stdlib)
    // Fragment 支持是可选的：只有调用 Fragment.fxScope / FragmentHost 的使用方才需要自己依赖 androidx.fragment
    compileOnly(libs.fragment)

    testImplementation(libs.fragment)
}
