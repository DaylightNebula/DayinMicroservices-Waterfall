package waterfall.microservices

import org.json.JSONObject
import java.lang.Thread.sleep

fun main() {
    val service = Microservice("test")
    service.start()

    println("Sending test request...")
    Requester.rawRequest("http://localhost:8080/", JSONObject(), onComplete = { println("Response: ${it?.toString()}") })
}