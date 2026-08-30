package com.petterp.floatingx.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** app 模块只依赖 core + androidx.core（spec §1） */
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
    fun `app main sources import no forbidden packages`() {
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
        assertTrue("app 违反依赖边界：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
