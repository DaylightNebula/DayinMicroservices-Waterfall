package waterfall.microservices.waterfall

import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.PreLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import org.json.JSONObject
import waterfall.microservices.Microservice
import waterfall.microservices.OtherMicroservice
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

class WaterfallPlugin: Plugin(), Listener {
    // endpoints that can be edited by other plugins before service is set up
    private lateinit var service: Microservice
    private val onNewService: (json: JSONObject) -> Unit = { json -> newNode(json) }
    val endpoints = hashMapOf<String, (json: JSONObject) -> JSONObject>(
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
                    player.connect(serverInfo.first)
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
                    .put("node", servers.values.firstOrNull { it.first.socketAddress == player.server.socketAddress }?.second?.name ?: servers.values.first().second.name)
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
    private val servers = hashMapOf<String, Pair<ServerInfo, OtherMicroservice>>()
    private var initialServer: Pair<ServerInfo, OtherMicroservice>? = null

    override fun onEnable() {
        // setup listener
        ProxyServer.getInstance().pluginManager.registerListener(this, this)

        // setup service
        service = Microservice("waterfall", endpoints = endpoints, onServiceOpen = onNewService)
        service.start()
    }

    private fun newNode(json: JSONObject) {
        // make sure this is a node
        if (!json.has("port") || !json.getString("name").startsWith("node-")) return

        // create new server info
        val address = InetSocketAddress("localhost", json.getInt("port"))
        val serverInfo = ProxyServer.getInstance().constructServerInfo(json.getString("name"), address, "", false)

        // save server info
        servers[json.getString("name")] = Pair(serverInfo, OtherMicroservice(json))
    }

    @EventHandler
    fun onLogin(event: PostLoginEvent) {
        // send player to initial server
        event.player.connect(
            initialServer?.first
                    ?: servers.values.firstOrNull()?.first
                    ?: return
        )
    }
}