import org.json.JSONObject
import waterfall.microservices.Microservice
import java.lang.Thread.sleep

fun main() {
    val service1 = Microservice("test1", port = 8080)
    service1.start()

    sleep(1000)

    val service2 = Microservice("test2", port = 8081)
    service2.start()

    sleep(2000)
    service2.dispose(hidden = true)

    sleep(1000)
    service1.request("test2", "info", JSONObject()) { println("Request $it") }

//    sleep(250)
//    println("Sending test request...")
//    Requester.rawRequest("http://localhost:8080/", JSONObject(), onComplete = { println("Response: ${it?.toString()}") })
}