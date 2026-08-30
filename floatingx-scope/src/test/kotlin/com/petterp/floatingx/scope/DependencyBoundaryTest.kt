package com.petterp.floatingx.scope

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * scope 模块的依赖边界（spec §1 + Plan 2 Global Constraints）：
 * fragment / lifecycle 只允许出现在 FragmentHost.kt 与 FxFragmentScope.kt。
 */
class DependencyBoundaryTest {

    private val forbiddenEverywhere = listOf(
        "android.view.WindowManager",
        "androidx.compose",
        "androidx.appcompat",
        "androidx.savedstate",
        "kotlinx.coroutines",
    )
    private val fragmentOnly = listOf("androidx.fragment", "androidx.lifecycle")
    private val fragmentFiles = setOf("FragmentHost.kt", "FxFragmentScope.kt")

    @Test
    fun `scope main sources respect dependency boundary`() {
        val root = File("src/main")
        assertTrue("找不到 src/main，当前目录 ${File(".").absolutePath}", root.exists())
        val violations = root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null
                    val imported = trimmed.removePrefix("import ").trim()
                    val bad = forbiddenEverywhere.any { imported.startsWith(it) } ||
                        (file.name !in fragmentFiles && fragmentOnly.any { imported.startsWith(it) })
                    if (bad) "${file.relativeTo(root)}:${index + 1}: $trimmed" else null
                }
            }
            .toList()
        assertTrue("scope 违反依赖边界：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
