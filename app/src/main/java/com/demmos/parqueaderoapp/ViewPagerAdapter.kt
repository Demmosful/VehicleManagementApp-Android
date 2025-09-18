package com.demmos.parqueaderoapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.demmos.parqueaderoapp.ui.history.HistoryFragment
import com.demmos.parqueaderoapp.ui.home.HomeFragment
import com.demmos.parqueaderoapp.ui.sync.SyncFragment
import com.demmos.parqueaderoapp.ui.user.UserFragment

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    // El número total de páginas/fragmentos en nuestra navegación.
    override fun getItemCount(): Int {
        return 4
    }

    // Devuelve el fragmento correcto para cada posición.
    // Esto es lo que conecta el deslizamiento de la pantalla con el contenido.
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> HistoryFragment()
            2 -> SyncFragment()
            3 -> UserFragment()
            else -> throw IllegalStateException("Posición de ViewPager no válida: $position")
        }
    }
}