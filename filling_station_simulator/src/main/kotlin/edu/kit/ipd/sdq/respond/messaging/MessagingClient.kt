package edu.kit.ipd.sdq.respond.messaging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage

abstract class MessagingClient {
    //TODO: Find a technology independent way to handle topics
    abstract fun publish(message: Message)
    abstract fun disconnect()
}

class MqttMessagingClient(private val client: MqttClient, val coding: MessageCoding, topicPrefix: String = "/") : MessagingClient() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private val topicPrefix: String =
        if (topicPrefix == "" || topicPrefix.reversed()[0] != '/') {
            "$topicPrefix/"
        } else {
            topicPrefix
        }

    init {
        client.connect()
    }

    override fun publish(message: Message) {
        scope.launch {
            mutex.withLock {
                while (!client.isConnected) {
                    delay(100)
                }
                client.publish(topicPrefix, MqttMessage(coding.encode(message)))
            }
        }
    }

    override fun disconnect() {
        client.disconnect()
        client.close()
    }
}