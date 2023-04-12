package waterfall.microservices.nodemanager

import com.orbitz.consul.model.health.Service
import daylightnebula.daylinmicroservices.Microservice
import daylightnebula.daylinmicroservices.MicroserviceConfig
import daylightnebula.daylinmicroservices.requests.request
import daylightnebula.daylinmicroservices.requests.requestByUUID
import mu.KotlinLogging
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.HashMap

val onServiceOpen: (serv: Service) -> Unit = { serv ->
    // if that service is a spigot node, load it
    if (serv.tags.contains("spigot")) {
        println("Spigot service created ${serv.service}")
        // request the nodes info
        service.requestByUUID(UUID.fromString(serv.id), "info", JSONObject())?.whenComplete { json, _ ->
            val templateName = json.getString("template")
            val port = json.getInt("serverPort")
            templates[templateName]?.let { template ->
                template.getNode(port)?.let {
                    it.setService(serv)
                    it.info = json
                    it.running = true
                    logger.info("Node ${it.serverPort} connected!")
                } ?: template.newNode(serv, json)
            }
        } ?: logger.warn("Could not request info on service open from ${serv.service}")
    }
}
val onServiceClose: (serv: Service) -> Unit = { serv ->
    // make sure this is a spigot service
    if (serv.tags.contains("spigot")) {
        // find a node with the given service (since we cant request info)
        templates.values.forEach { template -> template.getNodes().forEach { node -> if (node.nodeService == serv) node.running = false } }
    }
}
val service = Microservice(
    MicroserviceConfig(
        "node-manager",
        listOf("node-manager")
    ),
    endpoints = hashMapOf(
    // endpoint ot create a new node of the given template
    "create_node" to { json ->
        val template = templates[json.optString("template", "")]
        val success = if (template != null) {
            template.newNode()
            true
        } else false
        JSONObject().put("success", success)
    },

    // endpoint to close a node with a given port
    "close_node" to { json ->
        var success = false

        // if a name is given, remove that node
        if (json.has("name")) {
            val name = json.getString("name")
            templates.forEach { (_, template) ->
                template.getNodes()
                    .filter { it.nodeService?.service == name }
                    .apply { if (isNotEmpty()) success = true }
                    .forEach { template.removeNode(it) }
            }
        }
        // otherwise, if a server port is given, remove that node
        else if (json.has("serverPort")) {
            val serverPort = json.getInt("serverPort")
            templates.forEach { (_, template) ->
                template.getNodes()
                    .filter { it.serverPort == serverPort }
                    .apply { if (isNotEmpty()) success = true }
                    .forEach { template.removeNode(it) }
            }
        }

        JSONObject().put("success", success)
    },

    // endpoint to move a player to a node
    "get_node_from_template" to { json ->
        // get template
        val template = templates[json.optString("template", "")]

        // get node name for a balanced node for the given template
        val server = if (template != null) {
            val node = template.getBalancedNode()
            if (node?.nodeService != null) {
                node.nodeService!!.service
            } else false
        } else null

        // send back result
        JSONObject().put("success", server != null).put("server", server)
    },

    // endpoint get all nodes of a given template
    "get_nodes_from_template" to { json ->
        // get template
        val template = templates[json.optString("template", "")]

        // get nodes from the template
        val info = template?.getNodes()?.mapNotNull { it.nodeService?.service } ?: listOf()

        // send back result
        JSONObject().put("servers", info)
    },

    // endpoint get all templates
    "get_templates" to { _ ->
        // send back list of active templates
        JSONObject().put("templates", templates.values.map { it.name })
    },

    // endpoint that is called when a player joins a node
    "player_join" to { json ->
        // get and setup basic info
        val node = json.optString("node", "")
        val uuid = UUID.fromString(json.getJSONObject("player").getString("uuid"))
        println("Player join $json")

        // find the given node and add the player
        templates.values.forEach { it.getNode(node)?.addPlayer(uuid) }

        // send back nothing
        JSONObject()
    },

    // endpoint that is called when a player leaves a node
    "player_quit" to { json ->
        // get and setup basic info
        val node = json.optString("node", "")
        val uuid = UUID.fromString(json.getJSONObject("player").getString("uuid"))
        println("Player quit $json")

        // find the given node and remove the player
        templates.values.forEach { it.getNode(node)?.removePlayer(uuid) }

        // send back nothing
        JSONObject()
    }
), onServiceOpen = onServiceOpen, onServiceClose = onServiceClose)
val options = hashMapOf(
    "templates_description_path" to "templates.json",
    "templates_directory_path" to "templates",
    "instances_directory_path" to "instances"
)
val templates = hashMapOf<String, Template>()
val logger = KotlinLogging.logger("NodeManager")

fun main(args: Array<String>) {
    // update options
    updateOptions(args, options)

    // clear instances directory
    val instances = File(options["instances_directory_path"]!!)
    if (instances.exists()) instances.deleteRecursively()

    // load templates
    loadTemplates(
        File(options["templates_description_path"]!!),
        File(options["templates_directory_path"]!!),
        templates
    )
    templates.forEach { template -> logger.info("Loaded template ${template.value.name}") }

    service.start()
}

// function that loads template json from the given file and puts them in the target list
fun loadTemplates(file: File, rootDir: File, target: HashMap<String, Template>) {
    // if file does not exist, throw warning and cancel
    if (!file.exists()) {
        logger.warn("Could not find file at ${file.absolutePath} so no templates will be loaded!")
        return
    }

    // add all template json objects from the given file to the target list
    target.putAll(JSONArray(file.readText()).mapNotNull {
        // get json
        val json = it as JSONObject

        // get directory for this template, drop the template if it could not be found
        val directory = File(rootDir, json.getString("name"))
        if (!directory.exists() || !directory.isDirectory)
            logger.warn("Could not get directory at path ${directory.absolutePath}, dropping template ${json.getString("name")}")

        // create the template if the directory exists
        if (directory.exists() && directory.isDirectory) json.getString("name") to Template(json, directory) else null
    })
}

// function that takes then given arguments and options and populates options with any options specified in the given options
fun updateOptions(args: Array<String>, options: HashMap<String, String>) {
    // loop through each arg and process
    args.forEach { str ->
        // make sure this argument is an option
        if (!str.contains("=")) return@forEach

        // process the option and put it in the output
        val tokens = str.split("=", limit = 2)
        val option = tokens.first().lowercase()
        if (options.containsKey(option))
            options[option] = tokens.last()
    }
}