@file:JvmName("Main")

package com.sd.hw

fun main() {
    var isRunning = true
    val environment = Environment()
    while (isRunning) {
        val input = readLine() ?: return
        val result = environment.execute(input)
        if (result.textResult.isNotEmpty()) {
            println(result.textResult)
        }
        isRunning = !result.isInterrupted
    }
}

