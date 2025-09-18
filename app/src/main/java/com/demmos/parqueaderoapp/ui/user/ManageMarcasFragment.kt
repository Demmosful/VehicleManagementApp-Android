

package com.demmos.parqueaderoapp.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.demmos.parqueaderoapp.data.model.Marca
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.demmos.parqueaderoapp.databinding.FragmentManageMarcasBinding
import com.demmos.parqueaderoapp.ui.MainEvent
import com.demmos.parqueaderoapp.ui.MainViewModel
import com.demmos.parqueaderoapp.ui.MainViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ManageMarcasFragment : Fragment() {

    private var _binding: FragmentManageMarcasBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(VehiculoRepository(), UsuarioRepository())
    }
    private lateinit var marcaAdapter: MarcaGestionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageMarcasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupListeners()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        marcaAdapter = MarcaGestionAdapter(
            onEditClicked = { marca -> showEditDialog(marca) },
            onDeleteClicked = { /* La lógica de borrado estará en MainViewModel */ }
        )
        binding.rvMarcas.adapter = marcaAdapter
        binding.rvMarcas.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.fabAddMarca.setOnClickListener { showAddDialog() }
    }

    private fun setupObservers() {
        mainViewModel.todasLasMarcas.observe(viewLifecycleOwner) { marcas ->
            marcaAdapter.submitList(marcas)
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

    private fun showAddDialog() {
        val editText = EditText(requireContext()).apply { hint = "Nombre de la marca" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Añadir Nueva Marca")
            .setView(editText)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString().trim().uppercase()
                if (newName.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        mainViewModel.findOrCreateMarca(newName)
                    }
                }
            }
            .show()
    }

    private fun showEditDialog(marca: Marca) {
        val editText = EditText(requireContext()).apply { setText(marca.nombre) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Marca")
            .setView(editText)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString().trim().uppercase()
                if (newName.isNotEmpty() && newName != marca.nombre) {


                }
            }
            .show()
    }
}