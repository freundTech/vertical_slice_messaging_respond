package edu.kit.ipd.sdq.respond.repository.`interface`

import com.google.gson.Gson
import edu.kit.ipd.sdq.respond.repository.*
import edu.kit.ipd.sdq.respond.repository.tables.Plant
import edu.kit.ipd.sdq.respond.repository.tables.Process
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.lang.Exception

class MqttRepositoryInterface(private val client: MqttClient, private val repository: Repository) : MqttCallback {
    private val gson = Gson()
    private val prefixRegex = Regex("(.*)/repository/.*")

    init {
        client.connect()
        client.setCallback(this)
        client.subscribe("#")
    }

    private fun publishProcesses(plant: Plant) {
        val processes = repository.getProcesses(plant)
        val payload = ProcessesPayload(processes)
        client.publish("${plant.path}/repository/processes", gson.toJson(payload).toMqttMessage(true))
    }

    private fun publishProcess(process: Process, plant: Plant) {
        client.publish("${plant.path}/repository/process/${process.id}", gson.toJson(process).toMqttMessage(true))
    }

    private fun publishRemovedProcess(processId: ProcessId, plant: Plant) {
        client.publish("${plant.path}/repository/process/$processId", MqttMessage().also { it.isRetained = true })
    }

    private fun newProcess(mqttMessage: MqttMessage?, plant: Plant) {
        val payload = gson.fromJson(mqttMessage.toStringOrNull(), NewProcessPayload::class.java)
        val processContent = Process(payload.name, payload.process, plant)
        val process = repository.registerProcess(processContent)
        publishProcess(process, plant)
        publishProcesses(plant)
    }

    private fun deleteProcess(mqttMessage: MqttMessage?, plant: Plant) {
        val processId = mqttMessage.toIntOrNull()
        if (processId != null) {
            repository.removeProcess(processId, plant)
            publishProcesses(plant)
            publishRemovedProcess(processId, plant)
        }
    }

    private fun deleteAllProcesses(mqttMessage: MqttMessage?, plant: Plant) {
        val payload = mqttMessage.toStringOrNull()
        if (payload == "YES") {
            val processes = repository.getProcesses(plant)
            repository.removeAllProcesses(plant)
            publishProcesses(plant)
            processes.forEach {
                publishRemovedProcess(it.id, plant)
            }
        }
    }

    private fun updateProcess(id: Int?, mqttMessage: MqttMessage?, plant: Plant) {
        val payload = mqttMessage.toStringOrNull()
        if (id != null && payload != null) {
            val process = repository.updateProcess(id, plant, payload)
            publishProcess(process, plant)
        }
    }

    override fun messageArrived(topic: String?, mqttMessage: MqttMessage?) {
        try {
            if (topic == null) return

            val prefixMatch = prefixRegex.matchEntire(topic) ?: return
            val plant = repository.getPlant(prefixMatch.groupValues[1])

            val paths = PathMatcher(prefix = ".*/repository/") {
                "new_process" { newProcess(mqttMessage, plant) }
                "delete_process" { deleteProcess(mqttMessage, plant) }
                "delete_all_processes" { deleteAllProcesses(mqttMessage, plant) }
                "update/(\\d+)" { updateProcess(it[0].toIntOrNull(), mqttMessage, plant) }
                default {
                    print("Unknown endpoint: $topic")
                }
            }

            paths.match(topic)
        }
        catch (e: Exception) {
            println(e)
        }
    }

    override fun connectionLost(cause: Throwable?) {
        client.reconnect()
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
    }
}


fun String.toMqttMessage(retained: Boolean = false) = MqttMessage(this.toByteArray()).also { it.isRetained = retained }
fun Any.toMqttMessage(retained: Boolean = false) = this.toString().toMqttMessage(retained)
fun MqttMessage?.toStringOrNull(): String? = this?.payload?.decodeToString()
fun MqttMessage?.toIntOrNull(): Int? = this.toStringOrNull()?.toIntOrNull()