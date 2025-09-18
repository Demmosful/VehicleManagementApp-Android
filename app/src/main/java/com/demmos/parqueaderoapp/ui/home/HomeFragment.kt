package com.demmos.parqueaderoapp.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import com.demmos.parqueaderoapp.R
import com.demmos.parqueaderoapp.data.model.Marca
import com.demmos.parqueaderoapp.data.model.Modelo
import com.demmos.parqueaderoapp.data.model.Vehiculo
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.demmos.parqueaderoapp.databinding.DialogAddVehicleBinding
import com.demmos.parqueaderoapp.databinding.FragmentHomeBinding
import com.demmos.parqueaderoapp.ui.MainEvent
import com.demmos.parqueaderoapp.ui.MainViewModel
import com.demmos.parqueaderoapp.ui.MainViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(VehiculoRepository(), UsuarioRepository())
    }

    private lateinit var antiguedadAdapter: AntiguedadAdapter

    private val hintHandler = Handler(Looper.getMainLooper())
    private var hintRunnable: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAntiguedadRecyclerView()
        setupListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        showFabHint()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        hintRunnable?.let { hintHandler.removeCallbacks(it) }
        _binding = null
    }

    private fun showFabHint() {
        hintRunnable?.let { hintHandler.removeCallbacks(it) }

        val hintView = binding.fabTextHint
        hintView.alpha = 0f
        hintView.visibility = View.VISIBLE

        hintView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        hintRunnable = Runnable {
            hintView.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    hintView.visibility = View.GONE
                }
                .start()
        }

        hintHandler.postDelayed(hintRunnable!!, 3000)
    }

    private fun setupAntiguedadRecyclerView() {
        antiguedadAdapter = AntiguedadAdapter { vehiculoSeleccionado ->
            val matricula = vehiculoSeleccionado.matricula
            val bundle = Bundle().apply { putString("matricula_a_buscar", matricula) }
            val bottomNavView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
            val historyMenuItem = bottomNavView.menu.findItem(R.id.navigation_history)
            NavigationUI.onNavDestinationSelected(historyMenuItem, findNavController())
            findNavController().navigate(R.id.navigation_history, bundle)
        }
        binding.rvAntiguedad.adapter = antiguedadAdapter
        binding.rvAntiguedad.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.fabAddVehicle.setOnClickListener {
            showAddVehicleDialog()
        }
    }

    private fun setupObservers() {
        mainViewModel.conteoVehiculosActivos.observe(viewLifecycleOwner) { count ->
            binding.tvActiveVehiclesCount.text = count?.toString() ?: "0"
        }
        mainViewModel.vehiculosActivosPorAntiguedad.observe(viewLifecycleOwner) { lista ->
            antiguedadAdapter.submitList(lista)
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

    private fun showAddVehicleDialog() {
        val dialogBinding = DialogAddVehicleBinding.inflate(LayoutInflater.from(requireContext()))
        var marcasList: List<Marca> = emptyList()
        var selectedMarca: Marca? = null
        var selectedModelo: Modelo? = null

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar Nuevo Vehículo")
            .setCancelable(false)
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        val marcaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        dialogBinding.actvMarca.setAdapter(marcaAdapter)

        mainViewModel.todasLasMarcas.observe(viewLifecycleOwner) { marcas ->
            marcasList = marcas
            val marcaNombres = marcas.map { it.nombre }.toMutableList()
            marcaNombres.add(0, "+ Agregar nueva marca...")
            marcaAdapter.clear()
            marcaAdapter.addAll(marcaNombres)
            marcaAdapter.notifyDataSetChanged()
        }

        mainViewModel.modelosDeMarcaSeleccionada.observe(viewLifecycleOwner) { modelos ->
            val modeloNombres = modelos.map { it.nombre }.toMutableList()
            modeloNombres.add(0, "+ Agregar nuevo modelo...")
            val nuevoModeloAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, modeloNombres)
            dialogBinding.actvModelo.setAdapter(nuevoModeloAdapter)
        }

        dialogBinding.actvMarca.setOnItemClickListener { parent, _, position, _ ->
            val nombreSeleccionado = parent.getItemAtPosition(position) as String
            dialogBinding.actvModelo.setText("", false)
            selectedModelo = null
            if (nombreSeleccionado == "+ Agregar nueva marca...") {
                showAddNewBrandDialog(dialogBinding) { nuevaMarca ->
                    selectedMarca = nuevaMarca
                    mainViewModel.onMarcaSeleccionada(nuevaMarca.id)
                }
            } else {
                selectedMarca = marcasList.find { it.nombre == nombreSeleccionado }
                selectedMarca?.let { mainViewModel.onMarcaSeleccionada(it.id) }
            }
        }

        dialogBinding.actvModelo.setOnItemClickListener { parent, _, position, _ ->
            val nombreSeleccionado = parent.getItemAtPosition(position) as String
            if (nombreSeleccionado == "+ Agregar nuevo modelo...") {
                val marcaActual = selectedMarca
                if (marcaActual == null) {
                    Toast.makeText(context, "Primero debe seleccionar una marca", Toast.LENGTH_SHORT).show()
                    return@setOnItemClickListener
                }
                showAddNewModelDialog(marcaActual, dialogBinding) { nuevoModelo ->
                    selectedModelo = nuevoModelo
                }
            } else {
                val modelosActuales = mainViewModel.modelosDeMarcaSeleccionada.value ?: emptyList()
                selectedModelo = modelosActuales.find { it.nombre == nombreSeleccionado }
            }
        }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val matricula = dialogBinding.etMatricula.text.toString().trim().uppercase()
                val ubicacion = dialogBinding.etUbicacion.text.toString().trim().uppercase()
                val detalle = dialogBinding.etDetalle.text.toString().trim()

                if (matricula.isEmpty() || selectedMarca == null || selectedModelo == null) {
                    Toast.makeText(context, "Matrícula, marca y modelo son obligatorios", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentUserProfile = mainViewModel.currentUserProfile.value
                if (currentUserProfile == null) {
                    Toast.makeText(context, "Error: Perfil de usuario no cargado. Intente de nuevo.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val nuevoVehiculo = Vehiculo(
                    matricula = matricula,
                    marcaNombre = selectedMarca!!.nombre,
                    modeloNombre = selectedModelo!!.nombre,
                    ubicacion = ubicacion,
                    detalle = detalle.ifEmpty { null },
                    fechaIngreso = System.currentTimeMillis(),
                    estado = "activo",
                    usuarioRegistrador = currentUserProfile.id,
                    nombreCompletoRegistrador = currentUserProfile.nombreCompleto
                )

                mainViewModel.insertVehiculo(nuevoVehiculo)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showErrorDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    @SuppressLint("RestrictedApi")
    private fun showAddNewBrandDialog(dialogBinding: DialogAddVehicleBinding, onBrandCreated: (Marca) -> Unit) {
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
                        onBrandCreated(marcaCreada)
                    }
                }
            }
            .show()
    }

    @SuppressLint("RestrictedApi")
    private fun showAddNewModelDialog(marca: Marca, dialogBinding: DialogAddVehicleBinding, onModelCreated: (Modelo) -> Unit) {
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
                        onModelCreated(modeloCreado)
                    }
                }
            }
            .show()
    }
}