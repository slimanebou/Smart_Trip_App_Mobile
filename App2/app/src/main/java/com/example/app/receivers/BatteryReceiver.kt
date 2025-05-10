package com.example.app.receivers

import android.content.BroadcastReceiver
import android.content.Intent
import android.os.BatteryManager
import android.content.Context
import android.util.Log

class BatteryReceiver(private val onLowBattery: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // L'état actuel de la batterie
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
        // L'échelle max de la batterie
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        // Le pourcentage
        val batteryPct = (level / scale.toFloat()) * 100

        Log.d("BATTERY", "Battery level: $batteryPct%")
        // Si la betterie est moins de 15%
        if (batteryPct <= 15) {
            onLowBattery()
        }
    }
}
