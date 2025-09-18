package com.demmos.parqueaderoapp.ui.sync

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.demmos.parqueaderoapp.databinding.FragmentSyncBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class SyncFragment : Fragment() {

    private var _binding: FragmentSyncBinding? = null
    private val binding get() = _binding!!

    private lateinit var syncViewModel: SyncViewModel

    private var exportType: String = "normal"

    private var fechaInicio: Long? = null
    private var fechaFin: Long? = null
    private var lastExportedFileUri: Uri? = null

    private val exportarCsvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                lastExportedFileUri = uri
                exportarDatos(uri)
            }
        }
    }

    private val importarCsvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                importarDatos(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()

        arguments?.getString("exportType")?.let {
            exportType = it
        }

        setupUI()
        setupListeners()
        setupObservers()
        handleArguments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViewModel() {
        val vehiculoRepository = VehiculoRepository()
        val usuarioRepository = UsuarioRepository()
        val factory = SyncViewModelFactory(vehiculoRepository, usuarioRepository)
        syncViewModel = ViewModelProvider(this, factory)[SyncViewModel::class.java]
    }

    private fun handleArguments() {
        arguments?.let {
            val fileUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable("fileUriToImport", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable("fileUriToImport")
            }
            fileUri?.let { uri ->
                importarDatos(uri)
                arguments?.clear()
            }
        }
    }

    private fun setupUI() {
        if (exportType == "advanced") {
            binding.cardImport.visibility = View.GONE
            binding.tvExportTitle.text = "Exportar Reporte Avanzado"
            binding.tvExportDescription.text = "Seleccione un rango de fechas para generar la bitácora de todos los movimientos de vehículos."
        } else {
            binding.cardImport.visibility = View.VISIBLE
            binding.tvExportTitle.text = "Exportar Datos a CSV"
            binding.tvExportDescription.text = "Guarda un reporte de vehículos en un archivo CSV."
        }
    }

    private fun setupListeners() {
        binding.btnImportCsv.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "text/*"; addCategory(Intent.CATEGORY_OPENABLE)
            }
            importarCsvLauncher.launch(Intent.createChooser(intent, "Seleccionar archivo CSV"))
        }

        binding.btnSelectDates.setOnClickListener {
            mostrarSelectorDeFechas()
        }

        binding.btnExportCsv.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "text/csv"
                val fileName = if (exportType == "advanced") "ReporteAvanzado" else "ReporteParqueadero"
                putExtra(Intent.EXTRA_TITLE, "${fileName}_${System.currentTimeMillis()}.csv")
            }
            exportarCsvLauncher.launch(intent)
        }
    }

    private fun setupObservers() {
        syncViewModel.syncResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SyncResult.Loading -> Toast.makeText(context, "Procesando...", Toast.LENGTH_SHORT).show()
                is SyncResult.Success -> {
                    if (result.operationTag == "export" && lastExportedFileUri != null) {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        shareFile(lastExportedFileUri!!)
                        lastExportedFileUri = null
                    } else {
                        showResultDialog("Operación Completada", result.message)
                    }
                }
                is SyncResult.Error -> showResultDialog("Error", result.message)
            }
        }
    }

    private fun shareFile(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir Reporte"))
    }

    private fun showResultDialog(title: String, message: String) {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title).setMessage(message)
            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false).show()
    }

    private fun mostrarSelectorDeFechas() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccione un rango")
            .setSelection(Pair(MaterialDatePicker.thisMonthInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds()))
            .build()
        dateRangePicker.show(parentFragmentManager, "DATE_RANGE_PICKER")
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection.first
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
            fechaInicio = calendar.timeInMillis

            calendar.timeInMillis = selection.second
            calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59)
            fechaFin = calendar.timeInMillis

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvDateRange.text = "Del ${sdf.format(Date(fechaInicio!!))} al ${sdf.format(Date(fechaFin!!))}"
            binding.btnExportCsv.isEnabled = true
        }
    }

    private fun importarDatos(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                syncViewModel.importarVehiculosDesdeCSV(inputStream)
            } else {
                Toast.makeText(context, "No se pudo leer el archivo seleccionado.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir el archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportarDatos(uri: Uri) {
        val inicio = fechaInicio; val fin = fechaFin
        if (inicio == null || fin == null) {
            Toast.makeText(context, "Por favor, seleccione un rango de fechas primero.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val outputStream = requireContext().contentResolver.openOutputStream(uri)
            if (outputStream != null) {

                syncViewModel.exportarVehiculosACSV(inicio, fin, outputStream)
            } else {
                Toast.makeText(context, "No se pudo abrir el archivo para escribir.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al escribir el archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}