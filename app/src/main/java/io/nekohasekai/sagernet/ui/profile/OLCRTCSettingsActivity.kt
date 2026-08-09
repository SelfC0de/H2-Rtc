/******************************************************************************
 *                                                                            *
 * Copyright (C) 2026  olcRTC for Android contributors                        *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                       *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.      *
 *                                                                            *
 ******************************************************************************/

package io.nekohasekai.sagernet.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.olcrtc.OLCRTCBean
import io.nekohasekai.sagernet.fmt.olcrtc.toUri
import io.nekohasekai.sagernet.ktx.showAllowingStateLoss
import io.nekohasekai.sagernet.widget.QRCodeDialog
import io.nekohasekai.sagernet.widget.SimpleMenuPreference

class OLCRTCSettingsActivity : ProfileSettingsActivity<OLCRTCBean>() {

    companion object {
        private const val MENU_SHARE_QR = 1001
        private const val MENU_SHARE_CLIPBOARD = 1002
    }

    override fun createEntity() = OLCRTCBean()

    override fun OLCRTCBean.init() {
        DataStore.profileName = name
        DataStore.serverOlcrtcProvider = OLCRTCBean.PROVIDER_TELEMOST
        DataStore.serverOlcrtcTransport = OLCRTCBean.TRANSPORT_VP8CHANNEL
        DataStore.serverOlcrtcRoomId = roomId
        DataStore.serverOlcrtcRoomPassword = roomPassword.orEmpty()
        DataStore.serverOlcrtcClientId = clientId.orEmpty()
        DataStore.serverOlcrtcAuthToken = authToken.orEmpty()
        DataStore.serverOlcrtcKeyHex = keyHex
        DataStore.serverOlcrtcDnsServer = dnsServer
        DataStore.serverOlcrtcKeepaliveInterval = keepaliveIntervalSec
    }

    override fun OLCRTCBean.serialize() {
        name = DataStore.profileName
        provider = OLCRTCBean.PROVIDER_TELEMOST
        transport = OLCRTCBean.TRANSPORT_VP8CHANNEL
        roomId = DataStore.serverOlcrtcRoomId
        // Password only persists for SaluteJazz; clear it for the other carriers
        // so a user who switches provider does not silently leave a stale value.
        roomPassword = if (provider == OLCRTCBean.PROVIDER_JITSI) {
            DataStore.serverOlcrtcRoomPassword.orEmpty()
        } else {
            ""
        }
        // clientId is read-only in the UI, but we still round-trip through the
        // DataStore so the value survives configuration changes during edit.
        clientId = DataStore.serverOlcrtcClientId.orEmpty()
        authToken = if (provider == OLCRTCBean.PROVIDER_WB_STREAM) {
            DataStore.serverOlcrtcAuthToken.orEmpty()
        } else {
            ""
        }
        keyHex = DataStore.serverOlcrtcKeyHex
        dnsServer = DataStore.serverOlcrtcDnsServer.ifEmpty { "77.88.8.8:53" }
        vp8Fps = 60
        vp8BatchSize = 64
        keepaliveIntervalSec = DataStore.serverOlcrtcKeepaliveInterval.let { if (it <= 0) 15 else it }
        serverAddress = "olcrtc"
        serverPort = 1
    }

    private fun buildCurrentUri(): String? {
        return try {
            OLCRTCBean().apply { serialize() }.toUri()
        } catch (_: Exception) {
            null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        menu.add(Menu.NONE, MENU_SHARE_QR, Menu.NONE, R.string.share_qr_nfc)
        menu.add(Menu.NONE, MENU_SHARE_CLIPBOARD, Menu.NONE, R.string.action_export_clipboard)
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_SHARE_QR -> {
                val uri = buildCurrentUri()
                if (uri != null) {
                    QRCodeDialog(uri).showAllowingStateLoss(supportFragmentManager)
                } else {
                    Toast.makeText(this, R.string.action_import_err, Toast.LENGTH_SHORT).show()
                }
                true
            }
            MENU_SHARE_CLIPBOARD -> {
                val uri = buildCurrentUri()
                if (uri != null) {
                    val clipboard = getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("olcRTC URI", uri))
                    Toast.makeText(this, R.string.action_export_msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.action_import_err, Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun PreferenceFragmentCompat.createPreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.olcrtc_preferences)

        // Pre-shared key & room password: render dots in summary instead of plaintext.
        findPreference<EditTextPreference>(Key.SERVER_OLCRTC_KEY_HEX)?.summaryProvider =
            PasswordSummaryProvider
        val roomPasswordPref = findPreference<EditTextPreference>(Key.SERVER_OLCRTC_ROOM_PASSWORD)
        roomPasswordPref?.summaryProvider = PasswordSummaryProvider
        val authTokenPref = findPreference<EditTextPreference>(Key.SERVER_OLCRTC_AUTH_TOKEN)
        authTokenPref?.summaryProvider = PasswordSummaryProvider

        roomPasswordPref?.isVisible = false
        authTokenPref?.isVisible = false

        // Per-profile Client ID: read-only, displayed in the Connection section.
        // Tapping copies the value to the clipboard so the user can paste it
        // into the server admin panel for diagnostics. The value originates
        // from the URI/QR (`client_id=`) generated by the server side and is
        // never editable from the UI.
        findPreference<Preference>(Key.SERVER_OLCRTC_CLIENT_ID)?.apply {
            val current = DataStore.serverOlcrtcClientId.orEmpty()
            summary = if (current.isEmpty()) {
                getString(R.string.olcrtc_client_id_missing)
            } else {
                current
            }
            setOnPreferenceClickListener {
                val value = DataStore.serverOlcrtcClientId.orEmpty()
                if (value.isEmpty()) {
                    Toast.makeText(
                        this@OLCRTCSettingsActivity,
                        R.string.olcrtc_client_id_missing,
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    val clipboard = getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("olcRTC clientID", value))
                    Toast.makeText(
                        this@OLCRTCSettingsActivity,
                        R.string.action_export_msg,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                true
            }
        }
    }

}
