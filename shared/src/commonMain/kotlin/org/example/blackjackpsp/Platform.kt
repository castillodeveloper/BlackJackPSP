package org.example.blackjackpsp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform