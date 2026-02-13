package org.example.blackjackpsp

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// --- CARTAS ---
@Serializable
data class Card(
    val rank: String,
    val suit: String
)

// --- NUEVO: ESTRUCTURA PARA LOS RÉCORDS ---
@Serializable
data class PlayerRecord(
    val name: String,
    val wins: Int
)

// --- PROTOCOLO DE MENSAJES ---
@Serializable
sealed class NetworkMessage {

    // 1. CLIENTE -> SERVIDOR
    @Serializable
    @SerialName("join_table")
    data class JoinTable(
        val playerName: String,
        val buyIn: Int = 1000,
        val numDecks: Int = 4
    ) : NetworkMessage()

    @Serializable
    @SerialName("place_bet")
    data class PlaceBet(val amount: Int) : NetworkMessage()

    @Serializable
    @SerialName("hit")
    data object Hit : NetworkMessage()

    @Serializable
    @SerialName("stand")
    data object Stand : NetworkMessage()

    @Serializable
    @SerialName("double")
    data object Double : NetworkMessage()

    @Serializable
    @SerialName("surrender")
    data object Surrender : NetworkMessage()

    // NUEVO: Mensaje para pedir la lista de mejores jugadores
    @Serializable
    @SerialName("get_records")
    data object GetRecords : NetworkMessage()

    // 2. SERVIDOR -> CLIENTE
    @Serializable
    @SerialName("table_state")
    data class TableState(
        val playerName: String,
        val playerChips: Int,
        val currentBet: Int,
        val playerHand: List<Card>,
        val dealerHand: List<Card>,
        val gameState: String
    ) : NetworkMessage()

    @Serializable
    @SerialName("hand_result")
    data class HandResult(
        val winner: String,
        val payout: Int,
        val message: String
    ) : NetworkMessage()

    // NUEVO: Respuesta del servidor con la lista de récords
    @Serializable
    @SerialName("records_list")
    data class RecordsList(
        val records: List<PlayerRecord>
    ) : NetworkMessage()
}