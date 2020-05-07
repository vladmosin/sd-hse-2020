@file:JvmName("Main")

package com.sd.hw

fun main() {
    var isWorking = ExecutionState.WORKING
    val environment = Environment()
    while (isWorking != ExecutionState.FINISHED) {
        try {
            val input = readLine() ?: return
            val result = environment.execute(input)
            if (result.textResult.isNotEmpty()) {
                println(result.textResult)
            }
            isWorking = result.isInterrupted
        } catch (e: Exception) {
            print("Something went wrong")
        }
    }
}

