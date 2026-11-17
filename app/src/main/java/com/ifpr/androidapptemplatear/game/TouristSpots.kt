package com.ifpr.androidapptemplatear.game

/** Ponto de encontro: ao chegar a até 100 m, o jogador encontra um novo modelo 3D. */
data class TouristSpot(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Pontos ganhos ao encontrar um modelo novo neste local. */
    val points: Int
)

/** Pontos turísticos de Ponta Grossa - PR. Para trocar/adicionar locais, edite só esta lista. */
object TouristSpots {
    val all: List<TouristSpot> = listOf(
        TouristSpot("Catedral Sant'Ana", -25.097922, -50.159264, 100),
        TouristSpot("Igreja dos Polacos", -25.094904, -50.163075, 100),
        TouristSpot("Estação Ferroviária", -25.099575, -50.158396, 150),
        TouristSpot("Buraco do Padre", -25.171830, -49.968420, 200),
        TouristSpot("Parque Estadual de Vila Velha", -25.246243, -50.021782, 250)
    )
}
