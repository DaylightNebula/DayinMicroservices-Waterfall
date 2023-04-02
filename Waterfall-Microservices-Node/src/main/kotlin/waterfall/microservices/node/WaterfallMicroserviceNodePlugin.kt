package waterfall.microservices.node

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.json.JSONArray
import org.json.JSONObject
import waterfall.microservices.Microservice
import java.io.File
import java.util.*

class WaterfallMicroserviceNodePlugin: JavaPlugin(), Listener {
    // list of endpoints
    val endpoints = hashMapOf<String, (json: JSONObject) -> JSONObject>(
        "info" to {
            JSONObject()
                .put("directory", System.getProperty("user.dir"))
                .put("players", Bukkit.getOnlinePlayers().map { it.uniqueId })
                .put("plugins", Bukkit.getPluginManager().plugins.map { it.name })
                .put("serverPort", Bukkit.getPort())
                .put("template", System.getProperty("template", ""))
        },
        "get_player_info" to { inJson ->
            if (inJson.has("uuid")) {
                val player = Bukkit.getPlayer(UUID.fromString(inJson.getString("uuid")))
                player?.getJsonInfo() ?: JSONObject()
            } else JSONObject()
        },
        "get_all_players" to { inJson ->
            val includeInfo = inJson.optBoolean("include_info", false)
            JSONObject()
                .put(
                    "players",
                    if (includeInfo)
                        JSONArray().putAll(Bukkit.getOnlinePlayers().map { it.getJsonInfo() })
                    else
                        JSONArray().putAll(Bukkit.getOnlinePlayers().map { it.uniqueId })
                )
        }
    )

    // some necessary variables
    private val rootDir = File(System.getProperty("user.dir"))
    private lateinit var service: Microservice

    // create microservice on start
    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
        service = Microservice("node-${rootDir.name}", endpoints = endpoints)
        service.start()
    }

    // close node service when the plugin shuts down
    override fun onDisable() {
        service.dispose()
    }

    // when a player joins this node, ping all services with a join endpoint telling them that the player joined this node
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val joinServices = service.getOtherServices().filter { it.endpoints.contains("player_join") }
        if (joinServices.isNotEmpty()) {
            val data = JSONObject()
                .put("state", "join")
                .put("node", rootDir.name)
                .put("serviceUUID", service.uuid)
                .put("player", event.player.getJsonInfo())
            joinServices.forEach { service.request(it.uuid, "player_join", data) }
        }
    }

    // when a player leaves this node, ping all services with a quit endpoint telling them that the player left this node
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val quitServices = service.getOtherServices().filter { it.endpoints.contains("player_quit") }
        if (quitServices.isNotEmpty()) {
            val data = JSONObject()
                .put("state", "quit")
                .put("node", rootDir.name)
                .put("serviceUUID", service.uuid)
                .put("player", event.player.getJsonInfo())
            quitServices.forEach { service.request(it.uuid, "player_quit", data) }
        }
    }
}

fun Player.getJsonInfo(): JSONObject {
    return JSONObject()
        .put("uuid", this.uniqueId)
        .put("name", this.name)
        .put("displayName", this.displayName().toString())
        .put("health", this.health)
        .put("gamemode", this.gameMode)
        .put("allowFlight", this.allowFlight)
        .put("location", arrayOf(this.location.x, this.location.y, this.location.z))
        .put("level", this.level)
        .put("ping", this.ping)
        .put("world", this.world.name)
        .put("effects", this.activePotionEffects.map { effect ->
            JSONObject()
                .put("duration", effect.duration)
                .put("type", effect.type)
                .put("amplifier", effect.amplifier)
                .put("ambient", effect.isAmbient)
                .put("infinite", effect.isInfinite)
        })
        .put("inventory", this.inventory.map { item ->
            JSONObject()
                .put("type", item.type)
                .put("amount", item.amount)
                .put("displayName", item.displayName().toString())
        })
}