package com.ifpr.androidapptemplatear.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.databinding.ItemModelBinding

data class ModelItem(val file: String, val imported: Boolean)

class ModelAdapter(
    private val onShow: (ModelItem) -> Unit,
    private val onDelete: (ModelItem) -> Unit
) : ListAdapter<ModelItem, ModelAdapter.VH>(Diff) {

    inner class VH(val binding: ItemModelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemModelBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            modelName.text = friendlyModelName(item.file)
            modelSource.setText(if (item.imported) R.string.model_imported else R.string.model_builtin)
            deleteButton.visibility = if (item.imported) View.VISIBLE else View.GONE
            deleteButton.setOnClickListener { onDelete(item) }
            showButton.setOnClickListener { onShow(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ModelItem>() {
        override fun areItemsTheSame(a: ModelItem, b: ModelItem) = a.file == b.file
        override fun areContentsTheSame(a: ModelItem, b: ModelItem) = a == b
    }
}
