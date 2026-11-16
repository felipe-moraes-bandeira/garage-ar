package com.ifpr.androidapptemplatear.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Caminho absoluto da foto no armazenamento interno do app. */
    val photoPath: String,
    val brand: String,
    val model: String,
    val year: Int,
    val engine: String = "",
    val horsepower: Int = 0,
    val torque: String = "",
    val drivetrain: String = "",
    val rarity: String = Rarity.COMUM.label,
    val foundLocation: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$brand $model"
}
