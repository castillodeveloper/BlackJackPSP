package org.example.blackjackpsp

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val desktopClient = remember { DesktopClient() }
    val tableState = remember { mutableStateOf<NetworkMessage.TableState?>(null) }
    val handResult = remember { mutableStateOf<NetworkMessage.HandResult?>(null) }

    // NUEVO: Lista de récords que llegan del servidor
    val topPlayers = remember { mutableStateListOf<PlayerRecord>() }

    val gameHistory = remember { mutableStateListOf<String>() }
    val wins = remember { mutableStateOf(0) }
    val losses = remember { mutableStateOf(0) }
    val draws = remember { mutableStateOf(0) }

    val clientInterface = object : GameClientInterface {
        override fun connect() {
            desktopClient.connect("127.0.0.1", 5000) { msg ->
                when (msg) {
                    is NetworkMessage.TableState -> {
                        tableState.value = msg
                        if (msg.gameState == "BETTING") handResult.value = null
                    }
                    is NetworkMessage.HandResult -> {
                        handResult.value = msg
                        val resultStr = when(msg.winner) {
                            "PLAYER" -> { wins.value++; "✅ GANASTE (+${msg.payout})" }
                            "DEALER" -> { losses.value++; "❌ PERDISTE" }
                            else -> { draws.value++; "➖ EMPATE" }
                        }
                        gameHistory.add(0, resultStr)
                        if (gameHistory.size > 10) gameHistory.removeLast()
                    }
                    // NUEVO: Recibimos la lista y actualizamos la UI
                    is NetworkMessage.RecordsList -> {
                        topPlayers.clear()
                        topPlayers.addAll(msg.records)
                    }
                    else -> {}
                }
            }
        }
        override fun send(msg: NetworkMessage) {
            desktopClient.sendMessage(msg)
        }
    }

    Window(onCloseRequest = ::exitApplication, title = "BlackJack PSP - Pro Edition") {
        App(clientInterface, tableState.value, handResult.value, gameHistory, Triple(wins.value, losses.value, draws.value), topPlayers)
    }
}