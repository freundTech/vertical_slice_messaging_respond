package edu.kit.ipd.sdq.respond.filling_station

import edu.kit.ipd.sdq.respond.messaging.MessagingClient
import edu.kit.ipd.sdq.respond.utils.listOfLambda
import org.kodein.di.generic.instance
import org.kodein.di.newInstance

fun main() {
    val station by normalScenario.newInstance {
        FillingStation(
            instance(),
            instance(),
            listOfLambda(10) {
                instance<Pump>() //For some reason type inference fails here. Specify it explicitly
            }
        )
    }
    station.moveTo(5)

    val a by normalScenario.instance<MessagingClient>()
    a.disconnect()
    print("reached end of main")
}
