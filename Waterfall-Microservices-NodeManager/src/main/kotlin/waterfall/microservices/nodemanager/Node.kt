package waterfall.microservices.nodemanager

import com.orbitz.consul.model.health.Service
import org.json.JSONObject
import java.io.File
import java.net.ServerSocket
import java.util.*
import kotlin.random.Random

class Node(
    val template: Template,
    private val players: MutableList<UUID> = mutableListOf(),
    var running: Boolean = false,
    var serverPort: Int = 0,
    internal var nodeService: Service? = null,
    internal var info: JSONObject? = null
) {
    private val instanceDirectory = File(options["instances_directory_path"]!!, "tmp-${Random.nextInt(0, Int.MAX_VALUE)}")

    fun setService(nodeService: Service) {
        this.nodeService = nodeService

        updateRecommended()
    }

    init {
        if (nodeService == null) create()
        else {
            setService(nodeService!!) // just trust me
            logger.info("Received node ${nodeService!!.service}, requesting info...")
            // get info
            service.request(nodeService!!.service, "info", JSONObject())?.whenComplete { json, _ ->
                info = json
                players.addAll(info!!.getJSONArray("players").map { UUID.fromString(it as String) })
                logger.info("Received info from node ${nodeService!!.service}")
            } ?: logger.warn("Could not request info from ${nodeService!!.service}")
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
        updateRecommended()
    }

    fun removePlayer(uuid: UUID) {
        players.remove(uuid)
        updateRecommended()
    }

    fun hasPlayer(uuid: UUID): Boolean {
        return players.contains(uuid)
    }

    fun getPlayers(): List<UUID> {
        return players
    }

    fun stop() {
        nodeService?.service?.let { service.request(it, "stop", JSONObject()) }
    }

    fun updateRecommended() {

    }
}