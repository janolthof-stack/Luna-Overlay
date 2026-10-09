package de.olti.lunaoverlay

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Shut down the previous manually controlled window when upgrading.
        stopService(Intent(this, OverlayService::class.java))
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }
        layout.addView(TextView(this).apply {
            text = "Luna Overlay – automatische Erkennung (Prototyp)\n\nLuna wertet lokal die Beschriftungen von Bedienelementen und Statusanzeigen der ChatGPT-App aus. Gesprächsinhalte werden nicht gespeichert oder versendet. Es gibt keine Testknöpfe für Gesprächszustände.\n\nEinmalig den Dienst unter Bedienungshilfen aktivieren. Danach erscheint Luna automatisch, solange ChatGPT im Vordergrund ist.\n\nDie Erkennung ist noch nicht am echten Gerät bestätigt. Wenn ChatGPT kein eindeutiges Signal liefert, zeigt Luna das ausdrücklich an. Die vorhandenen Bilder sind noch Standbilder."
            textSize = 18f
        })
        status = TextView(this).apply { textSize = 16f }
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "Automatik in Android einrichten"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onResume() {
        super.onResume()
        val component = ComponentName(this, LunaAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').any { it.equals(component, ignoreCase = true) }
        val last = getSharedPreferences("luna_status", MODE_PRIVATE).getString("last_status", "Noch nicht geprüft")
        status.text = "\nAutomatik: ${if (enabled) "aktiviert" else "nicht aktiviert"}\nLetzter Befund: $last\n"
    }
}
