package de.olti.lunaoverlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }
        layout.addView(TextView(this).apply {
            text = "Luna Overlay – Zustandstest\n\nDie vier Zustände lassen sich hier einzeln prüfen. Die automatische Kopplung an ChatGPT und echte Bildsequenzen fehlen noch."
            textSize = 20f
        })
        layout.addView(Button(this).apply {
            text = "Overlay erlauben"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        })
        fun stateButton(label: String, action: String) {
            layout.addView(Button(this).apply {
                text = label
                setOnClickListener {
                    if (Settings.canDrawOverlays(this@MainActivity)) {
                        startService(Intent(this@MainActivity, OverlayService::class.java).setAction(action))
                    } else {
                        Toast.makeText(this@MainActivity, "Bitte zuerst Overlay erlauben.", Toast.LENGTH_LONG).show()
                    }
                }
            })
        }
        stateButton("Luna starten / Ruhezustand", OverlayService.ACTION_IDLE)
        stateButton("Zuhören testen", OverlayService.ACTION_LISTENING)
        stateButton("Denken testen", OverlayService.ACTION_THINKING)
        stateButton("Antworten testen", OverlayService.ACTION_SPEAKING)
        layout.addView(Button(this).apply {
            text = "Luna beenden"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }
}
