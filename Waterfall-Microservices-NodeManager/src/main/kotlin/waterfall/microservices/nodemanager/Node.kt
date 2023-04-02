package waterfall.microservices.nodemanager

import org.json.JSONObject
import waterfall.microservices.OtherMicroservice
import java.io.File
import java.net.ServerSocket
import java.util.*
import kotlin.random.Random

class Node(
    val template: Template,
    private val players: MutableList<UUID> = mutableListOf(),
    var running: Boolean = false,
    var serverPort: Int = 0,
    internal var oService: OtherMicroservice? = null,
    internal var info: JSONObject? = null
) {
    private val instanceDirectory = File(options["instances_directory_path"]!!, "tmp-${Random.nextInt(0, Int.MAX_VALUE)}")

    init {
        if (oService == null) create()
        else {
            println("Received node ${oService!!.name}")
            players.addAll(info!!.getJSONArray("players").map { UUID.fromString(it as String) })
        }
    }

    private fun create() {
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
        ProcessBuilder("cmd", "/C", startFile.absolutePath)
            .directory(instanceDirectory)
            .redirectOutput(File(instanceDirectory, "log.txt"))
            .start()
    }

    fun addPlayer(uuid: UUID) {
        players.add(uuid)
        println("Added player $uuid")
    }

    fun removePlayer(uuid: UUID) {
        players.remove(uuid)
        println("Removed player $uuid")
    }

    fun hasPlayer(uuid: UUID): Boolean {
        return players.contains(uuid)
    }

    fun getPlayers(): List<UUID> {
        return players
    }

    fun stop() {
        oService?.let { service.request(it. uuid, "stop", JSONObject()) }
    }
}