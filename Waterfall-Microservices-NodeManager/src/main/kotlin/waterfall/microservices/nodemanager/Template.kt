package waterfall.microservices.nodemanager

import com.orbitz.consul.model.health.Service
import mu.KotlinLogging
import org.json.JSONObject
import waterfall.microservices.loopingThread
import java.io.File

class Template(
    val name: String,
    val directory: File,

    // sets maximum players each node can have
    val maxPlayers: Int = 20,

    // if true, when a node would reach its max player count soon, a new node will be created
    val newNodeAtPlayerCount: Int = 0,

    // if true and there are more nodes around than the specified minimum, a node will be shutdown if it has no players
    val shutdownNoPlayers: Boolean = false,

    // min and max nodes that this template can have
    val minNodes: Int = 0,
    val maxNodes: Int = 100,

    // if true, players will first be sent to a node of this template when they join or when they are sent to a fallback server
    val defaultTemplate: Boolean = false,
) {
    private val nodes = mutableListOf<Node>()
    internal val logger = KotlinLogging.logger("Template - $name")

    constructor(json: JSONObject, directory: File): this(
        json.getString("name"),
        directory,
        json.optInt("max_players", 20),
        json.optInt("new_node_at_player_count", 0),
        json.optBoolean("shutdown_no_players", false),
        json.optInt("min_nodes", 0),
        json.optInt("max_nodes", 100),
        json.optBoolean("default_template", false)
    )

    // create update loop
    val startTime = System.currentTimeMillis() + 5000
    private val updateLoop = loopingThread(1000) {
        if (System.currentTimeMillis() < startTime) return@loopingThread
        // if a server has closed, remove it
        nodes.removeAll { it.nodeService != null && !it.running }

        // if we have less nodes than necessary, create a new node
        val neededNodes = minNodes - nodes.size
        if (neededNodes > 0) for (i in 0 until neededNodes) { newNode() }
    }

    // create new node of this type
    fun getNodes(): List<Node> = nodes
    fun getNode(serverPort: Int): Node? = nodes.firstOrNull { it.serverPort == serverPort }
    fun getNode(name: String): Node? = nodes.firstOrNull { it.nodeService?.service == name }
    fun newNode(): Node { val node = Node(this); nodes.add(node); return node }
    fun newNode(oService: Service, info: JSONObject): Node { val node = Node(this, nodeService = oService, info = info); node.running = true; nodes.add(node); return node }
    fun removeNode(node: Node?) = node?.let { nodes.remove(node); node.stop() }
    fun removeNode(serverPort: Int) { removeNode(nodes.firstOrNull { it.serverPort == serverPort }) }

    // functions for load balancing
    fun getBalancedNode(): Node? {
        // TODO balancing
        return nodes.minByOrNull { it.getPlayers().size }
    }

    fun dispose() {
        updateLoop.dispose()
    }
}