// RUTA: java/com/demmos/parqueaderoapp/ui/history/HistoryFragment.kt

package com.demmos.parqueaderoapp.ui.history

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.demmos.parqueaderoapp.R
import com.demmos.parqueaderoapp.data.model.Marca
import com.demmos.parqueaderoapp.data.model.Modelo
import com.demmos.parqueaderoapp.data.model.Vehiculo
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.demmos.parqueaderoapp.databinding.DialogAddVehicleBinding
import com.demmos.parqueaderoapp.databinding.FragmentHistoryBinding
import com.demmos.parqueaderoapp.ui.MainViewModel
import com.demmos.parqueaderoapp.ui.MainViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(VehiculoRepository(), UsuarioRepository())
    }

    private var historyAdapter: HistoryAdapter? = null
    private var actionMode: ActionMode? = null

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater: MenuInflater = mode.menuInflater
            inflater.inflate(R.menu.history_contextual_menu, menu)
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete_selected -> {
                    historyAdapter?.selectedItems?.toList()?.let {
                        if (it.isNotEmpty()) confirmMultipleDelete(it, mode)
                    }
                    true
                }
                else -> false
            }
        }
        override fun onDestroyActionMode(mode: ActionMode) {
            historyAdapter?.endSelectionMode()
            actionMode = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupListeners()
        handleArguments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.finish()
        _binding = null
    }

    private fun handleArguments() {
        val matricula = arguments?.getString("matricula_a_buscar")
        if (!matricula.isNullOrEmpty()) {
            binding.etSearch.setText(matricula)
            mainViewModel.setSearchQuery(matricula)
            arguments?.clear()
        }
    }

    private fun setupRecyclerView() {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {

        mainViewModel.currentUserProfile.observe(viewLifecycleOwner) { user ->
            if (user == null) return@observe


            if (historyAdapter == null) {
                historyAdapter = HistoryAdapter(
                    rolUsuario = user.rol,
                    onSalidaClicked = { vehiculo -> confirmarSalida(vehiculo) },
                    onEditClicked = { vehiculo -> showEditVehicleDialog(vehiculo) },
                    onDeleteClicked = { vehiculo -> confirmDelete(vehiculo) },
                    onSelectionChanged = {
                        val selectedCount = historyAdapter?.getSelectedCount() ?: 0
                        if (selectedCount > 0) {
                            if (actionMode == null) {
                                actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(actionModeCallback)
                            }
                            actionMode?.title = "$selectedCount seleccionados"
                        } else {
                            actionMode?.finish()
                        }
                    }
                )
                binding.rvHistory.adapter = historyAdapter
            }



            mainViewModel.todosLosVehiculos.observe(viewLifecycleOwner) { listaVehiculos ->
                historyAdapter?.submitList(listaVehiculos)
            }
        }
    }

    private fun setupListeners() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mainViewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun confirmMultipleDelete(itemsToDelete: List<Vehiculo>, mode: ActionMode) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Eliminación Múltiple")
            .setMessage("¿Estás seguro de que quieres ELIMINAR PERMANENTEMENTE ${itemsToDelete.size} registros?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Sí, eliminar") { _, _ ->
                mainViewModel.deleteMultipleVehiculos(itemsToDelete)
                mode.finish()
            }
            .show()
    }

    private fun confirmarSalida(vehiculo: Vehiculo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Salida")
            .setMessage("¿Estás seguro de que quieres marcar la salida del vehículo ${vehiculo.matricula}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Sí, marcar salida") { _, _ ->
                mainViewModel.marcarVehiculoComoSalido(vehiculo)
            }
            .show()
    }

    private fun confirmDelete(vehiculo: Vehiculo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres ELIMINAR PERMANENTEMENTE el registro del vehículo ${vehiculo.matricula}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Sí, eliminar") { _, _ ->
                mainViewModel.deleteVehiculo(vehiculo)
            }
            .show()
    }

    private fun showEditVehicleDialog(vehiculoAEditar: Vehiculo) {
        val dialogBinding = DialogAddVehicleBinding.inflate(LayoutInflater.from(requireContext()))
        var marcasList: List<Marca> = emptyList()
        var selectedMarca: Marca? = null
        var selectedModelo: Modelo? = null

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Registro")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogBinding.etMatricula.setText(vehiculoAEditar.matricula)
        dialogBinding.etUbicacion.setText(vehiculoAEditar.ubicacion)
        dialogBinding.etDetalle.setText(vehiculoAEditar.detalle)

        val marcaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        dialogBinding.actvMarca.setAdapter(marcaAdapter)

        mainViewModel.todasLasMarcas.observe(viewLifecycleOwner) { marcas ->
            marcasList = marcas
            val marcaNombres = marcas.map { it.nombre }.toMutableList()
            marcaNombres.add(0, "+ Agregar nueva marca...")
            marcaAdapter.clear()
            marcaAdapter.addAll(marcaNombres)
            marcaAdapter.notifyDataSetChanged()
            dialogBinding.actvMarca.setText(vehiculoAEditar.marcaNombre, false)
            selectedMarca = marcas.find { it.nombre == vehiculoAEditar.marcaNombre }
            selectedMarca?.let { mainViewModel.onMarcaSeleccionada(it.id) }
        }

        mainViewModel.modelosDeMarcaSeleccionada.observe(viewLifecycleOwner) { modelos ->
            val modeloNombres = modelos.map { it.nombre }.toMutableList()
            modeloNombres.add(0, "+ Agregar nuevo modelo...")
            val nuevoModeloAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, modeloNombres)
            dialogBinding.actvModelo.setAdapter(nuevoModeloAdapter)
            if (selectedMarca?.nombre == vehiculoAEditar.marcaNombre) {
                dialogBinding.actvModelo.setText(vehiculoAEditar.modeloNombre, false)
                selectedModelo = modelos.find { it.nombre == vehiculoAEditar.modeloNombre }
            }
        }

        dialogBinding.actvMarca.setOnItemClickListener { parent, _, position, _ ->
            val nombreSeleccionado = parent.getItemAtPosition(position) as String
            dialogBinding.actvModelo.setText("", false)
            selectedModelo = null
            if (nombreSeleccionado == "+ Agregar nueva marca...") {
                showAddNewBrandDialog(dialogBinding)
            } else {
                selectedMarca = marcasList.find { it.nombre == nombreSeleccionado }
                selectedMarca?.let { mainViewModel.onMarcaSeleccionada(it.id) }
            }
        }

        dialogBinding.actvModelo.setOnItemClickListener { parent, _, position, _ ->
            val nombreSeleccionado = parent.getItemAtPosition(position) as String
            if (nombreSeleccionado == "+ Agregar nuevo modelo...") {
                showAddNewModelDialog(selectedMarca, dialogBinding)
            } else {
                val modelosActuales = mainViewModel.modelosDeMarcaSeleccionada.value ?: emptyList()
                selectedModelo = modelosActuales.find { it.nombre == nombreSeleccionado }
            }
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val matriculaActualizada = dialogBinding.etMatricula.text.toString().trim().uppercase()
                if (matriculaActualizada.isEmpty() || selectedMarca == null || selectedModelo == null) {
                    Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val vehiculoActualizado = vehiculoAEditar.copy(
                    matricula = matriculaActualizada,
                    ubicacion = dialogBinding.etUbicacion.text.toString().trim().uppercase(),
                    detalle = dialogBinding.etDetalle.text.toString().trim().ifEmpty { null },
                    marcaNombre = selectedMarca!!.nombre,
                    modeloNombre = selectedModelo!!.nombre
                )
                mainViewModel.updateVehiculo(vehiculoActualizado)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    @SuppressLint("RestrictedApi")
    private fun showAddNewBrandDialog(dialogBinding: DialogAddVehicleBinding) {
        val editText = TextInputEditText(requireContext())
        editText.hint = "Nombre de la nueva marca"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar Nueva Marca")
            .setView(editText, 50, 20, 50, 20)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombreMarca = editText.text.toString().trim().uppercase()
                if (nuevoNombreMarca.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val marcaCreada = mainViewModel.findOrCreateMarca(nuevoNombreMarca)
                        dialogBinding.actvMarca.setText(marcaCreada.nombre, false)
                        mainViewModel.onMarcaSeleccionada(marcaCreada.id)
                    }
                }
            }
            .show()
    }

    @SuppressLint("RestrictedApi")
    private fun showAddNewModelDialog(marca: Marca?, dialogBinding: DialogAddVehicleBinding) {
        if (marca == null) {
            Toast.makeText(context, "Primero debe seleccionar una marca", Toast.LENGTH_SHORT).show()
            return
        }
        val editText = TextInputEditText(requireContext())
        editText.hint = "Nombre del nuevo modelo"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar Modelo para ${marca.nombre}")
            .setView(editText, 50, 20, 50, 20)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombreModelo = editText.text.toString().trim().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                if (nuevoNombreModelo.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val modeloCreado = mainViewModel.findOrCreateModelo(nuevoNombreModelo, marca.id)
                        dialogBinding.actvModelo.setText(modeloCreado.nombre, false)
                    }
                }
            }
            .show()
    }
}