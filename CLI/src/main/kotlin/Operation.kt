package com.sd.hw

import com.sd.hw.Environment.Companion.CURRENT_DIRECTORY
import com.sd.hw.Environment.Companion.HOME_DIRECTORY
import java.io.File

/**
 * Parent class for all bash commands.
 * @param environment environment for command execution
 */
abstract class Operation(val environment: Environment) {
    var args = mutableListOf<String>()

    /**
     * Add execution arguments.
     */
    open fun withArgs(values: List<String>): Operation {
        args = values.toMutableList()
        return this
    }

    /**
     * Run operation with saved arguments and optional input from pipe.
     */
    abstract fun run(additionalInput: String? = null): ExecutionResult
}

/**
 * Class for wc bash command partial simulation.
 */
class WC(environment: Environment) : Operation(environment) {
    /**
     * Returns number of lines, words and bytes.
     */
    override fun run(additionalInput: String?): ExecutionResult {
        val fileText = (if (args.isNotEmpty()) environment.resolveFile(args[0]) else additionalInput)
            ?: return ExecutionResult(ExecutionState.ERRORED, "Error: invalid wc args")
        val lines = fileText.lines().size
        val words = fileText.trim().split("\\s".toRegex()).size
        val bytes = fileText.length
        return ExecutionResult(ExecutionState.WORKING, "$lines $words $bytes")
    }
}

/**
 * Class for echo bash command partial simulation.
 */
class Echo(environment: Environment) : Operation(environment) {
    /**
     * Returns given arguments with spaces and new line in the end.
     */
    override fun run(additionalInput: String?): ExecutionResult {
        return ExecutionResult(ExecutionState.WORKING, args.joinToString(" ") + '\n')
    }
}

/**
 * Class for pwd bash command partial simulation.
 */
class Pwd(environment: Environment) : Operation(environment) {
    /**
     * Returns absolute path for . directory.
     */
    override fun run(additionalInput: String?): ExecutionResult {
        return ExecutionResult(ExecutionState.WORKING, environment.resolveVariable(CURRENT_DIRECTORY))
    }
}

/**
 * Class for cat bash command partial simulation.
 */
class Cat(environment: Environment) : Operation(environment) {
    /**
     * Returns text of file given in the arguments or additional input.
     */
    override fun run(additionalInput: String?): ExecutionResult {
        var text = if (args.isNotEmpty()) args[0] else additionalInput
        text ?: return ExecutionResult(ExecutionState.ERRORED)
        text = pathToStandard(environment.resolveVariable(CURRENT_DIRECTORY) + File.separator + text)

        text ?: return ExecutionResult(ExecutionState.ERRORED, "No file found")

        val result = environment.resolveFile(text)
            ?: return ExecutionResult(ExecutionState.ERRORED, "No file named $text found")
        return ExecutionResult(ExecutionState.WORKING, result)
    }
}

/**
 * Class for exit bash command partial simulation.
 */
class Exit(environment: Environment) : Operation(environment) {
    /**
     * Returns interrupting ExecutionResult.
     */
    override fun run(additionalInput: String?): ExecutionResult {
        return ExecutionResult(ExecutionState.FINISHED)
    }
}

/**
 * Class for cd bash command partial simulation.
 * */
class Cd(environment: Environment) : Operation(environment) {
    /**
     * Returns list of files and directories
     */
    override fun run(additionalInput: String?): ExecutionResult {
        val currentDirectory = environment.resolveVariable(CURRENT_DIRECTORY)

        if (args.size == 0) {
            environment.addVariable(CURRENT_DIRECTORY, environment.resolveVariable(HOME_DIRECTORY))
            return ExecutionResult(ExecutionState.WORKING)
        }

        if (args.size > 1) {
            return ExecutionResult(ExecutionState.ERRORED, "Too many args(${args.size}) for cd function")
        }

        val nextDirectory = resolveNextDirectory(currentDirectory, args[0])
        nextDirectory ?: return ExecutionResult(ExecutionState.ERRORED, "Directory ${args[0]} not found")

        environment.addVariable(CURRENT_DIRECTORY, nextDirectory)
        return ExecutionResult(ExecutionState.WORKING)
    }
}

/**
 * Class for ls bash command partial simulation.
 * */
class Ls(environment: Environment) : Operation(environment) {
    /**
     * Returns list of files and directories
     */
    override fun run(additionalInput: String?): ExecutionResult {
        var currentDirectory = environment.resolveVariable(CURRENT_DIRECTORY)
        if (args.size != 0) {
            val possiblePath = resolveNextDirectory(currentDirectory, args[0])
            possiblePath ?: return ExecutionResult(ExecutionState.ERRORED, "Illegal operation")

            currentDirectory = possiblePath
        }

        if (args.size > 1) {
            return ExecutionResult(ExecutionState.ERRORED, "Too many args(${args.size}) for ls function")
        }

        var result = ""
        val files = File(currentDirectory).list()
        files ?: return ExecutionResult(ExecutionState.ERRORED, "Error with getting list of files")
        files.forEach { filename ->
            result += if (result == "") {
                filename
            } else {
                "\n" + filename
            }
        }

        return ExecutionResult(ExecutionState.WORKING, result)
    }
}

/**
 * Class for inner process run.
 */
class RunProcess(environment: Environment) : Operation(environment) {
    /**
     * Delegate all given arguments to process builder and translate process output to ExecutionResult.
     */
    override fun run(additionalInput: String?): ExecutionResult {
        val process = ProcessBuilder(args)
                .directory(File(environment.resolveVariable(CURRENT_DIRECTORY)))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
        val output = process.inputStream
        val error = process.errorStream
        process.waitFor()
        val outputString = output.bufferedReader().readText().trim()
        val errorString = error.bufferedReader().readText().trim()

        process.destroy()
        return if (errorString.isNotEmpty()) {
            ExecutionResult(ExecutionState.ERRORED, errorString)
        }
        else {
            ExecutionResult(ExecutionState.WORKING, outputString)
        }
    }
}

/**
 * Class for environmental variable association command.
 */
class Association(environment: Environment) : Operation(environment) {
    /**
     * Add new value to the environment.
     */
    override fun run(additionalInput: String?): ExecutionResult {
        if (args.size != 2) {
            return ExecutionResult(ExecutionState.ERRORED)
        }
        val variableName = args[0]
        val variableValue = args[1]
        environment.addVariable(variableName, variableValue)
        return ExecutionResult(ExecutionState.WORKING)
    }

}

/**
 * Class for Operation instances creation.
 */
class OperationFactory(private val environment: Environment) {
    /**
     * Returns new instance of particular Operation child class.
     * In case of incorrect name returns null.
     */
    fun getOperationByName(name: String): Operation? {
        return when (name) {
            "wc" -> WC(environment)
            "echo" -> Echo(environment)
            "pwd" -> Pwd(environment)
            "cat" -> Cat(environment)
            "exit" -> Exit(environment)
            "ls" -> Ls(environment)
            "cd" -> Cd(environment)
            "!$" -> RunProcess(environment)
            "=" -> Association(environment)
            else -> null
        }
    }
}

fun resolveNextDirectory(currentDirectory: String, cdArg: String): String? {
    return if (cdArg[0] == File.separatorChar) {
        cdArg
    } else {
        val path = pathToStandard("$currentDirectory${File.separator}$cdArg") ?: return null
        if (File(path).exists()) {
            path
        } else {
            null
        }
    }
}

fun pathToStandard(path: String): String? {
    val pathParts = ArrayList<String>()
    var errorOccurs = false

    path.split(File.separator).forEach { part ->
        if (part == "..") {
            if (pathParts.size == 0) {
                errorOccurs = true
            } else {
                pathParts.removeAt(pathParts.size - 1)
            }
        } else {
            pathParts.add(part)
        }
    }
    var result = ""
    return if (errorOccurs) {
        null
    } else {
        pathParts.forEach { part ->
            result += if (part == "") {
                part
            } else {
                "${File.separator}$part"
            }
        }
        result
    }
}