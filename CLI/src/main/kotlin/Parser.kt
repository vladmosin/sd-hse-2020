package com.sd.hw

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Class for storing command name and grouped argument together.
 */
internal data class CommandWithArgs(val commandName: String, val args: List<String> = mutableListOf())

/**
 * Class for console input parsing.
 */
class Parser(private val operationFactory: OperationFactory, private val environment: Environment) {
    /**
     * Translate one line console input to sequence of Operation instances.
     */
    fun parse(input: String): List<Operation?> {
        val inputString = splitInputBySeparators(input)
        val args = inputString.map(this::resolveQuotesAndVariables)
        val commandsWithArgs = separateByPipes(args)

        return mapNamesToOperations(commandsWithArgs)
    }

    internal fun splitInputBySeparators(input: String): List<String> {
        val matcher: Matcher = Pattern.compile("(([^(\"')]\\S*|\".+?\"|'.+?')\\s*)")
                    .matcher(replaceBracketsInRunProcess(input))
        val list = mutableListOf<String>()
        while (matcher.find()) {
            list.add(matcher.group().trim())
        }
        return list
    }

    private fun replaceBracketsInRunProcess(input: String): String {
        val regex = "\\$\\((.*?)\\)".toRegex()
        return regex.replace(input) {
            val text = it.value
            "!$ " + text.drop(2).dropLast(1) + " "
        }
    }

    internal fun resolveQuotesAndVariables(str: String): String {
        return if (str.first() == '"' && str.last() == '"')
            replaceAllVariables(str.drop(1).dropLast(1))
        else if (str.first() == '\'' && str.last() == '\'')
            str.drop(1).dropLast(1)
        else
            replaceAllVariables(str)
    }

    internal fun separateByPipes(args: List<String>) : List<CommandWithArgs> {
        val commandsList = mutableListOf<CommandWithArgs>()
        var isNewCommandStarted = true
        val groupsList = mutableListOf<MutableList<String>>()
        for (element in args) {
            when {
                isNewCommandStarted -> {
                    groupsList.add(mutableListOf(element))
                    isNewCommandStarted = false
                }
                element == "|" -> {
                    isNewCommandStarted = true
                }
                else -> {
                    groupsList.last().add(element)
                }
            }
        }

        for (group in groupsList) {
            if (group.size >= 2 && group[1] == "=") {
                commandsList.add(CommandWithArgs("=", listOf(group[0], group[2])))
            } else if (group.isNotEmpty()) {
                commandsList.add(CommandWithArgs(group[0], group.drop(1)))
            }
        }
        return commandsList
    }

    internal fun mapNamesToOperations(commandsWithArgs: List<CommandWithArgs>): List<Operation?> {
        return commandsWithArgs.map {
            val command = operationFactory.getOperationByName(it.commandName)
            command?.withArgs(it.args)
        }
    }

    private fun replaceAllVariables(input: String): String {
        return input.split(" ").joinToString(" ") {
            if (it.isNotEmpty() && it[0] == '$') environment.resolveVariable(it.drop(1))
            else it
        }
    }

}