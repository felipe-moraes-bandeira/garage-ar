package com.ifpr.androidapptemplatear.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt DESC")
    fun observeAll(): LiveData<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: Long): LiveData<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Long): Vehicle?

    /** Quantos outros veículos têm a mesma marca, modelo e ano (ignorando maiúsculas). */
    @Query(
        "SELECT COUNT(*) FROM vehicles WHERE LOWER(TRIM(brand)) = LOWER(TRIM(:brand)) " +
            "AND LOWER(TRIM(model)) = LOWER(TRIM(:model)) AND year = :year AND id != :excludeId"
    )
    suspend fun countSame(brand: String, model: String, year: Int, excludeId: Long): Int

    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)

    @Query("UPDATE vehicles SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}
