package com.demmos.parqueaderoapp.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.demmos.parqueaderoapp.data.model.Marca
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.demmos.parqueaderoapp.databinding.FragmentManageModelosBinding
import com.demmos.parqueaderoapp.ui.MainEvent
import com.demmos.parqueaderoapp.ui.MainViewModel
import com.demmos.parqueaderoapp.ui.MainViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.*

class ManageModelosFragment : Fragment() {
    private var _binding: FragmentManageModelosBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(VehiculoRepository(), UsuarioRepository())
    }
    private lateinit var modeloAdapter: ModeloGestionAdapter
    private var marcasList: List<Marca> = emptyList()
    private var selectedMarca: Marca? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageModelosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        modeloAdapter = ModeloGestionAdapter(
            onEditClicked = { modelo -> showEditDialog(modelo) },
            onDeleteClicked = { /* Lógica de borrado en ViewModel */ }
        )
        binding.rvModelos.adapter = modeloAdapter

        binding.rvModelos.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.actvMarcaSelector.setOnItemClickListener { parent, _, position, _ ->
            val nombreMarca = parent.getItemAtPosition(position) as String
            selectedMarca = marcasList.find { it.nombre == nombreMarca }
            selectedMarca?.let {
                mainViewModel.onMarcaSeleccionada(it.id)
                binding.fabAddModelo.show()
            }
        }
        binding.fabAddModelo.setOnClickListener {
            selectedMarca?.let { showAddDialog(it) } ?: Toast.makeText(context, "Seleccione una marca", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        mainViewModel.todasLasMarcas.observe(viewLifecycleOwner) { marcas ->
            marcasList = marcas
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, marcas.map { it.nombre })
            binding.actvMarcaSelector.setAdapter(adapter)
        }
        mainViewModel.modelosDeMarcaSeleccionada.observe(viewLifecycleOwner) { modelos ->
            modeloAdapter.submitList(modelos)
        }
        mainViewModel.event.observe(viewLifecycleOwner) { event ->
            event?.let {
                when (it) {
                    is MainEvent.ShowToast -> Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    is MainEvent.ShowDialog -> showErrorDialog(it.title, it.message)
                }
                mainViewModel.onEventHandled()
            }
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showAddDialog(marca: Marca) {
        val editText = EditText(requireContext()).apply { hint = "Nombre del modelo" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Añadir Modelo a ${marca.nombre}")
            .setView(editText)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString().trim().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                if (newName.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        mainViewModel.findOrCreateModelo(newName, marca.id)
                    }
                }
            }
            .show()
    }

    private fun showEditDialog(modelo: com.demmos.parqueaderoapp.data.model.Modelo) {
        val editText = EditText(requireContext()).apply { setText(modelo.nombre) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Modelo")
            .setView(editText)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString().trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                if (newName.isNotEmpty() && newName != modelo.nombre) {


                }
            }
            .show()
    }
}