

package com.demmos.parqueaderoapp.ui.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.demmos.parqueaderoapp.data.model.Marca
import com.demmos.parqueaderoapp.databinding.ListItemGestionBinding

class MarcaGestionAdapter(
    private val onEditClicked: (Marca) -> Unit,
    private val onDeleteClicked: (Marca) -> Unit
) : ListAdapter<Marca, MarcaGestionAdapter.MarcaViewHolder>(MarcaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarcaViewHolder {
        val binding = ListItemGestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MarcaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarcaViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClicked, onDeleteClicked)
    }

    class MarcaViewHolder(private val binding: ListItemGestionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(marca: Marca, onEdit: (Marca) -> Unit, onDelete: (Marca) -> Unit) {
            binding.tvItemName.text = marca.nombre
            binding.btnEditItem.setOnClickListener { onEdit(marca) }
            binding.btnDeleteItem.setOnClickListener { onDelete(marca) }
        }
    }
}

class MarcaDiffCallback : DiffUtil.ItemCallback<Marca>() {
    override fun areItemsTheSame(oldItem: Marca, newItem: Marca): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Marca, newItem: Marca): Boolean = oldItem == newItem
}