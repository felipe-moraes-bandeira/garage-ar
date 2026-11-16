package com.ifpr.androidapptemplatear.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ifpr.androidapptemplatear.data.Rarity
import com.ifpr.androidapptemplatear.data.Vehicle
import com.ifpr.androidapptemplatear.databinding.ItemVehicleBinding

class VehicleAdapter(
    private val onClick: (Vehicle) -> Unit,
    private val onFavorite: (Vehicle) -> Unit
) : ListAdapter<Vehicle, VehicleAdapter.VH>(Diff) {

    inner class VH(val binding: ItemVehicleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val v = getItem(position)
        with(holder.binding) {
            photo.loadVehiclePhoto(v.photoPath)
            brand.text = v.brand
            model.text = v.model
            summary.text = buildString {
                append(v.year)
                if (v.horsepower > 0) append(" • ${v.horsepower} cv")
            }
            rarity.bindRarity(Rarity.fromLabel(v.rarity))
            favorite.setFavoriteIcon(v.isFavorite)
            favorite.setOnClickListener { onFavorite(v) }
            root.setOnClickListener { onClick(v) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Vehicle>() {
        override fun areItemsTheSame(a: Vehicle, b: Vehicle) = a.id == b.id
        override fun areContentsTheSame(a: Vehicle, b: Vehicle) = a == b
    }
}
