package io.nekohasekai.sagernet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import io.nekohasekai.sagernet.R

class H2FeaturedFragment : Fragment(R.layout.fragment_h2_featured) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val preferences = requireContext().getSharedPreferences("h2_vps", Context.MODE_PRIVATE)
        val host = view.findViewById<TextInputEditText>(R.id.ssh_host)
        val user = view.findViewById<TextInputEditText>(R.id.ssh_user)
        host.setText(preferences.getString("host", ""))
        user.setText(preferences.getString("user", "root"))
        view.findViewById<MaterialButton>(R.id.install_button).setOnClickListener {
            preferences.edit().putString("host", host.text?.toString()?.trim()).putString("user", user.text?.toString()?.trim()).apply()
            val command = "curl -fsSL https://raw.githubusercontent.com/Oleglog/Olcrtc_manager/master/server-install/olcrtc-setup.sh | sudo bash -s -- --carrier telemost --transport vp8channel"
            val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("H2 Rtc install", command))
            (activity as? MainActivity)?.snackbar(getString(R.string.h2_install_command_copied))?.show()
        }
    }
}
