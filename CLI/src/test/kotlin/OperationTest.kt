package com.sd.hw

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.IOException

internal class OperationTest {
    private val environment = Environment()

    @Test
    fun pwdSimpleTest() {
        val pwd = Pwd(environment)
        val result = pwd.run()
        assertEquals(false, result.isInterrupted)
        assertEquals(File(".").absolutePath, result.textResult)
    }

    @Test
    fun pwdArgumentsIgnoreTest() {
        val pwd = Pwd(environment)
        val result = pwd.withArgs(listOf("aaa", "bbb", "a")).run()
        assertEquals(false, result.isInterrupted)
        assertEquals(File(".").absolutePath, result.textResult)
    }

    @Test
    fun pwdAdditionalInputIgnoreTest() {
        val pwd = Pwd(environment)
        val result = pwd.run("kek")
        assertEquals(false, result.isInterrupted)
        assertEquals(File(".").absolutePath, result.textResult)
    }

    @Test
    fun echoSimpleTest() {
        val echo = Echo(environment)
        val result = echo.run()
        assertEquals(false, result.isInterrupted)
        assertEquals("\n", result.textResult)
    }

    @Test
    fun echoSeveralArgsTest() {
        val echo = Echo(environment)
        val result = echo.withArgs(listOf("a", "a a", "c", "d   d")).run()
        assertEquals(false, result.isInterrupted)
        assertEquals("a a a c d   d\n", result.textResult)
    }

    @Test
    fun echoAdditionalInputIgnoreTest() {
        val echo = Echo(environment)
        val result = echo.withArgs(listOf("a")).run("ddddddddddddddddd")
        assertEquals(false, result.isInterrupted)
        assertEquals("a\n", result.textResult)
    }

    @Test
    fun wcSimpleTest() {
        val wc = WC(environment)
        val result = wc.run()
        assertEquals(true, result.isInterrupted)
    }

    @Test
    fun wcFileTest() {
        val wc = WC(environment)
        val file = File("kek")
        file.createNewFile()
        file.writeText("a a")
        val result = wc.withArgs(listOf("kek")).run()
        assertEquals(false, result.isInterrupted)
        assertEquals("1 2 3", result.textResult)
    }

    @Test
    fun wcNonexistentFileTest() {
        val wc = WC(environment)
        val result = wc.withArgs(listOf("asf")).run()
        assertEquals(true, result.isInterrupted)
    }

    @Test
    fun wcExtraArgsTest() {
        val wc = WC(environment)
        val file = File("kek")
        file.createNewFile()
        file.writeText("a a")
        file.deleteOnExit()
        val result = wc.withArgs(listOf("kek", "a", "b", "c")).run()
        assertEquals(false, result.isInterrupted)
        assertEquals("1 2 3", result.textResult)
    }

    @Test
    fun wcWithAdditionalInputTest() {
        val wc = WC(environment)
        val result = wc.run("a a")
        assertEquals(false, result.isInterrupted)
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
        assertEquals(false, result.isInterrupted)
        assertEquals("a\na\na a a b \nc", result.textResult)
    }

    @Test
    fun catNonexistentFileTest() {
        val cat = Cat(environment)

        val result = cat.withArgs(listOf("kek12")).run()
        assertEquals(true, result.isInterrupted)
    }

    @Test
    fun catAdditionalInputTest() {
        val cat = Cat(environment)
        val file = File("kek")
        file.createNewFile()
        file.writeText("a\na\na a a b \nc")
        file.deleteOnExit()

        val result = cat.run("kek")
        assertEquals(false, result.isInterrupted)
        assertEquals("a\na\na a a b \nc", result.textResult)
    }

    @Test
    fun exitSimpleTest() {
        val exit = Exit(environment)
        val result = exit.run()
        assertEquals(true, result.isInterrupted)
        assertEquals("", result.textResult)
    }

    @Test
    fun exitArgsIgnoreTest() {
        val exit = Exit(environment)
        val result = exit.withArgs(listOf("a", "b", "c")).run()
        assertEquals(true, result.isInterrupted)
        assertEquals("", result.textResult)
    }

    @Test
    fun exitAdditionalInputIgnoreTest() {
        val exit = Exit(environment)
        val result = exit.run("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        assertEquals(true, result.isInterrupted)
        assertEquals("", result.textResult)
    }

    @Test
    fun simpleAssociationTest() {
        val association = Association(environment)
        val result = association.withArgs(listOf("a", "10")).run()
        assertEquals(false, result.isInterrupted)
        assertEquals("", result.textResult)
        assertEquals("10", environment.resolveVariable("a"))
    }

    @Test
    fun updateValueAssociationTest() {
        Association(environment).withArgs(listOf("a", "10")).run()
        val result = Association(environment).withArgs(listOf("a", "40 40")).run()
        assertEquals(false, result.isInterrupted)
        assertEquals("", result.textResult)
        assertEquals("40 40", environment.resolveVariable("a"))
    }

    @Test
    fun ignoreAdditionalInputAssociationTest() {
        val association = Association(environment)
        val result = association.withArgs(listOf("a", "10")).run("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa aaaa")
        assertEquals(false, result.isInterrupted)
        assertEquals("", result.textResult)
        assertEquals("10", environment.resolveVariable("a"))
    }

    @Test
    fun runProcessSimpleTest() {
        val runProcess = RunProcess(environment)
        val result = runProcess.withArgs(listOf("echo", "1")).run()
        assertEquals(false, result.isInterrupted)
        assertEquals("1", result.textResult)
    }

    @Test
    fun runProcessStderrTest() {
        val runProcess = RunProcess(environment)
        val result = runProcess.withArgs(listOf("cat", "1")).run()
        assertEquals(true, result.isInterrupted)
        assertTrue(result.textResult.contains("No such file or directory"))
    }

    @Test
    fun runProcessUnknownCommandTest() {
        val runProcess = RunProcess(environment)
        assertThrows<IOException> { runProcess.withArgs(listOf("icho", "1")).run() }
    }
}