package waterfall.microservices.nodemanager

import mu.KotlinLogging
import org.json.JSONArray
import org.json.JSONObject
import waterfall.microservices.Microservice
import waterfall.microservices.OtherMicroservice
import java.io.File

val service = Microservice("node-manager", endpoints = hashMapOf(
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
                    .filter { it.oService?.name == name }
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
            if (node?.oService != null) {
                node.oService!!.name
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
        val info = template?.getNodes()?.mapNotNull { it.oService?.name } ?: listOf()

        // send back result
        JSONObject().put("servers", info)
    },

    // endpoint get all templates
    "get_templates" to { _ ->
        // send back list of active templates
        JSONObject().put("templates", templates.values.map { it.name })
    }
), onServiceOpen = { json ->
    // if this is server node, add it to its respective node
    val serverPort = json.optInt("serverPort", -1)
    if (serverPort != -1 && json.has("template")) {
        val template = templates[json.getString("template")] ?: return@Microservice
        template.getNode(serverPort)?.let {
            it.oService = OtherMicroservice(json)
            it.info = json
            it.running = true
            logger.info("Node ${it.serverPort} connected!")
        } ?: template.newNode(OtherMicroservice(json), json)
    }
}, onServiceClose = { json ->
    // get given template and mark the node with the given port as closed
    val serverPort = json.optInt("serverPort", -1)
    if (serverPort != -1 && json.has("template"))
        templates[json.getString("template")]?.getNode(serverPort)?.let { it.running = false }
})
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