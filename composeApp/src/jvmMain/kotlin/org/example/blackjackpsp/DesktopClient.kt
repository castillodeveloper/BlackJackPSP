package org.example.blackjackpsp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintStream
import java.net.Socket
import java.util.Scanner

class DesktopClient {
    private var socket: Socket? = null
    private var output: PrintStream? = null

    fun connect(ip: String, port: Int, onMessageReceived: (NetworkMessage) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket(ip, port)
                output = PrintStream(socket!!.getOutputStream())
                val scanner = Scanner(socket!!.getInputStream())

                println("✅ Conectado a $ip:$port")

                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    try {
                        val message = Json.decodeFromString<NetworkMessage>(line)
                        onMessageReceived(message)
                    } catch (e: Exception) {
                        println("Error leyendo mensaje: $line")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(msg: NetworkMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = Json.encodeToString(msg)
                output?.println(json)
                output?.flush()
            } catch (e: Exception) {
                println("Error enviando: ${e.message}")
            }
        }
    }
}