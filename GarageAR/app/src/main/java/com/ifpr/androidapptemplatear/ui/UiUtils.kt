package com.ifpr.androidapptemplatear.ui

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.Glide
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.data.Rarity
import java.io.File

fun TextView.bindRarity(rarity: Rarity) {
    text = rarity.label
    setTextColor(rarity.color)
    backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(rarity.color, 0x33))
}

fun ImageView.loadVehiclePhoto(path: String) {
    Glide.with(this).load(File(path)).centerCrop().into(this)
}

fun ImageView.setFavoriteIcon(favorite: Boolean) {
    setImageResource(if (favorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
}
