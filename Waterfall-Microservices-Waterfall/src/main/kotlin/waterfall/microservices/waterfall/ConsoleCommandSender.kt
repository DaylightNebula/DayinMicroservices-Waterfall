package waterfall.microservices.waterfall

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.BaseComponent

class ConsoleCommandSender: CommandSender {
    override fun getName(): String {
        return "CONSOLE"
    }

    @Deprecated("Deprecated in Java", ReplaceWith("println(\"CONSOLE SENDER: \$message\")"))
    override fun sendMessage(message: String?) {
        println("CONSOLE SENDER: $message")
    }

    override fun sendMessage(vararg message: BaseComponent?) {
        println("CONSOLE SENDER: ${message.map { it.toString() }}")
    }

    override fun sendMessage(message: BaseComponent?) {
        println("CONSOLE SENDER: ${message.toString()}")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("println(\"CONSOLE SENDER: \$messages\")"))
    override fun sendMessages(vararg messages: String?) {
        println("CONSOLE SENDER: $messages")
    }

    override fun getGroups(): MutableCollection<String> = mutableListOf()
    override fun addGroups(vararg groups: String?) {}
    override fun removeGroups(vararg groups: String?) {}

    override fun hasPermission(permission: String?): Boolean = true
    override fun setPermission(permission: String?, value: Boolean) {}
    override fun getPermissions(): MutableCollection<String> { return mutableListOf("*") }
}