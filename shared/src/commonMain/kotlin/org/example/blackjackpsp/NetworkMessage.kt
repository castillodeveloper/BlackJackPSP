package org.example.blackjackpsp

import kotlinx.serialization.Serializable

// --- CARTAS ---
@Serializable
data class Card(
    val rank: String,
    val suit: String
)