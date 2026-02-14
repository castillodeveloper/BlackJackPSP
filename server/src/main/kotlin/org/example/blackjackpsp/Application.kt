package org.example.blackjackpsp

// --- LÓGICA DE CARTAS Y MAZO ---
fun createDeck(numDecks: Int): MutableList<Card> {
    val suits = listOf("Corazones", "Diamantes", "Tréboles", "Picas")
    val ranks = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")
    val deck = mutableListOf<Card>()
    for (i in 1..numDecks) {
        for (suit in suits) {
            for (rank in ranks) {
                deck.add(Card(rank, suit))
            }
        }
    }
    deck.shuffle()
    return deck
}

fun calculateHandValue(hand: List<Card>): Int {
    var value = 0
    var aces = 0
    for (card in hand) {
        when (card.rank) {
            "A" -> { aces += 1; value += 11 }
            "K", "Q", "J", "10" -> value += 10
            else -> value += card.rank.toInt()
        }
    }
    while (value > 21 && aces > 0) {
        value -= 10
        aces -= 1
    }
    return value
}