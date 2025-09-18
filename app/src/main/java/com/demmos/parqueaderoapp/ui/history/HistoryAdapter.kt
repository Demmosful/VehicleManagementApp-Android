package com.demmos.parqueaderoapp.ui.history

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.demmos.parqueaderoapp.R
import com.demmos.parqueaderoapp.data.model.Vehiculo
import com.demmos.parqueaderoapp.databinding.ListItemVehiculoBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val rolUsuario: String,
    private val onSalidaClicked: (Vehiculo) -> Unit,
    private val onEditClicked: (Vehiculo) -> Unit,
    private val onDeleteClicked: (Vehiculo) -> Unit,
    private val onSelectionChanged: () -> Unit
) : ListAdapter<Vehiculo, HistoryAdapter.VehiculoViewHolder>(VehiculoDiffCallback()) {

    private var isSelectionMode = false
    val selectedItems = mutableSetOf<Vehiculo>()

    fun startSelectionMode() {
        if (isSelectionMode) return
        isSelectionMode = true
        onSelectionChanged()
    }

    fun endSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun toggleSelection(vehiculo: Vehiculo) {
        if (selectedItems.contains(vehiculo)) {
            selectedItems.remove(vehiculo)
        } else {
            selectedItems.add(vehiculo)
        }
        notifyItemChanged(currentList.indexOfFirst { it.id == vehiculo.id })
        onSelectionChanged()
    }

    fun getSelectedCount(): Int = selectedItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehiculoViewHolder {
        val binding = ListItemVehiculoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehiculoViewHolder(binding, parent.context, this, rolUsuario)
    }

    override fun onBindViewHolder(holder: VehiculoViewHolder, position: Int) {
        holder.bind(getItem(position), onSalidaClicked, onEditClicked, onDeleteClicked)
    }

    class VehiculoViewHolder(
        private val binding: ListItemVehiculoBinding,
        private val context: Context,
        private val adapter: HistoryAdapter,
        private val rolUsuario: String
    ) : RecyclerView.ViewHolder(binding.root) {

        private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(
            item: Vehiculo,
            onSalidaClicked: (Vehiculo) -> Unit,
            onEditClicked: (Vehiculo) -> Unit,
            onDeleteClicked: (Vehiculo) -> Unit
        ) {
            binding.tvMatricula.text = item.matricula
            binding.tvMarcaModelo.text = "${item.marcaNombre} ${item.modeloNombre}"
            binding.tvUbicacion.text = "Ubicación: ${item.ubicacion.ifEmpty { "N/A" }}"
            binding.tvFechaIngreso.text = "Entrada: ${sdf.format(Date(item.fechaIngreso))}"
            binding.tvRegistradoPor.text = "Ingresado por: ${item.nombreCompletoRegistrador ?: item.usuarioRegistrador}"

            if (item.fechaSalida != null) {
                binding.tvFechaSalida.visibility = View.VISIBLE
                binding.tvSalidaPor.visibility = View.VISIBLE
                binding.tvFechaSalida.text = "Salida: ${sdf.format(Date(item.fechaSalida))}"


                binding.tvSalidaPor.text = "Salida por: ${item.nombreCompletoUsuarioSalida ?: "N/A"}"
            } else {
                binding.tvFechaSalida.visibility = View.GONE
                binding.tvSalidaPor.visibility = View.GONE
            }

            itemView.setOnClickListener {
                if (adapter.isSelectionMode) {
                    adapter.toggleSelection(item)
                }
            }

            itemView.setOnLongClickListener {
                if (!adapter.isSelectionMode) {
                    adapter.startSelectionMode()
                    adapter.toggleSelection(item)
                }
                true
            }

            val isDarkTheme = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_MASK
            val cardBackgroundColor = when {
                adapter.selectedItems.contains(item) -> ContextCompat.getColor(context, R.color.selected_item_color)
                item.estado == "salido" -> if (isDarkTheme) ContextCompat.getColor(context, R.color.card_background_disabled_dark) else ContextCompat.getColor(context, R.color.card_background_disabled_light)
                else -> if (isDarkTheme) ContextCompat.getColor(context, R.color.card_background_dark_active) else ContextCompat.getColor(context, R.color.card_background_day_active)
            }
            binding.cardView.setCardBackgroundColor(cardBackgroundColor)

            val primaryTextColor: Int
            val secondaryTextColor: Int
            if (adapter.selectedItems.contains(item)) {
                primaryTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK
                secondaryTextColor = primaryTextColor
            } else if (item.estado == "salido") {
                primaryTextColor = if (isDarkTheme) ContextCompat.getColor(context, R.color.text_color_disabled_dark) else ContextCompat.getColor(context, R.color.text_color_disabled_light)
                secondaryTextColor = primaryTextColor
            } else {
                primaryTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK
                secondaryTextColor = if (isDarkTheme) Color.LTGRAY else Color.GRAY
            }
            binding.tvMatricula.setTextColor(primaryTextColor)
            binding.tvMarcaModelo.setTextColor(primaryTextColor)
            binding.tvUbicacion.setTextColor(primaryTextColor)
            binding.tvFechaIngreso.setTextColor(secondaryTextColor)
            binding.tvRegistradoPor.setTextColor(secondaryTextColor)
            binding.tvFechaSalida.setTextColor(secondaryTextColor)
            binding.tvSalidaPor.setTextColor(secondaryTextColor)

            if (adapter.isSelectionMode) {
                binding.btnDelete.visibility = View.INVISIBLE
                binding.btnEdit.visibility = View.INVISIBLE
                binding.btnMarcarSalida.visibility = View.INVISIBLE
            } else {
                binding.btnDelete.visibility = if (rolUsuario == "admin") View.VISIBLE else View.GONE
                binding.btnDelete.setOnClickListener { onDeleteClicked(item) }

                if (item.estado == "salido") {
                    binding.btnMarcarSalida.visibility = View.GONE
                    binding.btnEdit.visibility = View.GONE
                } else {
                    binding.btnMarcarSalida.visibility = View.VISIBLE
                    binding.btnEdit.visibility = View.VISIBLE
                    binding.btnMarcarSalida.setOnClickListener { onSalidaClicked(item) }
                    binding.btnEdit.setOnClickListener { onEditClicked(item) }
                }
            }
        }
    }
}

class VehiculoDiffCallback : DiffUtil.ItemCallback<Vehiculo>() {
    override fun areItemsTheSame(oldItem: Vehiculo, newItem: Vehiculo): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Vehiculo, newItem: Vehiculo): Boolean = oldItem == newItem
}