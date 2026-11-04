package com.alex.tvremote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val ip = NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress ?: "unknown"
        val info = TextView(this).apply {
            text = "On your phone, open:\nhttp://$ip:8080\n\nFirst time: turn on TV Remote in Accessibility."
            textSize = 28f; setPadding(80, 80, 80, 40)
        }
        val btn = Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener {
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (e: Exception) {
                    text = "Not available here - use Activity Launcher"
                }
            }
        }
        setContentView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(info); addView(btn) })
    }
}
