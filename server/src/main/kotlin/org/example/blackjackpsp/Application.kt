package org.example.blackjackpsp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import java.util.Scanner

// --- GESTIÓN DE FICHERO JSON ---
val recordsFile = File("records.json")
var globalRecords = mutableMapOf<String, Int>() // Mapa en memoria: Nombre -> Victorias

fun loadRecords() {
    if (recordsFile.exists()) {
        try {
            val jsonStr = recordsFile.readText()
            val list = Json.decodeFromString<List<PlayerRecord>>(jsonStr)
            globalRecords = list.associate { it.name to it.wins }.toMutableMap()
            println("📂 Récords cargados: $globalRecords")
        } catch (e: Exception) {
            println("⚠️ Error cargando récords: ${e.message}")
        }
    }
}

fun saveRecords() {
    try {
        val list = globalRecords.map { PlayerRecord(it.key, it.value) }
        val jsonStr = Json.encodeToString(list)
        recordsFile.writeText(jsonStr)
        println("💾 Récords guardados.")
    } catch (e: Exception) {
        println("⚠️ Error guardando récords: ${e.message}")
    }
}

fun main() {
    loadRecords() // Cargar al inicio
    val port = 5000
    val serverSocket = ServerSocket(port)
    // ESTE ES EL MENSAJE QUE DEBES VER PARA SABER QUE ESTÁ ACTUALIZADO:
    println("🚀 Servidor Blackjack Pro + Persistence LISTO en puerto $port")

    runBlocking {
        while (true) {
            val clientSocket = serverSocket.accept()
            launch(Dispatchers.IO) { handleClient(clientSocket) }
        }
    }
}

fun handleClient(socket: Socket) {
    val input = Scanner(socket.getInputStream())
    val output = PrintStream(socket.getOutputStream())
    val blackjackPayout = 1.5

    var playerName = "Jugador"
    var chips = 1000
    var currentBet = 0
    var deck = generateDeck(4)
    val playerHand = mutableListOf<Card>()
    val dealerHand = mutableListOf<Card>()
    var gameState = "BETTING"

    try {
        while (input.hasNextLine()) {
            val jsonStr = input.nextLine()
            val message = try { Json.decodeFromString<NetworkMessage>(jsonStr) } catch (e: Exception) { continue }

            when (message) {
                is NetworkMessage.GetRecords -> {
                    // Enviamos la lista ordenada por victorias
                    val sortedList = globalRecords.map { PlayerRecord(it.key, it.value) }
                        .sortedByDescending { it.wins }
                    output.println(Json.encodeToString<NetworkMessage>(NetworkMessage.RecordsList(sortedList)))
                    output.flush()
                }
                is NetworkMessage.JoinTable -> {
                    if (gameState == "BETTING") chips = message.buyIn
                    playerName = message.playerName
                    gameState = "BETTING"
                    deck = generateDeck(message.numDecks)
                    println("--> $playerName entró. Mesa configurada con ${message.numDecks} mazos.")
                    playerHand.clear(); dealerHand.clear(); currentBet = 0
                    sendState(output, playerName, chips, currentBet, playerHand, dealerHand, gameState)
                }
                is NetworkMessage.PlaceBet -> {
                    if (gameState == "BETTING" && message.amount <= chips && message.amount > 0) {
                        currentBet = message.amount
                        chips -= currentBet
                        gameState = "PLAYING"
                        if (deck.size < 20) deck = generateDeck(4)
                        playerHand.clear(); dealerHand.clear()
                        playerHand.add(drawCard(deck)); dealerHand.add(drawCard(deck))
                        playerHand.add(drawCard(deck)); dealerHand.add(drawCard(deck))

                        if (calculatePoints(playerHand) == 21) {
                            gameState = "FINISHED"
                            val winAmount = (currentBet * (1 + blackjackPayout)).toInt()
                            chips += winAmount

                            // RECORD ACTUALIZADO
                            globalRecords[playerName] = globalRecords.getOrDefault(playerName, 0) + 1
                            saveRecords() // Persistencia inmediata

                            sendState(output, playerName, chips, currentBet, playerHand, dealerHand, gameState)
                            sendResult(output, NetworkMessage.HandResult("PLAYER", winAmount, "¡BLACKJACK! Ganas 3:2"))
                        } else {
                            sendState(output, playerName, chips, currentBet, playerHand, dealerHand, gameState)
                        }
                    }
                }
                is NetworkMessage.Hit -> {
                    if (gameState == "PLAYING") {
                        playerHand.add(drawCard(deck))
                        if (calculatePoints(playerHand) > 21) {
                            gameState = "FINISHED"
                            sendState(output, playerName, chips, currentBet, playerHand, dealerHand, gameState)
                            sendResult(output, NetworkMessage.HandResult("DEALER", 0, "¡Te has pasado! Pierdes."))
                        } else {
                            sendState(output, playerName, chips, currentBet, playerHand, dealerHand, gameState)
                        }
                    }
                }
                is NetworkMessage.Double -> {
                    if (gameState == "PLAYING" && chips >= currentBet) {
                        chips -= currentBet; currentBet *= 2
                        playerHand.add(drawCard(deck))
                        if (calculatePoints(playerHand) > 21) {
                            gameState = "FINISHED"
                            sendState(output, playerName, chips, currentBet, playerHand, dealerHand, gameState)
                            sendResult(output, NetworkMessage.HandResult("DEALER", 0, "¡Te has pasado al doblar!"))
                        } else {
                            finishRound(output, deck, playerHand, dealerHand, currentBet, playerName, chips).also { chips = it }
                            gameState = "FINISHED"
                        }
                    }
                }
                is NetworkMessage.Surrender -> {
                    if (gameState == "PLAYING") {
                        gameState = "FINISHED"
                        val refund = currentBet / 2
                        chips += refund
                        sendState(output, playerName, chips, currentBet, playerHand, dealerHand, gameState)
                        sendResult(output, NetworkMessage.HandResult("DEALER", refund, "Te has rendido."))
                    }
                }
                is NetworkMessage.Stand -> {
                    if (gameState == "PLAYING") {
                        finishRound(output, deck, playerHand, dealerHand, currentBet, playerName, chips).also { chips = it }
                        gameState = "FINISHED"
                    }
                }
                else -> {}
            }
        }
    } catch (e: Exception) { println("Jugador desconectado") } finally { socket.close() }
}

fun finishRound(out: PrintStream, deck: MutableList<Card>, pHand: List<Card>, dHand: MutableList<Card>, bet: Int, name: String, currentChips: Int): Int {
    var newChips = currentChips
    while (calculatePoints(dHand) < 17) dHand.add(drawCard(deck))
    val pPoints = calculatePoints(pHand)
    val dPoints = calculatePoints(dHand)
    var msg = ""; var winner = "DEALER"; var winnings = 0

    if (dPoints > 21 || pPoints > dPoints) {
        winner = "PLAYER"
        winnings = bet * 2
        newChips += winnings
        msg = "¡GANASTE! Recibes $winnings fichas."

        // RECORD ACTUALIZADO
        globalRecords[name] = globalRecords.getOrDefault(name, 0) + 1
        saveRecords() // Persistencia inmediata

    } else if (pPoints == dPoints) {
        winner = "PUSH"; winnings = bet; newChips += winnings; msg = "EMPATE."
    } else {
        msg = "Gana la banca."
    }

    val stateMsg = NetworkMessage.TableState(name, newChips, bet, pHand, dHand, "FINISHED")
    out.println(Json.encodeToString<NetworkMessage>(stateMsg))
    val resMsg = NetworkMessage.HandResult(winner, winnings, msg)
    out.println(Json.encodeToString<NetworkMessage>(resMsg))
    out.flush()
    return newChips
}

fun sendState(out: PrintStream, name: String, chips: Int, bet: Int, pHand: List<Card>, dHand: List<Card>, state: String) {
    val visibleDealerHand = if (state == "PLAYING" && dHand.size >= 2) listOf(dHand[0], Card("?", "?")) else dHand
    out.println(Json.encodeToString<NetworkMessage>(NetworkMessage.TableState(name, chips, bet, pHand, visibleDealerHand, state)))
    out.flush()
}

fun sendResult(out: PrintStream, result: NetworkMessage.HandResult) {
    out.println(Json.encodeToString<NetworkMessage>(result))
    out.flush()
}

fun generateDeck(numDecks: Int): MutableList<Card> {
    val suits = listOf("HEARTS", "DIAMONDS", "CLUBS", "SPADES")
    val ranks = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")
    val deck = mutableListOf<Card>()
    repeat(numDecks) { for (suit in suits) for (rank in ranks) deck.add(Card(rank, suit)) }
    deck.shuffle()
    return deck
}

fun drawCard(deck: MutableList<Card>) = if (deck.isNotEmpty()) deck.removeAt(0) else Card("?", "?")

fun calculatePoints(hand: List<Card>): Int {
    var points = 0; var aces = 0
    for (card in hand) {
        when (card.rank) {
            "A" -> { points += 11; aces++ }
            "K", "Q", "J", "10" -> points += 10
            "?" -> points += 0
            else -> points += card.rank.toInt()
        }
    }
    while (points > 21 && aces > 0) { points -= 10; aces-- }
    return points
}