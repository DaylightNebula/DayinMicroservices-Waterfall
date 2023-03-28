package waterfall.microservices

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import kotlin.concurrent.thread

internal object Requester {

    private fun rawGet(address: String, builder: HttpRequestBuilder.() -> Unit, onComplete: (json: String?) -> Unit) {
        // start a thread to run the request async
        thread {
            runBlocking {
                val response = HttpClient(CIO) {
                    install(Logging) {
                        level = LogLevel.INFO
                    }
                }.get(address, builder)

                // when request completes, call the on complete function
                onComplete(response.bodyAsText())
            }
        }
    }

    fun rawRequest(address: String, json: JSONObject, onComplete: (json: JSONObject?) -> Unit = {}) {
        // make a get request to the address
        rawGet(
            // just pass raw address
            address = address,
            // pass json as a parameter in the request
            builder = {
                parameter("json", json.toString(1))
            },
            // when request completes, attempt to convert it to a json object and call the callback
            onComplete = {
                val outJson = try { JSONObject(it) } catch (ex: Exception) { null }
                onComplete(outJson)
            }
        )
    }
}