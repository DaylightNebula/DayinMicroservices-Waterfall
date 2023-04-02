package waterfall.microservices.nodemanager

import mu.KotlinLogging
import org.json.JSONObject
import waterfall.microservices.OtherMicroservice
import waterfall.microservices.loopingThread
import java.io.File

class Template(
    val name: String,
    val directory: File,

    // sets maximum players each node can have
    val maxPlayers: Int = 20,

    // if true, when a node would reach its max player count soon, a new node will be created
    val newNodeAtPlayerCount: Int = 0,

    // determines how the template handles when more players try to join a template with all nodes completely full
    val overflowBehavior: TemplateOverflow = TemplateOverflow.WAIT,

    // if true and there are more nodes around than the specified minimum, a node will be shutdown if it has no players
    val shutdownNoPlayers: Boolean = false,

    // if a nodes player count is at or below this value and there are more nodes around than the specified minimum, the players will be sent to another node
    val maxPlayersMerge: Int = 0,

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
        TemplateOverflow.valueOf(json.optString("overflow_behavior", "wait").uppercase()),
        json.optBoolean("shutdown_no_players", false),
        json.optInt("max_players_merge", 0),
        json.optInt("min_nodes", 0),
        json.optInt("max_nodes", 100),
        json.optBoolean("default_template", false)
    )

    // create update loop
    private val updateLoop = loopingThread(1000) {
        val neededNodes = minNodes - nodes.size

        // if we have less nodes than necessary, create a new node
        if (neededNodes > 0) nodes.addAll(Array(neededNodes) { newNode() })
    }

    // create new node of this type
    fun getNode(serverPort: Int): Node? = nodes.firstOrNull { it.serverPort == serverPort }
    fun newNode(): Node = Node(this)
    fun removeNode(node: Node?) = node?.let { nodes.remove(node); node.stop() }
    fun removeNode(serverPort: Int) { removeNode(nodes.firstOrNull { it.serverPort == serverPort }) }

    fun dispose() {
        updateLoop.dispose()
    }
}
enum class TemplateOverflow { ALLOW, KICK, WAIT, CANCEL }