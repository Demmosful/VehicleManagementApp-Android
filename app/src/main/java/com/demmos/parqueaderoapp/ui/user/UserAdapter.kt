package com.demmos.parqueaderoapp.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.demmos.parqueaderoapp.R
import com.demmos.parqueaderoapp.data.model.Usuario
import com.demmos.parqueaderoapp.databinding.ListItemUsuarioBinding
import java.util.Locale

/**
 * Enum para representar las acciones que un administrador puede realizar sobre un usuario.
 */
enum class UserAction { PROMOTE_TO_ADMIN, DEMOTE_TO_USER, DELETE }

/**
 * Adaptador para el RecyclerView que muestra la lista de usuarios.
 * Utiliza ListAdapter para manejar de forma eficiente las actualizaciones de la lista.
 *
 * @param onActionSelected Lambda que se invoca cuando el administrador selecciona una acción
 * (promover, degradar, eliminar) para un usuario.
 */
class UserAdapter(
    private val onActionSelected: (Usuario, UserAction) -> Unit
) : ListAdapter<Usuario, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ListItemUsuarioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding, onActionSelected)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder para cada ítem de usuario en la lista.
     */
    class UserViewHolder(
        private val binding: ListItemUsuarioBinding,
        private val onActionSelected: (Usuario, UserAction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Vincula los datos de un objeto [Usuario] a las vistas del layout.
         */
        fun bind(usuario: Usuario) {


            binding.tvUserName.text = usuario.nombreCompleto?.takeIf { it.isNotBlank() } ?: usuario.nombre


            binding.tvUserRole.text = usuario.rol.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }


            if (usuario.nombre.equals("admin@parqueadero.com", ignoreCase = true)) {
                binding.ivOptions.visibility = View.INVISIBLE
            } else {
                binding.ivOptions.visibility = View.VISIBLE
                binding.ivOptions.setOnClickListener { view ->
                    showPopupMenu(view, usuario)
                }
            }
        }

        /**
         * Muestra el menú contextual con las acciones disponibles para el usuario.
         */
        private fun showPopupMenu(anchorView: View, usuario: Usuario) {
            val popup = PopupMenu(anchorView.context, anchorView)
            popup.inflate(R.menu.user_item_menu)

            val promoteItem = popup.menu.findItem(R.id.action_promote)
            val demoteItem = popup.menu.findItem(R.id.action_demote)


            val esAdmin = usuario.rol == "admin"
            promoteItem.isVisible = !esAdmin
            demoteItem.isVisible = esAdmin

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_promote -> onActionSelected(usuario, UserAction.PROMOTE_TO_ADMIN)
                    R.id.action_demote -> onActionSelected(usuario, UserAction.DEMOTE_TO_USER)
                    R.id.action_delete -> onActionSelected(usuario, UserAction.DELETE)
                }
                true
            }
            popup.show()
        }
    }
}

/**
 * DiffUtil.ItemCallback para que ListAdapter calcule las diferencias entre listas
 * de forma eficiente.
 */
class UserDiffCallback : DiffUtil.ItemCallback<Usuario>() {
    override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario): Boolean = oldItem == newItem
}