package org.example.blackjackpsp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BlackJack_PSP",
    ) {
        App()
    }
}