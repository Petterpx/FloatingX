package com.petterp.floatingx.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** core 不得依赖任何平台专属包（spec §1）。Gradle 单测的 working dir 是模块目录 */
class DependencyBoundaryTest {

    private val forbidden = listOf(
        "android.view.WindowManager",
        "androidx.fragment",
        "androidx.compose",
        "androidx.lifecycle",
        "androidx.appcompat",
        "androidx.savedstate",
        "kotlinx.coroutines",
    )

    @Test
    fun `core main sources import no platform specific packages`() {
        val root = File("src/main")
        assertTrue("找不到 src/main，当前目录 ${File(".").absolutePath}", root.exists())
        val violations = root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    val imported = trimmed.removePrefix("import ").trim()
                    if (trimmed.startsWith("import ") && forbidden.any { imported.startsWith(it) }) {
                        "${file.relativeTo(root)}:${index + 1}: $trimmed"
                    } else null
                }
            }
            .toList()
        assertTrue("core 不允许依赖平台专属包：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
