package waterfall.microservices.nodemanager

import java.io.File
import java.net.ServerSocket
import java.util.*
import kotlin.properties.Delegates
import kotlin.random.Random

class Node(
    template: Template,
    val players: MutableList<UUID> = mutableListOf(),
    var running: Boolean = false,
    var port: Int = 0
) {
    private val instanceDirectory = File(options["instances_directory_path"]!!, "tmp-${Random.nextInt(0, Int.MAX_VALUE)}")
    private val process: Process

    init {
        // log the start
        template.logger.info("Creating node from template ${template.name}...")

        // copy template to instance directory
        instanceDirectory.mkdirs()
        template.directory.copyRecursively(instanceDirectory, overwrite = true)

        // get a blank port if necessary
        if (port == 0) {
            val sSocket = ServerSocket(0)
            port = sSocket.localPort
            sSocket.close()
        }

        // set port in start file
        val startFile = File(instanceDirectory, "start.${if (System.getProperty("os.name").startsWith("win", ignoreCase = true)) "bat" else "sh"}")
        if (!startFile.exists()) {
            template.logger.error("No file named start in template ${template.name}, cancelling node creation!")
        }
        startFile.writeText(startFile.readText().replace("{port}", port.toString()))

        // run start file
        process = ProcessBuilder("cmd", "/C", startFile.absolutePath)
            .directory(instanceDirectory)
            .redirectOutput(File(instanceDirectory, "log.txt"))
            .start()
    }
}