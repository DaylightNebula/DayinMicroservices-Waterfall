package waterfall.microservices.waterfall

import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin

class WaterfallPlugin: Plugin(), Listener {
    override fun onEnable() {
        ProxyServer.getInstance().pluginManager.registerListener(this, this)
    }
}