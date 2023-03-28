package waterfall.microservices

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withTimeout
import mu.KLogger
import org.json.JSONObject
import kotlin.concurrent.thread

internal object Requester {

    private fun rawGet(logger: KLogger, address: String, builder: HttpRequestBuilder.() -> Unit, onComplete: (json: String?) -> Unit) {
        // start a thread to run the request async
        thread {
            runBlocking {
                // send get request with timeout, if any error occurs, just return null
                val response = try {
                    HttpClient(CIO) {
                        install(Logging) {
                            level = LogLevel.INFO
                        }
                        install(HttpTimeout) {
                            requestTimeoutMillis = 3000
                        }
                    }.get(address, builder)
                } catch (ex: Exception) { logger.warn("Request failed with exception: ${ex.message}"); null }

                // when request completes, call the on complete function
                onComplete(response?.bodyAsText())
            }
        }
    }

    fun rawRequest(logger: KLogger, address: String, json: JSONObject, onComplete: (json: JSONObject?) -> Unit = {}) {
        // make a get request to the address
        rawGet(
            // just pass raw address and logger
            address = address,
            logger = logger,
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