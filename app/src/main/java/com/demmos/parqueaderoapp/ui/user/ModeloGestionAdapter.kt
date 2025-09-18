package com.demmos.parqueaderoapp.ui.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.demmos.parqueaderoapp.data.model.Modelo
import com.demmos.parqueaderoapp.databinding.ListItemGestionBinding

class ModeloGestionAdapter(
    private val onEditClicked: (Modelo) -> Unit,
    private val onDeleteClicked: (Modelo) -> Unit
) : ListAdapter<Modelo, ModeloGestionAdapter.ModeloViewHolder>(ModeloDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeloViewHolder {
        val binding = ListItemGestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModeloViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModeloViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClicked, onDeleteClicked)
    }

    class ModeloViewHolder(private val binding: ListItemGestionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(modelo: Modelo, onEdit: (Modelo) -> Unit, onDelete: (Modelo) -> Unit) {
            binding.tvItemName.text = modelo.nombre
            binding.btnEditItem.setOnClickListener { onEdit(modelo) }
            binding.btnDeleteItem.setOnClickListener { onDelete(modelo) }
        }
    }
}

class ModeloDiffCallback : DiffUtil.ItemCallback<Modelo>() {
    override fun areItemsTheSame(oldItem: Modelo, newItem: Modelo): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Modelo, newItem: Modelo): Boolean = oldItem == newItem
}