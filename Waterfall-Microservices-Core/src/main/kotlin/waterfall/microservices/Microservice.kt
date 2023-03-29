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
import java.lang.Thread
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.util.*
import javax.print.attribute.standard.MediaSize.Other
import kotlin.collections.HashMap

class Microservice(
    // name and id for the service, these are used to identify the
    private val name: String,
    val uuid: UUID = UUID.randomUUID(),

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

    // server stuff
    private lateinit var server: NettyApplicationEngine

    // other services
    private val otherServices = hashMapOf<UUID, OtherMicroservice>()

    // broadcast checker, runs once per second, sees if any new threads where created or destroyed by listening to the multicast group packets
    private val broadcastChecker = loopingThread(1000) {
        // read packet length
        val sizeBuffer = ByteArray(4)
        val sizePacket = DatagramPacket(sizeBuffer, 4)
        multicastSocket.receive(sizePacket)

        // read packet
        val buffer = ByteArray(ByteBuffer.wrap(sizeBuffer).int)
        val packet = DatagramPacket(buffer, buffer.size)
        multicastSocket.receive(packet)

        // convert packet to json, cancel if json could not be read
        val json = try { JSONObject(String(packet.data)) } catch (ex: Exception) { ex.printStackTrace(); return@loopingThread }

        // handle join and close
        when (val status = json.optString("status") ?: "NOT_GIVEN") {
            "join" -> {
                // make sure new service is not a new service
                val servName = json.getString("name")
                val servUUID = UUID.fromString(json.getString("uuid"))
                if (uuid != servUUID && !otherServices.any { it.key == servUUID && it.value.name == servName }) joinServices(servUUID, OtherMicroservice(json))
            }
            "close" -> {
                // remove service
                otherServices.remove(UUID.fromString(json.getString("uuid")))
            }
            else -> throw IllegalArgumentException("Unknown status: $status")
        }
    }

    fun getOtherServices(): Collection<OtherMicroservice> = otherServices.values

    private fun joinServices(uuid: UUID, otherService: OtherMicroservice) {
        // save service
        otherServices[uuid] = otherService

        // send it a join packet
        broadcastPacket(getJoinPacket().toString(0).toByteArray())
    }

    // just start the server on this thread
    override fun run() {
        // finish setting up sockets
        multicastSocket.joinGroup(InetSocketAddress(multicastAddress, multicastPort), NetworkInterface.getByName("bge0"))

        // create microservice server and endpoints
        setupPort()
        setupDefaults()
        server = embeddedServer(Netty, port = port, module = module).start(wait = false)

        // broadcast join packet
        broadcastPacket(getJoinPacket().toString(0).toByteArray())
    }

    // make request to services
    fun request(target: String, endpoint: String, json: JSONObject, onComplete: (json: JSONObject?) -> Unit)
        { request(otherServices.values.firstOrNull { it.name == target } ?: return, endpoint, json, onComplete) }
    fun request(target: UUID, endpoint: String, json: JSONObject, onComplete: (json: JSONObject?) -> Unit)
        { request(otherServices[target] ?: return, endpoint, json, onComplete) }
    private fun request(target: OtherMicroservice, endpoint: String, json: JSONObject, onComplete: (json: JSONObject?) -> Unit) {
        Requester.rawRequest(logger, "http://localhost:${target.port}/$endpoint", json, onComplete)
    }

    // function that sends a byte array to a given socket
    private fun broadcastPacket(data: ByteArray) {
        val socket = DatagramSocket()
        socket.broadcast = true
        val sizeBuffer = ByteBuffer.allocate(4).putInt(data.size).array()
        socket.send(DatagramPacket(sizeBuffer, 4, multicastAddress, multicastPort))
        socket.send(DatagramPacket(data, data.size, multicastAddress, multicastPort))
        socket.close()
    }

    // function that creates service join packet
    private lateinit var cachedJoinPacket: JSONObject
    private fun getJoinPacket(): JSONObject {
        if (!this::cachedJoinPacket.isInitialized)
            cachedJoinPacket = (endpoints["info"]?.let { it(JSONObject()) } ?: JSONObject())
                .put("status", "join")
                .put("port", port)
        return cachedJoinPacket
    }

    // function that creates service close packet
    private lateinit var cachedClosePacket: JSONObject
    private fun getClosePacket(): JSONObject {
        if (!this::cachedClosePacket.isInitialized)
            cachedClosePacket = (endpoints["info"]?.let { it(JSONObject()) } ?: JSONObject()).put("status", "close")
        return cachedClosePacket
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
    fun dispose(hidden: Boolean = false) {
        if (!hidden) broadcastPacket(getClosePacket().toString(0).toByteArray())
        broadcastChecker.dispose()
        server.stop(1000, 1000)
        logger.info { "Shutdown $name, hidden = $hidden" }
        super.join()
    }
}

data class OtherMicroservice(val name: String, val uuid: UUID, val port: Int, val endpoints: List<String>) {
    constructor(json: JSONObject): this(
        json.getString("name"),
        UUID.fromString(json.getString("uuid")),
        json.getInt("port"),
        json.getJSONArray("endpoints").map { it as String }
    )
}