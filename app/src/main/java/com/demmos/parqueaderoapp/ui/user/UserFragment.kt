package com.demmos.parqueaderoapp.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.demmos.parqueaderoapp.LoginActivity
import com.demmos.parqueaderoapp.R
import com.demmos.parqueaderoapp.data.model.Usuario
import com.demmos.parqueaderoapp.data.repository.AuthResult
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.demmos.parqueaderoapp.databinding.DialogAddUserBinding
import com.demmos.parqueaderoapp.databinding.FragmentUserBinding
import com.demmos.parqueaderoapp.ui.MainEvent
import com.demmos.parqueaderoapp.ui.MainViewModel
import com.demmos.parqueaderoapp.ui.MainViewModelFactory
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(VehiculoRepository(), UsuarioRepository())
    }
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupUI()
        setupListeners()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViewModel() {
        val factory = UserViewModelFactory(UsuarioRepository())
        userViewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]
    }

    private fun setupUI() {
        mainViewModel.currentUserProfile.observe(viewLifecycleOwner) { userProfile ->
            if (userProfile == null) return@observe
            binding.tvCurrentUserName.text = "${userProfile.nombreCompleto} (${userProfile.rol})"
            if (userProfile.rol.equals("admin", ignoreCase = true)) {
                setupAdminView()
            } else {
                setupNormalUserView()
            }
        }
    }

    private fun setupNormalUserView() {
        binding.adminSection.visibility = View.GONE
    }

    private fun setupAdminView() {
        binding.adminSection.visibility = View.VISIBLE
        userAdapter = UserAdapter { usuario, action ->
            handleUserAction(usuario, action)
        }
        binding.rvUsers.adapter = userAdapter
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())

        userViewModel.todosLosUsuarios.observe(viewLifecycleOwner) { userList ->
            val currentUserEmail = Firebase.auth.currentUser?.email
            userAdapter.submitList(userList.filter { it.nombre != currentUserEmail })
        }
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.btnAddUser.setOnClickListener { showAddUserDialog() }
        binding.btnManageLists.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_user_to_settingsFragment)
        }

        binding.btnDeleteByPeriod.setOnClickListener {
            mostrarSelectorDeFechasParaBorrado()
        }
    }


    private fun mostrarSelectorDeFechasParaBorrado() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccione un rango para borrar")
            .setSelection(Pair(MaterialDatePicker.thisMonthInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds()))
            .build()

        dateRangePicker.show(parentFragmentManager, "DATE_RANGE_DELETE_PICKER")

        dateRangePicker.addOnPositiveButtonClickListener { selection ->

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection.first
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
            val fechaInicio = calendar.timeInMillis

            calendar.timeInMillis = selection.second
            calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
            val fechaFin = calendar.timeInMillis


            mostrarDialogoDeConfirmacionBorrado(fechaInicio, fechaFin)
        }
    }


    private fun mostrarDialogoDeConfirmacionBorrado(fechaInicio: Long, fechaFin: Long) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaInicioStr = sdf.format(Date(fechaInicio))
        val fechaFinStr = sdf.format(Date(fechaFin))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ ¿Confirmar Eliminación Permanente?")
            .setMessage("Estás a punto de eliminar TODOS los registros de vehículos ingresados entre el $fechaInicioStr y el $fechaFinStr.\n\nESTA ACCIÓN ES IRREVERSIBLE.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Sí, Eliminar Todo") { _, _ ->
                mainViewModel.borrarRegistrosPorPeriodo(fechaInicio, fechaFin)
            }
            .show()
    }

    private fun setupObservers() {
        userViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Success -> Toast.makeText(context, "Usuario ${result.user.email} creado.", Toast.LENGTH_SHORT).show()
                is AuthResult.Error -> Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }


        mainViewModel.event.observe(viewLifecycleOwner) { event ->
            event?.let {
                when (it) {
                    is MainEvent.ShowToast -> Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    is MainEvent.ShowDialog -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(it.title)
                            .setMessage(it.message)
                            .setPositiveButton("Aceptar", null)
                            .show()
                    }
                }
                mainViewModel.onEventHandled()
            }
        }
    }

    private fun handleUserAction(usuario: Usuario, action: UserAction) {
        when (action) {
            UserAction.DELETE -> confirmDeleteUser(usuario)
            UserAction.PROMOTE_TO_ADMIN -> {
                val updatedUser = usuario.copy(rol = "admin")
                userViewModel.updateUser(updatedUser)
                Toast.makeText(context, "${usuario.nombreCompleto} ahora es administrador.", Toast.LENGTH_SHORT).show()
            }
            UserAction.DEMOTE_TO_USER -> {
                val updatedUser = usuario.copy(rol = "usuario")
                userViewModel.updateUser(updatedUser)
                Toast.makeText(context, "${usuario.nombreCompleto} ahora es usuario.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddUserDialog() {
        val dialogBinding = DialogAddUserBinding.inflate(LayoutInflater.from(requireContext()))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Añadir Nuevo Usuario")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val email = dialogBinding.etNewUsername.text.toString().trim()
                val fullName = dialogBinding.etNewFullname.text.toString().trim()
                val password = dialogBinding.etNewPassword.text.toString().trim()
                val role = if (dialogBinding.cbIsAdmin.isChecked) "admin" else "usuario"

                if (email.isNotEmpty() && password.isNotEmpty() && fullName.isNotEmpty()) {
                    val newUser = Usuario(nombre = email, rol = role, nombreCompleto = fullName)
                    userViewModel.addUsuario(newUser, password)
                } else {
                    Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun confirmDeleteUser(usuario: Usuario) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de que quieres eliminar a '${usuario.nombreCompleto}'? Su perfil será borrado de la base de datos.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                userViewModel.deleteUsuario(usuario)
            }
            .show()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro?")
            .setNegativeButton("No", null)
            .setPositiveButton("Sí") { _, _ -> logout() }
            .show()
    }

    private fun logout() {
        UsuarioRepository().logout()
        activity?.let {
            val intent = Intent(it, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            it.finish()
        }
    }
}