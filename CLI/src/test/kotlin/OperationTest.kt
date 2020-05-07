package com.sd.hw

import com.sd.hw.Environment.Companion.CURRENT_DIRECTORY
import com.sd.hw.Environment.Companion.HOME_DIRECTORY
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.IOException

internal class OperationTest {
    private var environment = Environment()

    @BeforeEach
    fun resetEnvironment() {
        environment = Environment()
    }

    @Test
    fun pwdSimpleTest() {
        val pwd = Pwd(environment)
        val result = pwd.run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertTrue(pathsEqual(environment.resolveVariable(CURRENT_DIRECTORY), result.textResult))
    }

    @Test
    fun pwdArgumentsIgnoreTest() {
        val pwd = Pwd(environment)
        val result = pwd.withArgs(listOf("aaa", "bbb", "a")).run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertTrue(pathsEqual(environment.resolveVariable(CURRENT_DIRECTORY), result.textResult))
    }

    @Test
    fun pwdAdditionalInputIgnoreTest() {
        val pwd = Pwd(environment)
        val result = pwd.run("kek")
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertTrue(pathsEqual(environment.resolveVariable(CURRENT_DIRECTORY), result.textResult))
    }

    @Test
    fun echoSimpleTest() {
        val echo = Echo(environment)
        val result = echo.run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("\n", result.textResult)
    }

    @Test
    fun echoSeveralArgsTest() {
        val echo = Echo(environment)
        val result = echo.withArgs(listOf("a", "a a", "c", "d   d")).run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("a a a c d   d\n", result.textResult)
    }

    @Test
    fun echoAdditionalInputIgnoreTest() {
        val echo = Echo(environment)
        val result = echo.withArgs(listOf("a")).run("ddddddddddddddddd")
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("a\n", result.textResult)
    }

    @Test
    fun wcSimpleTest() {
        val wc = WC(environment)
        val result = wc.run()
        assertEquals(ExecutionState.ERRORED, result.isInterrupted)
    }

    @Test
    fun wcFileTest() {
        val wc = WC(environment)
        val file = File("kek")
        file.createNewFile()
        file.writeText("a a")
        val result = wc.withArgs(listOf("kek")).run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("1 2 3", result.textResult)
    }

    @Test
    fun wcNonexistentFileTest() {
        val wc = WC(environment)
        val result = wc.withArgs(listOf("asf")).run()
        assertEquals(ExecutionState.ERRORED, result.isInterrupted)
    }

    @Test
    fun wcExtraArgsTest() {
        val wc = WC(environment)
        val file = File("kek")
        file.createNewFile()
        file.writeText("a a")
        file.deleteOnExit()
        val result = wc.withArgs(listOf("kek", "a", "b", "c")).run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("1 2 3", result.textResult)
    }

    @Test
    fun wcWithAdditionalInputTest() {
        val wc = WC(environment)
        val result = wc.run("a a")
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("1 2 3", result.textResult)
    }

    @Test
    fun catExistingFileTest() {
        val cat = Cat(environment)
        val file = File("kek")
        file.createNewFile()
        file.writeText("a\na\na a a b \nc")
        file.deleteOnExit()

        val result = cat.withArgs(listOf("kek")).run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("a\na\na a a b \nc", result.textResult)
    }

    @Test
    fun catNonexistentFileTest() {
        val cat = Cat(environment)

        val result = cat.withArgs(listOf("kek12")).run()
        assertEquals(ExecutionState.ERRORED, result.isInterrupted)
    }

    @Test
    fun catAdditionalInputTest() {
        val cat = Cat(environment)
        val file = File("kek")
        file.createNewFile()
        file.writeText("a\na\na a a b \nc")
        file.deleteOnExit()

        val result = cat.run("kek")
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("a\na\na a a b \nc", result.textResult)
    }

    @Test
    fun exitSimpleTest() {
        val exit = Exit(environment)
        val result = exit.run()
        assertEquals(ExecutionState.FINISHED, result.isInterrupted)
        assertEquals("", result.textResult)
    }

    @Test
    fun exitArgsIgnoreTest() {
        val exit = Exit(environment)
        val result = exit.withArgs(listOf("a", "b", "c")).run()
        assertEquals(ExecutionState.FINISHED, result.isInterrupted)
        assertEquals("", result.textResult)
    }

    @Test
    fun exitAdditionalInputIgnoreTest() {
        val exit = Exit(environment)
        val result = exit.run("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        assertEquals(ExecutionState.FINISHED, result.isInterrupted)
        assertEquals("", result.textResult)
    }

    @Test
    fun simpleAssociationTest() {
        val association = Association(environment)
        val result = association.withArgs(listOf("a", "10")).run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("", result.textResult)
        assertEquals("10", environment.resolveVariable("a"))
    }

    @Test
    fun updateValueAssociationTest() {
        Association(environment).withArgs(listOf("a", "10")).run()
        val result = Association(environment).withArgs(listOf("a", "40 40")).run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("", result.textResult)
        assertEquals("40 40", environment.resolveVariable("a"))
    }

    @Test
    fun ignoreAdditionalInputAssociationTest() {
        val association = Association(environment)
        val result = association.withArgs(listOf("a", "10")).run("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa aaaa")
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("", result.textResult)
        assertEquals("10", environment.resolveVariable("a"))
    }

    @Test
    fun runProcessSimpleTest() {
        val runProcess = RunProcess(environment)
        val result = runProcess.withArgs(listOf("echo", "1")).run()
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("1", result.textResult)
    }

    @Test
    fun runProcessStderrTest() {
        val runProcess = RunProcess(environment)
        val result = runProcess.withArgs(listOf("cat", "1")).run()
        assertEquals(ExecutionState.ERRORED, result.isInterrupted)
        assertTrue(result.textResult.contains("No such file or directory"))
    }

    @Test
    fun runProcessUnknownCommandTest() {
        val runProcess = RunProcess(environment)
        assertThrows<IOException> { runProcess.withArgs(listOf("icho", "1")).run() }
    }

    @Test
    fun runCd() {
        val cd = Cd(environment)
        val pwd = Pwd(environment)
        cd.withArgs(listOf("src")).run()
        val result = pwd.withArgs(listOf()).run()
        assertEquals(true, result.textResult.contains("${File.separator}src"))
    }

    @Test
    fun runCdWithPoints() {
        val cd = Cd(environment)
        val pwd = Pwd(environment)

        val dir1 = pwd.withArgs(listOf()).run().textResult
        cd.withArgs(listOf("src")).run()
        cd.withArgs(listOf("..")).run()
        val dir2 = pwd.withArgs(listOf()).run().textResult

        assertTrue(pathsEqual(dir1, dir2))
    }

    @Test
    fun runCdWithoutArg() {
        val cd = Cd(environment)
        val pwd = Pwd(environment)
        cd.withArgs(listOf()).run()
        val result = pwd.withArgs(listOf()).run()
        assertTrue(pathsEqual(environment.resolveVariable(HOME_DIRECTORY), result.textResult))
    }

    @Test
    fun runLsWithArg() {
        val ls = Ls(environment)
        val result = ls.withArgs(listOf("src")).run().textResult

        assertTrue(result.contains("main"))
        assertTrue(result.contains("test"))
    }

    @Test
    fun runLsWithoutArg() {
        val ls = Ls(environment)
        val result = ls.withArgs(listOf()).run().textResult

        assertTrue(result.contains("src"))
        assertTrue(result.contains("build.gradle"))
    }

    @Test
    fun runCdWithCat() {
        val cat = Cat(environment)
        val cd = Cd(environment)

        val result1 = cat.withArgs(listOf("src/main/kotlin/Main.kt")).run().textResult
        cd.withArgs(listOf("src/main/kotlin")).run()
        val result2 = cat.withArgs(listOf("Main.kt")).run().textResult

        assertEquals(result1, result2)
    }

    @Test
    fun runCdWithLs() {
        val ls = Ls(environment)
        val cd = Cd(environment)

        val result1 = ls.withArgs(listOf("src/main/kotlin")).run().textResult
        cd.withArgs(listOf("src/main/kotlin")).run()
        val result2 = ls.withArgs(listOf()).run().textResult

        assertEquals(result1, result2)
    }

    @Test
    fun cdToFile() {
        val cd = Cd(environment)
        val result = cd.withArgs(listOf("src/main/kotlin/Main.kt")).run()
        assertEquals(result.isInterrupted, ExecutionState.ERRORED)
    }

    private fun pathsEqual(path1: String, path2: String): Boolean {
        var path1Start = 0
        var path2Start = 0

        if (path1[0] == File.separatorChar) {
            path1Start = 1
        }

        if (path2[0] == File.separatorChar) {
            path2Start = 1
        }

        return path1.substring(path1Start) == path2.substring(path2Start)
    }
}