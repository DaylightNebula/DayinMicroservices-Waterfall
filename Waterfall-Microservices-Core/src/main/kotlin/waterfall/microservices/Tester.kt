package waterfall.microservices

import java.lang.Thread.sleep

val service = Microservice("tester")
fun main() {
    service.start()

    sleep(10000)
    service.dispose()
}