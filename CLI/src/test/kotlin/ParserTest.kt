import com.sd.hw.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ParserTest {
    private val environment = Environment()
    private val parser = Parser(OperationFactory(environment), environment)


    @Test
    fun oneCommandSplitBySeparatorsTest() {
        val result = parser.splitInputBySeparators("a a")
        assertEquals(listOf("a", "a"), result)
    }

    @Test
    fun argInQuotesSplitBySeparatorsTest() {
        val result = parser.splitInputBySeparators("a 'a a'")
        assertEquals(listOf("a", "'a a'"), result)
    }

    @Test
    fun argInDoubleQuotesSplitBySeparatorsTest() {
        val result = parser.splitInputBySeparators("a \"a a a\"")
        assertEquals(listOf("a", "\"a a a\""), result)
    }

    @Test
    fun processInBracketsSplitBySeparatorsTest() {
        val result = parser.splitInputBySeparators("$(a a a a)")
        assertEquals(listOf("!$", "a", "a", "a", "a"), result)
    }

    @Test
    fun simpleStringResolveQuotesAndVariablesTest() {
        assertEquals("aaaa", parser.resolveQuotesAndVariables("aaaa"))
    }

    @Test
    fun stringWithQuotesResolveQuotesAndVariablesTest() {
        assertEquals("aaaa", parser.resolveQuotesAndVariables("'aaaa'"))
    }

    @Test
    fun stringWithVariableInQuotesResolveQuotesAndVariablesTest() {
        environment.addVariable("kek", "aaa")
        assertEquals("aaa \$kek", parser.resolveQuotesAndVariables("'aaa \$kek'"))
    }

    @Test
    fun stringWithDoubleQuotesResolveQuotesAndVariablesTest() {
        environment.addVariable("kek", "aaa")
        assertEquals("aaa aaa", parser.resolveQuotesAndVariables("\"aaa \$kek\""))
    }

    @Test
    fun oneCommandSeparateByPipesTest() {
        val commands = listOf(CommandWithArgs("a", mutableListOf("a")))
        val strList = listOf("a", "a")
        assertEquals(commands, parser.separateByPipes(strList))
    }

    @Test
    fun severalCommandsSeparateByPipesTest() {
        val commands = listOf(
            CommandWithArgs("a", mutableListOf("a")),
            CommandWithArgs("b", mutableListOf("c", "c")),
            CommandWithArgs("d", mutableListOf())
        )
        val strList = listOf("a", "a", "|", "b", "c", "c", "|", "d")
        assertEquals(commands, parser.separateByPipes(strList))
    }

    @Test
    fun argsCountInMapNamesToOperationsTest() {
        val commands = listOf(
            CommandWithArgs("echo", mutableListOf("a", "b", "c"))
        )
        val result = parser.mapNamesToOperations(commands)
        assertEquals(1, result.size)
        assertTrue(result[0] is Echo)
        assertEquals(listOf("a", "b", "c"), result[0]?.args)
    }

    @Test
    fun severalCommandsMapNamesToOperationsTest() {
        val commands = listOf(
            CommandWithArgs("echo", mutableListOf("a", "b", "c")),
            CommandWithArgs("cat", mutableListOf("a")),
            CommandWithArgs("pwd", mutableListOf())
        )
        val result = parser.mapNamesToOperations(commands)
        assertEquals(3, result.size)
        assertTrue(result[0] is Echo)
        assertEquals(listOf("a", "b", "c"), result[0]?.args)
        assertTrue(result[1] is Cat)
        assertEquals(listOf("a"), result[1]?.args)
        assertTrue(result[2] is Pwd)
        assertEquals(emptyList<String>(), result[2]?.args)
    }
}