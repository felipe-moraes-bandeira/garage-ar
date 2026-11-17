package com.ifpr.androidapptemplatear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.ifpr.androidapptemplatear.data.AppDatabase
import com.ifpr.androidapptemplatear.data.PhotoStorage
import com.ifpr.androidapptemplatear.data.Vehicle
import kotlinx.coroutines.launch

class GarageViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).vehicleDao()

    val vehicles: LiveData<List<Vehicle>> = dao.observeAll()

    fun vehicle(id: Long): LiveData<Vehicle?> = dao.observeById(id)

    fun save(vehicle: Vehicle) {
        viewModelScope.launch {
            if (vehicle.id == 0L) dao.insert(vehicle) else dao.update(vehicle)
        }
    }

    suspend fun isDuplicate(brand: String, model: String, year: Int, excludeId: Long = 0L): Boolean =
        dao.countSame(brand, model, year, excludeId) > 0

    fun toggleFavorite(vehicle: Vehicle) {
        viewModelScope.launch { dao.setFavorite(vehicle.id, !vehicle.isFavorite) }
    }

    fun delete(vehicle: Vehicle) {
        viewModelScope.launch {
            dao.delete(vehicle)
            PhotoStorage.delete(vehicle.photoPath)
        }
    }
}
