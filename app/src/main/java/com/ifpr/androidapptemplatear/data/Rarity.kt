package com.ifpr.androidapptemplatear.data

import android.graphics.Color

enum class Rarity(val label: String, val color: Int) {
    COMUM("Comum", Color.parseColor("#AAAAAA")),
    INCOMUM("Incomum", Color.parseColor("#4CAF50")),
    RARO("Raro", Color.parseColor("#42A5F5")),
    EPICO("Épico", Color.parseColor("#AB47BC")),
    LENDARIO("Lendário", Color.parseColor("#FF8700"));

    companion object {
        fun fromLabel(label: String): Rarity = values().firstOrNull { it.label == label } ?: COMUM
    }
}

object Drivetrain {
    val options = listOf(
        "Dianteira (FWD)",
        "Traseira (RWD)",
        "Integral (AWD)",
        "4x4"
    )
}
