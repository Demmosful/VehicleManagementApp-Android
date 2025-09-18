package com.demmos.parqueaderoapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.demmos.parqueaderoapp.data.model.Vehiculo
import com.demmos.parqueaderoapp.databinding.ListItemAntiguedadBinding
import java.util.concurrent.TimeUnit

class AntiguedadAdapter(private val onItemClicked: (Vehiculo) -> Unit) :
    ListAdapter<Vehiculo, AntiguedadAdapter.AntiguedadViewHolder>(AntiguedadDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AntiguedadViewHolder {
        val binding = ListItemAntiguedadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AntiguedadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AntiguedadViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClicked)
    }

    class AntiguedadViewHolder(private val binding: ListItemAntiguedadBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Vehiculo, onItemClicked: (Vehiculo) -> Unit) {
            binding.tvInfoVehiculo.text = "${item.matricula} - ${item.marcaNombre} ${item.modeloNombre}"

            val diffInMillis = System.currentTimeMillis() - item.fechaIngreso
            val dias = TimeUnit.MILLISECONDS.toDays(diffInMillis)

            binding.tvDiasEstancia.text = when (dias) {
                0L -> "Hoy"
                1L -> "1 Día"
                else -> "$dias Días"
            }

            itemView.setOnClickListener {
                onItemClicked(item)
            }
        }
    }
}

class AntiguedadDiffCallback : DiffUtil.ItemCallback<Vehiculo>() {
    override fun areItemsTheSame(oldItem: Vehiculo, newItem: Vehiculo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Vehiculo, newItem: Vehiculo): Boolean {
        return oldItem == newItem
    }
}