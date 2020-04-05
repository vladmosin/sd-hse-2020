package com.sd.hw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

internal class EnvironmentTest {
    private val environment = Environment()

    @Test
    fun getNonexistentVariableTest() {
        assertEquals("", environment.resolveVariable("aaaaaa"))
    }

    @Test
    fun getVariableTest() {
        environment.addVariable("aaa", "123")
        assertEquals("123", environment.resolveVariable("aaa"))
    }

    @Test
    fun rewriteVariableTest() {
        environment.addVariable("aaa", "234")
        environment.addVariable("aaa", "23")
        assertEquals("23", environment.resolveVariable("aaa"))
    }

    @Test
    fun oneCommandExecuteTest() {
        val result = environment.execute("echo 1")
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("1\n", result.textResult)
    }

    @Test
    fun unknownCommandExecuteTest() {
        val result = environment.execute("eke a a a a")
        assertEquals(ExecutionState.ERRORED, result.isInterrupted)
        assertEquals(Environment.PARSING_ERROR_MESSAGE, result.textResult)
    }

    @Test
    fun exitCommandExecuteTest() {
        val result = environment.execute("exit")
        assertEquals(ExecutionState.FINISHED, result.isInterrupted)
        assertEquals("", result.textResult)
    }

    @Test
    fun commandSequenceExecuteTest() {
        val result = environment.execute("echo a a a a | wc")
        assertEquals(ExecutionState.WORKING, result.isInterrupted)
        assertEquals("2 4 8", result.textResult)
    }

    @Test
    fun simpleResolveFileTest() {
        val file = File("kek")
        file.createNewFile()
        file.writeText("a\nb")
        file.deleteOnExit()

        assertEquals("a\nb", environment.resolveFile("kek"))
    }

    @Test
    fun nonexistentFileResolveFileTest() {
        File("kek").delete()
        assertNull(environment.resolveFile("kek"))
    }
}