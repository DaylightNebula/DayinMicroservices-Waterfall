package waterfall.microservices.nodemanager

import com.orbitz.consul.model.health.Service
import org.json.JSONObject
import java.io.File
import java.net.ServerSocket
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class Node(
    val template: Template,
    private val players: MutableList<UUID> = mutableListOf(),
    var running: Boolean = false,
    var serverPort: Int = 0,
    internal var nodeService: Service? = null,
    internal var info: JSONObject? = null
) {
    private val instanceDirectory = File(options["instances_directory_path"]!!, "tmp-${Random.nextInt(0, Int.MAX_VALUE)}")
    private var maxTotalPlayers = 0

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

    private var lastNewNodeCall = System.currentTimeMillis()
    fun addPlayer(uuid: UUID) {
        // add player and update the recommended node
        players.add(uuid)
        updateRecommended()

        // update max total players if necessary
        if (players.size > maxTotalPlayers) maxTotalPlayers = players.size

        // if it has been at least 1 minute since the last new node call and new node at player count is enabled and that player count is reached, create a new node
        if (System.currentTimeMillis() - lastNewNodeCall > 60000 && players.size >= template.newNodeAtPlayerCount && template.newNodeAtPlayerCount != 0) {
            template.newNode()
            lastNewNodeCall = System.currentTimeMillis()
        }
    }

    fun removePlayer(uuid: UUID) {
        players.remove(uuid)
        updateRecommended()
        if (maxTotalPlayers > 0 && players.isEmpty() && template.shutdownNoPlayers)
            stop()
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

    private fun updateRecommended() {
        // if I am the recommended node and this template is default, send set initial node packet to waterfall
        if (template.defaultTemplate && template.getBalancedNode() == this && nodeService != null)
            service.request("waterfall", "set_intial_node", JSONObject().put("server", nodeService!!.service))
    }
}