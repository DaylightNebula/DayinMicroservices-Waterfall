package waterfall.microservices.nodemanager

import mu.KotlinLogging
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

val options = hashMapOf(
    "templates_description_path" to "templates.json",
    "templates_directory_path" to "templates",
    "instances_directory_path" to "instances"
)
val templates = mutableListOf<Template>()
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
    templates.forEach { template -> logger.info("Loaded template ${template.name}") }
}

// function that loads template json from the given file and puts them in the target list
fun loadTemplates(file: File, rootDir: File, target: MutableList<Template>) {
    // if file does not exist, throw warning and cancel
    if (!file.exists()) {
        logger.warn("Could not find file at ${file.absolutePath} so no templates will be loaded!")
        return
    }

    // add all template json objects from the given file to the target list
    target.addAll(JSONArray(file.readText()).mapNotNull {
        // get json
        val json = it as JSONObject

        // get directory for this template, drop the template if it could not be found
        val directory = File(rootDir, json.getString("name"))
        if (!directory.exists() || !directory.isDirectory)
            logger.warn("Could not get directory at path ${directory.absolutePath}, dropping template ${json.getString("name")}")

        // create the template if the directory exists
        if (directory.exists() && directory.isDirectory) Template(json, directory) else null
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