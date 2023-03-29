package waterfall.microservices.cliinterface

import waterfall.microservices.Microservice
import java.lang.StringBuilder
import java.lang.Thread.sleep

val service = Microservice("cli-interface")
var running = true

fun main() {
    // get service
    service.start()

    // run command while running
    sleep(500)
    while(running) runCommand()

    // stop service
    service.dispose()
}

fun runCommand() {
    // get new command
    print(">> ")
    val command = readln()

    // tokenize the command
    val tokens = tokenize(command)

    // if not tokens error and return
    if (tokens.isEmpty()) {
        println("Could not tokenize command \"$command\"")
        return
    }

    // run command
    when (tokens.first()) {
        "services" -> println("Current services: ${service.getOtherServices()}")
        "stop" -> running = false
        else -> println("Invalid/unknown command \"$command\"")
    }
}

val startGroupChars = arrayOf('{', '[', '(')
val stopGroupChars = arrayOf('}', ']', ')')
var lastQuoteState = false
fun tokenize(str: String): List<String> {
    // setup variables to keep track of the current buffer
    val output = mutableListOf<String>()
    var curBuilder = StringBuilder()
    var curDepth = 0

    // loop through each character in the string
    for (char in str) {
        // check if start and stop character
        val isStart = startGroupChars.contains(char)
        val isStop = !isStart && stopGroupChars.contains(char)

        // change depth
        if (isStart) curDepth++
        else if (isStop) curDepth--

        // custom handle for quotations
        if (char == '"') {
            if (lastQuoteState) {
                lastQuoteState = false
                curDepth--
            } else {
                lastQuoteState = true
                curDepth++
            }
        }

        // if depth is less than 0, return blank array as there was an error
        if (curDepth < 0) return listOf()

        // if space and depth is 0, split, otherwise just add to current builder
        if (char == ' ' && curDepth == 0) {
            output.add(curBuilder.toString())
            curBuilder = StringBuilder()
        } else
            curBuilder.append(char)
    }

    // add current builder to output
    output.add(curBuilder.toString())

    // pass back output
    return output
}