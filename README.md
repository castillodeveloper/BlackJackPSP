# 🃏 BlackJack PSP - Pro Edition (Arquitectura Cliente-Servidor)

![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=for-the-badge&logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Multiplatform-4285F4?style=for-the-badge&logo=android)
![Sockets](https://img.shields.io/badge/Java-Sockets-E34F26?style=for-the-badge&logo=java)
![JSON](https://img.shields.io/badge/JSON-Serialization-F7DF1E?style=for-the-badge&logo=json)
![Coroutines](https://img.shields.io/badge/Coroutines-Asynchronous-0095D5?style=for-the-badge&logo=kotlin)

Proyecto final para la asignatura de **Programación de Servicios y Procesos (PSP)**.

Este repositorio contiene una implementación completa del clásico juego de casino **Blackjack**, desarrollada desde cero utilizando **Kotlin Multiplatform (KMP)**. El sistema destaca por su robusta arquitectura Cliente-Servidor multihilo, comunicación mediante Sockets TCP, serialización de datos estructurados (JSON) y una interfaz gráfica de escritorio reactiva construida con Jetpack Compose.

---

## 🎥 Demostración en Vídeo

En el siguiente vídeo se explica la arquitectura del código y se realiza una demostración práctica ejecutando el servidor y múltiples clientes simultáneamente, mostrando las mecánicas de juego y la persistencia de los récords en tiempo real.

👉 https://youtu.be/pAWaj-nY0Fk👈

---

## 🏗️ Arquitectura del Proyecto y Patrones de Diseño

El proyecto ha sido modularizado siguiendo las mejores prácticas de la ingeniería de software para separar las responsabilidades, evitar la duplicación de código y garantizar la escalabilidad. Se divide en tres módulos principales:

### 1. Módulo `shared` (El Protocolo de Comunicación)
Este módulo es la piedra angular del proyecto. Al ser importado tanto por el cliente como por el servidor, garantiza que ambos hablen el mismo "idioma", evitando errores de parseo o tipos discordantes.
* **Protocolo de Red (`NetworkMessage`):** Se ha diseñado utilizando una `sealed class` anotada con `@Serializable`. Esto permite definir un conjunto cerrado y seguro de mensajes que pueden transitar por la red.
    * *Cliente -> Servidor:* Acciones del usuario (`JoinTable`, `PlaceBet`, `Hit`, `Stand`, `Double`, `Surrender`, `GetRecords`).
    * *Servidor -> Cliente:* Respuestas y actualizaciones de estado (`TableState`, `HandResult`, `RecordsList`).
* **Modelos de Dominio:** Entidades base como `Card` (Carta) y `PlayerRecord` (Récord de Jugador), compartidas en todo el ecosistema.

### 2. Módulo `server` (Motor de Juego y Concurrencia)
El backend del sistema, encargado de validar las reglas, mantener el estado de cada partida y gestionar la base de datos.
* **Java ServerSockets y Multihilo:** Escucha peticiones entrantes en el puerto TCP `5000`. Por cada cliente que se conecta (`serverSocket.accept()`), se levanta una **Corrutina** independiente usando `Dispatchers.IO`. Esto permite una concurrencia real donde múltiples jugadores juegan sus partidas de forma simultánea sin que los hilos se bloqueen entre sí.
* **Máquina de Estados:** Cada hilo de cliente mantiene su propio estado de juego (`BETTING`, `PLAYING`, `FINISHED`), asegurando que los usuarios no puedan realizar acciones ilegales (ej. pedir carta sin haber apostado).
* **Persistencia de Datos (JSON):** Implementa un sistema de guardado persistente para el "Salón de la Fama". Al finalizar cada mano, si el jugador gana, se actualiza un mapa en memoria de victorias y se vuelca de manera síncrona al archivo local `records.json` utilizando `kotlinx.serialization`.

### 3. Módulo `composeApp` (Cliente de Escritorio Reactivo)
Aplicación de escritorio (JVM) desarrollada con **Compose Multiplatform**.
* **Gestión de Red Asíncrona (`DesktopClient`):** Las operaciones de lectura/escritura del Socket (`InputStream` y `OutputStream`) se ejecutan en corrutinas en segundo plano. Cuando llega un mensaje del servidor, se actualiza el estado mediante un callback, evitando congelaciones en la interfaz gráfica (ANR).
* **UI Declarativa y Reactiva:** Uso intensivo de `remember` y `LaunchedEffect` para reaccionar a los cambios de estado (`TableState`). La pantalla se repinta automáticamente y de forma eficiente cuando el servidor envía la nueva distribución de cartas o fichas.
* **Cálculos en Cliente:** Aunque el servidor tiene la última palabra, el cliente realiza cálculos visuales útiles, como la puntuación actual de la mano y una **predicción probabilística (%)** en tiempo real del riesgo de pasarse si se pide otra carta.

---

## 📜 Reglas de Juego Avanzadas (Implementadas)

El motor lógico del servidor respeta rigurosamente las reglas del Blackjack profesional:
1. **Lógica de Puntuación:** Las figuras (J, Q, K) suman 10. El As es dinámico: suma 11 o 1 dependiendo de si la mano supera los 21 puntos (ajuste recursivo).
2. **Pagos (Payouts):** * Un Blackjack natural (21 con dos cartas) paga **3:2** sobre la apuesta.
    * Una victoria estándar paga **1:1**.
    * En caso de empate (*Push*), se devuelve la apuesta intacta.
3. **Restricciones del Crupier (Dealer):** Automatizado para pedir cartas (`Hit`) obligatoriamente hasta alcanzar un mínimo de 17 puntos, momento en el que se planta obligatoriamente (`Stand`).
4. **Acciones Tácticas del Jugador:**
    * **Doblar (Double Down):** Permite duplicar la apuesta en mitad de la mano a cambio de recibir *una única carta extra* y plantarse automáticamente.
    * **Rendirse (Surrender):** Si la mano inicial es muy desfavorable, el jugador puede abandonar recuperando el 50% de su apuesta original.

---

## 🛠️ Tecnologías y Dependencias

* **Kotlin (`2.x` / `1.9.x`):** Lenguaje principal del proyecto.
* **Coroutines Core (`1.10.2`):** Para la gestión de hilos, concurrencia de sockets y reactividad del cliente.
* **Kotlinx Serialization JSON (`1.8.0`):** Motor ultrarrápido para codificar y decodificar la jerarquía de `NetworkMessage` y estructurar el fichero `records.json`.
* **Jetpack Compose Desktop:** Framework moderno para la creación de interfaces gráficas nativas en entornos de escritorio.

---

## 🚀 Guía de Instalación y Ejecución

Para evaluar el proyecto en un entorno local, sigue estos pasos:

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/castillodeveloper/BlackJackPSP.git
   cd BlackJackPSP
