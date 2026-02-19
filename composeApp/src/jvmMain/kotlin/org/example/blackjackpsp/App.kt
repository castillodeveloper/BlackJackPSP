package org.example.blackjackpsp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class Screen { MENU, CONFIG, GAME, RULES, RECORDS } // <--- NUEVA PANTALLA

interface GameClientInterface {
    fun send(msg: NetworkMessage)
    fun connect()
}

@Composable
fun App(
    client: GameClientInterface,
    state: NetworkMessage.TableState?,
    result: NetworkMessage.HandResult?,
    history: List<String>,
    stats: Triple<Int, Int, Int>,
    topPlayers: List<PlayerRecord> // <--- NUEVO
) {
    var currentScreen by remember { mutableStateOf(Screen.MENU) }

    LaunchedEffect(state) {
        if (state != null) currentScreen = Screen.GAME
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFAFAFA)) {
            when (currentScreen) {
                Screen.MENU -> MenuScreen(
                    onPlayClick = { currentScreen = Screen.CONFIG },
                    onRulesClick = { currentScreen = Screen.RULES },
                    onRecordsClick = { // AL PULSAR EL BOTÓN...
                        client.connect() // Conectamos
                        client.send(NetworkMessage.GetRecords) // Pedimos datos
                        currentScreen = Screen.RECORDS // Cambiamos pantalla
                    }
                )
                Screen.RECORDS -> RecordsScreen(topPlayers) { currentScreen = Screen.MENU } // <--- NUEVA UI
                Screen.CONFIG -> ConfigScreen(client) { currentScreen = Screen.MENU }
                Screen.RULES -> RulesScreen { currentScreen = Screen.MENU }
                Screen.GAME -> {
                    if (state == null) {
                        currentScreen = Screen.MENU
                        MenuScreen({}, {}, {})
                    } else {
                        GameLayout(client, state, result, history, stats) { currentScreen = Screen.MENU }
                    }
                }
            }
        }
    }
}

// --- PANTALLA MENÚ (Con botón nuevo) ---
@Composable
fun MenuScreen(onPlayClick: () -> Unit, onRulesClick: () -> Unit, onRecordsClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("♠️ BLACKJACK PSP ♥️", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text("Pro Edition + Records", fontSize = 20.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(50.dp))
        Button(onClick = onPlayClick, modifier = Modifier.width(220.dp).height(60.dp), colors = ButtonDefaults.buttonColors(Color(0xFF1565C0))) { Text("▶  JUGAR AHORA", fontSize = 18.sp) }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRecordsClick, modifier = Modifier.width(220.dp).height(50.dp), colors = ButtonDefaults.buttonColors(Color(0xFFF9A825))) { Text("🏆  HALL OF FAME") } // BOTÓN NUEVO
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRulesClick, modifier = Modifier.width(220.dp).height(50.dp), colors = ButtonDefaults.buttonColors(Color(0xFF558B2F))) { Text("ℹ  REGLAS / INFO") }
    }
}

// --- PANTALLA DE RÉCORDS (NUEVA) ---
@Composable
fun RecordsScreen(records: List<PlayerRecord>, onBackClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏆 MEJORES JUGADORES", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF9A825))
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(Color.White)) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                items(records) { record ->
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("👤 ${record.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("⭐ ${record.wins} Victorias", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    Divider()
                }
                if (records.isEmpty()) {
                    item { Text("Cargando o sin datos...", modifier = Modifier.padding(20.dp)) }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onBackClick) { Text("⬅ VOLVER AL MENÚ") }
    }
}

// --- RESTO DE PANTALLAS (Sin cambios importantes) ---
@Composable
fun RulesScreen(onBackClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📜 REGLAS DEL CASINO", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(Color.White)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("• Objetivo: Conseguir 21 puntos o acercarse sin pasarse.")
                Spacer(modifier = Modifier.height(10.dp))
                Text("• El Dealer: Se planta obligatoriamente en 17.")
                Spacer(modifier = Modifier.height(10.dp))
                Text("• Pagos: Blackjack paga 3:2. Victoria normal 1:1.")
                Spacer(modifier = Modifier.height(10.dp))
                Text("• Acciones: Puedes Doblar (x2 apuesta) o Rendirte (recuperas 50%).")
                Spacer(modifier = Modifier.height(10.dp))
                Text("• Mazos: Configurable entre 1, 2 o 4 barajas.")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onBackClick) { Text("⬅ VOLVER AL MENÚ") }
    }
}

@Composable
fun ConfigScreen(client: GameClientInterface, onBackClick: () -> Unit) {
    var selectedDecks by remember { mutableStateOf(4) }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Card(elevation = CardDefaults.cardElevation(8.dp), colors = CardDefaults.cardColors(Color(0xFFF1F8E9))) {
            Column(modifier = Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚙️ CONFIGURACIÓN DE MESA", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Text("Número de mazos:")
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(1, 2, 4).forEach { num ->
                        Button(onClick = { selectedDecks = num }, colors = ButtonDefaults.buttonColors(if (selectedDecks == num) Color(0xFF2E7D32) else Color.LightGray)) { Text("$num") }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
                Button(onClick = { client.connect(); client.send(NetworkMessage.JoinTable("Jugador 1", 1000, selectedDecks)) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(Color(0xFF1565C0))) { Text("ENTRAR A LA MESA") }
                Spacer(modifier = Modifier.height(15.dp))
                TextButton(onClick = onBackClick) { Text("Cancelar y Volver") }
            }
        }
    }
}

@Composable
fun GameLayout(client: GameClientInterface, state: NetworkMessage.TableState, result: NetworkMessage.HandResult?, history: List<String>, stats: Triple<Int, Int, Int>, onExit: () -> Unit) {
    var inputReady by remember { mutableStateOf(false) }
    LaunchedEffect(state.gameState) { inputReady = false; delay(500); inputReady = true }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(0.75f).fillMaxHeight().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onExit, colors = ButtonDefaults.buttonColors(Color.Gray), modifier = Modifier.height(35.dp)) { Text("SALIR", fontSize = 10.sp) }
                Spacer(modifier = Modifier.width(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD54F))) { Text("💰 Fichas: ${state.playerChips}  |  Apuesta: ${state.currentBet}", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = Color.Black) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            when (state.gameState) {
                "BETTING" -> {
                    Spacer(modifier = Modifier.height(100.dp))
                    if (state.playerChips < 10) BancarrotaScreen(client, state) else {
                        Text("Realiza tu apuesta:", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(10, 50, 100, 200).forEach { amount ->
                                Button(onClick = { client.send(NetworkMessage.PlaceBet(amount)) }, enabled = state.playerChips >= amount && inputReady) { Text("$$amount") }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
                "PLAYING", "FINISHED" -> {
                    Text("DEALER"); Row { state.dealerHand.forEach { CartaVisual(it) } }
                    Spacer(modifier = Modifier.height(15.dp))
                    Text("TU MANO"); Row { state.playerHand.forEach { CartaVisual(it) } }
                    if (state.gameState == "PLAYING") {
                        val pPoints = calculateVisualPoints(state.playerHand)
                        val bustChance = calculateBustProbability(pPoints)
                        Text("Puntos: $pPoints | Prob. Pasarse: ${bustChance}%", fontSize = 12.sp, color = if(bustChance > 50) Color.Red else Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    if (state.gameState == "PLAYING") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                Button(onClick = { client.send(NetworkMessage.Hit) }, colors = ButtonDefaults.buttonColors(Color(0xFF1976D2)), enabled = inputReady) { Text("PEDIR") }
                                Button(onClick = { client.send(NetworkMessage.Stand) }, colors = ButtonDefaults.buttonColors(Color(0xFFD32F2F)), enabled = inputReady) { Text("PLANTARSE") }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                Button(onClick = { client.send(NetworkMessage.Double) }, enabled = state.playerChips >= state.currentBet && inputReady, colors = ButtonDefaults.buttonColors(Color(0xFFFB8C00))) { Text("DOBLAR") }
                                Button(onClick = { client.send(NetworkMessage.Surrender) }, colors = ButtonDefaults.buttonColors(Color.Gray), enabled = inputReady) { Text("RENDIRSE") }
                            }
                        }
                    } else {
                        Text(result?.message ?: "...", color = if (result?.winner == "PLAYER") Color(0xFF388E3C) else Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(15.dp))
                        Button(onClick = { client.send(NetworkMessage.JoinTable(state.playerName, state.playerChips, 4)) }, colors = ButtonDefaults.buttonColors(Color(0xFF388E3C)), enabled = inputReady) { Text("NUEVA RONDA") }
                    }
                }
            }
        }
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color.LightGray)
        Column(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFFFAFAFA)).padding(10.dp)) {
            Text("📊 ESTADÍSTICAS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Victorias: ${stats.first}", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
            Text("Derrotas: ${stats.second}", color = Color.Red, fontWeight = FontWeight.Bold)
            Text("Empates: ${stats.third}", color = Color.Gray)
            val total = stats.first + stats.second + stats.third
            val winRate = if (total > 0) (stats.first * 100 / total) else 0
            Text("Win Rate: $winRate%", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))
            Text("📜 HISTORIAL (Últ. 10)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            LazyColumn { items(history) { item -> Text(item, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)) } }
        }
    }
}

@Composable
fun BancarrotaScreen(client: GameClientInterface, state: NetworkMessage.TableState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💸 ¡BANCARROTA! 💸", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Text("Te has quedado sin fichas.")
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { client.send(NetworkMessage.JoinTable(state.playerName, 1000)) }, colors = ButtonDefaults.buttonColors(Color(0xFFD32F2F))) { Text("REINICIAR JUEGO") }
        }
    }
}

@Composable
fun CartaVisual(card: Card) {
    Card(modifier = Modifier.padding(4.dp).width(50.dp).height(80.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(Color.White)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            val color = if (card.suit == "HEARTS" || card.suit == "DIAMONDS") Color.Red else Color.Black
            val symbol = when(card.suit) { "HEARTS" -> "♥"; "DIAMONDS" -> "♦"; "CLUBS" -> "♣"; "SPADES" -> "♠"; else -> "?" }
            Text(text = if(card.rank == "?") "?" else "${card.rank}\n$symbol", color = if(card.rank == "?") Color.Blue else color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

fun calculateVisualPoints(hand: List<Card>): Int {
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

fun calculateBustProbability(currentPoints: Int): Int {
    if (currentPoints >= 21) return 100
    var bustCount = 0
    val deckModel = listOf(1,2,3,4,5,6,7,8,9,10,10,10,10)
    for (value in deckModel) { if (currentPoints + value > 21) bustCount++ }
    return (bustCount.toFloat() / 13.0f * 100).toInt()
}