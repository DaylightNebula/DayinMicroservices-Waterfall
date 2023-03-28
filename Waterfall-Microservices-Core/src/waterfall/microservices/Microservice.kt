package waterfall.microservices

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KLogger
import mu.KotlinLogging
import org.json.JSONObject
import org.json.JSONArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Thread
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.ServerSocket
import java.util.*
import kotlin.collections.HashMap

class Microservice(
    // name and id for the service, these are used to identify the
    private val name: String,
    private val uuid: UUID = UUID.randomUUID(),

    // port of this service, if zero, a port will be found automatically
    private var port: Int = 0,

    // an endpoint cannot return null, null is only returned as a result if there is an error
    // this is used instead of ktor routing so that errors can be handled
    private val endpoints: HashMap<String, (json: JSONObject) -> JSONObject> = hashMapOf(),

    // multicast socket info, used for broadcasting service create and shutdown
    private val multicastAddress: InetAddress = InetAddress.getByName("224.0.0.200"),
    private val multicastPort: Int = 3000,
    private val multicastSocket: MulticastSocket = MulticastSocket(multicastPort),

    // logger
    private val logger: KLogger = KotlinLogging.logger("Node ${name.ifBlank { uuid.toString() }}"),
): Thread() {

    lateinit var server: NettyApplicationEngine

    // broadcast checker, runs once per second, sees if any new threads where created or destroyed by listening to the multicast group packets
    private val broadcastChecker = loopingThread(1000) {
//        logger.info { "Time - ${System.currentTimeMillis()}" }
    }

    // just start the server on this thread
    override fun run() {
        setupPort()
        setupDefaults()
        server = embeddedServer(Netty, port = 8080, module = module).start(wait = false)
    }

    // function that finds an open port if necessary
    private fun setupPort() {
        // if port has already been set, skip
        if (port != 0) return

        // grab a blank port by creating a server socket, getting its port and then close the server
        val sSocket = ServerSocket(0)
        port = sSocket.localPort
        sSocket.close()
        logger.info("Found open port $port")
    }

    // function that just sets up default "/" endpoint and "/info" endpoints
    private fun setupDefaults() {
        // setup ping callback, adding to any preexisting version
        val pingCallback = endpoints[""] ?: { JSONObject() }
        endpoints[""] = { pingCallback(it).put("status", "ok") }

        // do the same with info
        val infoCallback = endpoints["info"] ?: { JSONObject() }
        endpoints["info"] = {
            infoCallback(it)
                .put("name", name)
                .put("uuid", uuid)
                .put("endpoints", JSONArray().putAll(endpoints.keys))
        }
    }

    // dynamically create routing using the given endpoints
    private val module: Application.() -> Unit = {
        logger.info("Creating endpoints...")
        routing {
            endpoints.forEach { (name, callback) ->
                get("/$name") {
                    // get json, cancel if null
                    val jsonString = this.call.request.queryParameters["json"]
                    val json =
                        try { JSONObject(jsonString) }
                        catch (ex: Exception) {
                            this.call.respondText("")
                            return@get
                        }

                    // try to get the result, if an error is thrown, return null
                    val result = try {
                        callback(json)
                    } catch (ex: Exception) { ex.printStackTrace(); null }
                    val resultString = result?.toString(4) ?: ""

                    // send back result
                    this.call.respondText(resultString)
                }
                logger.info("Created endpoint /$name")
            }
        }
        logger.info("Setup finished")
    }

    // function that stops everything
    fun dispose() {
        broadcastChecker.dispose()
        server.stop(1000, 1000)
        super.join()
    }
}
