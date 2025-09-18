package com.demmos.parqueaderoapp.ui.user

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SettingsPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ManageMarcasFragment()
            1 -> ManageModelosFragment()
            else -> throw IllegalStateException("Invalid position for ViewPager")
        }
    }
}