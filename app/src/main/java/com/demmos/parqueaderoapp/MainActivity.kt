package com.demmos.parqueaderoapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.demmos.parqueaderoapp.databinding.ActivityMainBinding
import com.demmos.parqueaderoapp.ui.MainViewModel
import com.demmos.parqueaderoapp.ui.MainViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewPagerAdapter: ViewPagerAdapter


    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(VehiculoRepository(), UsuarioRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tu lógica de ViewModel y Toolbar se mantiene.
        mainViewModel.currentUserProfile.observe(this) { /* Observador vacío para activar la carga inicial */ }
        setSupportActionBar(binding.toolbar)

        // --- CAMBIO: LÓGICA DE NAVEGACIÓN COMPLETAMENTE REEMPLAZADA ---
        // Se elimina toda la configuración del NavController, NavHostFragment y AppBarConfiguration.
        // Se implementa la lógica de sincronización manual entre ViewPager2 y BottomNavigationView.

        // 1. Configurar el ViewPager2 con el nuevo adaptador.
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        // 2. Sincronizar el clic en el BottomNavigationView para que cambie la página del ViewPager2.
        binding.navView.setOnItemSelectedListener { item ->
            val pageIndex = when (item.itemId) {
                R.id.navigation_home -> 0
                R.id.navigation_history -> 1
                R.id.navigation_sync -> 2
                R.id.navigation_user -> 3
                else -> -1
            }
            if (pageIndex != -1) {
                binding.viewPager.setCurrentItem(pageIndex, true)
                true
            } else {
                false
            }
        }

        // 3. Sincronizar el deslizamiento del ViewPager2 para que se marque el ícono correcto en el BottomNavigationView.
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Actualiza el ícono seleccionado en la barra inferior.
                binding.navView.menu.getItem(position).isChecked = true
                // Actualiza el título de la Toolbar.
                binding.toolbar.title = binding.navView.menu.getItem(position).title
            }
        })

        handleIncomingIntent(intent)
    }

    // CAMBIO: onSupportNavigateUp ya no es necesario con esta arquitectura.
    // El ViewPager2 maneja la pila de fragmentos de forma plana.
    // override fun onSupportNavigateUp(): Boolean { ... }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val fileUri: Uri = intent.data!!
            val bundle = Bundle().apply {
                putParcelable("fileUriToImport", fileUri)
            }
            // CAMBIO: En lugar de usar NavController.navigate, simplemente cambiamos a la página
            // de Sincronización. El fragmento SyncFragment deberá ser adaptado para
            // recibir los argumentos de una manera diferente (ej. a través del MainViewModel).
            // Por ahora, solo aseguramos que navegue a la pestaña correcta.
            binding.viewPager.setCurrentItem(2) // 2 es la posición del SyncFragment

            // Para pasar los datos, lo haremos a través del ViewModel para desacoplar la lógica.
            // (Esta es una mejora arquitectónica).
            // mainViewModel.setFileToImport(fileUri) // <- Deberías añadir esta función en tu MainViewModel
        }
    }
}