package com.petterp.floatingx.compose

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** compose 模块可以依赖 compose / lifecycle / savedstate / coroutines，但不得碰 fragment / appcompat / WindowManager */
class DependencyBoundaryTest {

    private val forbidden = listOf("androidx.fragment", "androidx.appcompat", "android.view.WindowManager")

    @Test
    fun `compose main sources import no forbidden packages`() {
        val root = File("src/main")
        assertTrue("找不到 src/main，当前目录 ${File(".").absolutePath}", root.exists())
        val violations = root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null
                    val imported = trimmed.removePrefix("import ").trim()
                    if (forbidden.any { imported.startsWith(it) }) "${file.relativeTo(root)}:${index + 1}: $trimmed" else null
                }
            }
            .toList()
        assertTrue("compose 违反依赖边界：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
