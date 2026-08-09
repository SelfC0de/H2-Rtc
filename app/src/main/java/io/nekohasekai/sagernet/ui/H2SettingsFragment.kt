package io.nekohasekai.sagernet.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.R

class H2SettingsFragment : Fragment(R.layout.fragment_h2_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.open_advanced_settings).setOnClickListener {
            (activity as? MainActivity)?.displayFragmentWithId(R.id.nav_settings)
        }
    }
}
