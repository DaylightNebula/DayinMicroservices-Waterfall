package waterfall.microservices.waterfall

import com.orbitz.consul.model.health.Service
import daylightnebula.daylinmicroservices.Microservice
import daylightnebula.daylinmicroservices.MicroserviceConfig
import daylightnebula.daylinmicroservices.requests.request
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import org.json.JSONObject
import java.net.InetSocketAddress
import java.util.*

class  WaterfallPlugin: Plugin(), Listener {
    // endpoints that can be edited by other plugins before service is set up
    private lateinit var service: Microservice
    private val onServiceOpen: (serv: Service) -> Unit = { serv -> newNode(serv) }
    private val onServiceClose: (serv: Service) -> Unit = { serv -> removeNode(serv) }
    private val endpoints = hashMapOf<String, (json: JSONObject) -> JSONObject>(
        "move_player" to { json ->
            // get player by name or uuid, null if neither
            val player =
                if (json.has("name")) ProxyServer.getInstance().getPlayer(json.getString("name"))
                else if (json.has("uuid")) ProxyServer.getInstance().getPlayer(UUID.fromString(json.getString("uuid")))
                else null

            // get server info
            val serverInfo = servers[json.optString("server", "")]

            // if both are not null, move the player to the server info
            val success =
                if (player != null && serverInfo != null) {
                    player.connect(serverInfo)
                    true
                } else false

            // pass success back to caller
            JSONObject().put("success", success)
        },
        "set_initial_node" to { json ->
            // get server info
            initialServer = servers[json.optString("server", "")]

            JSONObject().put("success", initialServer != null)
        },
        "get_player_info" to { json ->
            // get player
            val player =
                if (json.has("uuid")) ProxyServer.getInstance().getPlayer(UUID.fromString(json.getString("uuid")))
                else if (json.has("name")) ProxyServer.getInstance().getPlayer(json.getString("name"))
                else null

            // if we have a player send back info otherwise send back nothing
            if (player != null)
                JSONObject()
                    .put("node", servers.values.firstOrNull { it.socketAddress == player.server.socketAddress }?.name ?: servers.values.first().name)
                    .put("uuid", player.uniqueId)
                    .put("name", player.name)
            else JSONObject()
        },
        "kick_player" to { json ->
            // get player
            val player =
                if (json.has("uuid")) ProxyServer.getInstance().getPlayer(UUID.fromString(json.getString("uuid")))
                else if (json.has("name")) ProxyServer.getInstance().getPlayer(json.getString("name"))
                else null

            // reason
            val reason = json.getString("reason")

            // kick the player if needed
            val success =
                if (player == null || reason == null)
                    false
                else {
                    player.disconnect(ComponentBuilder().append(reason).create().first())
                    true
                }

            // tell the caller if this was successful
            JSONObject().put("success", success)
        }
    )

    // node stuff
    private val servers = hashMapOf<String, ServerInfo>()
    private var initialServer: ServerInfo? = null

    override fun onEnable() {
        // setup listener
        ProxyServer.getInstance().pluginManager.registerListener(this, this)

        // setup service
        service = Microservice(
            MicroserviceConfig(
                "waterfall",
                tags = listOf("waterfall")
            ),
            endpoints = endpoints, onServiceOpen = onServiceOpen, onServiceClose = onServiceClose
        )
        service.start()
    }

    override fun onDisable() {
        service.dispose()
    }

    private fun newNode(serv: Service) {
        // make sure this is a node
        if (!serv.tags.contains("spigot")) return

        // send info request
        service.request(serv, "info", JSONObject()).whenComplete { json, _ ->
            // create new server info
            val address = InetSocketAddress(serv.address, json.getInt("serverPort"))
            val serverInfo = ProxyServer.getInstance().constructServerInfo(serv.service, address, "", false)

            // save server info
            servers[serv.service] = serverInfo
            println("Created new node ${serv.service}")
        } ?: logger.warning("Could not send info request to new node ${serv.service}")
    }

    private fun removeNode(serv: Service) {
        // make sure this is a node
        if (!serv.tags.contains("spigot")) return

        // remove old server
        servers.remove(serv.service)
    }

    @EventHandler
    fun onLogin(event: ServerConnectEvent) {
        // send player to initial server
        event.target = initialServer
            ?: servers.values.firstOrNull()
                    ?: return
    }
}