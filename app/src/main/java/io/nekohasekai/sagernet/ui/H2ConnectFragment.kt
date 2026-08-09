package io.nekohasekai.sagernet.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.bg.BaseService

class H2ConnectFragment : Fragment(R.layout.fragment_h2_connect) {
    private var status: TextView? = null
    private var badge: TextView? = null
    private var button: MaterialButton? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        status = view.findViewById(R.id.connection_status)
        badge = view.findViewById(R.id.connection_badge)
        button = view.findViewById(R.id.connect_button)
        button?.setOnClickListener { (activity as? MainActivity)?.toggleH2Connection() }
        render((activity as? MainActivity)?.state ?: BaseService.State.Idle)
    }

    fun render(state: BaseService.State) {
        val connected = state == BaseService.State.Connected
        status?.setText(if (connected) R.string.h2_connected else R.string.h2_disconnected)
        badge?.setText(if (connected) R.string.h2_connected else R.string.h2_ready)
        button?.setText(if (connected) R.string.h2_disconnect else R.string.h2_connect)
    }
}
