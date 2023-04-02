package waterfall.microservices.nodemanager

import org.json.JSONObject
import waterfall.microservices.OtherMicroservice
import java.io.File
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread
import kotlin.random.Random

class Node(
    template: Template,
    val players: MutableList<UUID> = mutableListOf(),
    var running: Boolean = false,
    var serverPort: Int = 0
) {
    private val instanceDirectory = File(options["instances_directory_path"]!!, "tmp-${Random.nextInt(0, Int.MAX_VALUE)}")
    private val process: Process
    internal var service: OtherMicroservice? = null
    internal var info: JSONObject? = null

    init {
        // log the start
        template.logger.info("Creating node from template ${template.name}...")

        // copy template to instance directory
        instanceDirectory.mkdirs()
        template.directory.copyRecursively(instanceDirectory, overwrite = true)

        // get a blank port if necessary
        if (serverPort == 0) {
            val sSocket = ServerSocket(0)
            serverPort = sSocket.localPort
            sSocket.close()
        }

        // set port in start file
        val startFile = File(instanceDirectory, "start.${if (System.getProperty("os.name").startsWith("win", ignoreCase = true)) "bat" else "sh"}")
        if (!startFile.exists()) {
            template.logger.error("No file named start in template ${template.name}, cancelling node creation!")
        }
        startFile.writeText(
            startFile.readText()
                .replace("{port}", serverPort.toString())
                .replace("{template}", template.name)
        )

        // run start file
        process = ProcessBuilder("cmd", "/C", startFile.absolutePath)
            .directory(instanceDirectory)
            .redirectOutput(File(instanceDirectory, "log.txt"))
            .start()
    }

    fun stop() {
        // start a thread to stop the server so that the main thread does not lag
        thread {
            // give the process 10 seconds to shut down
            thread {
                process.destroy()
                process.waitFor()
            }.join(10000)

            // after which, if the process is still running, force it to shut down
            if (process.isAlive)
                process.destroyForcibly()
        }
    }
}