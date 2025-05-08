package com.example.app.receivers

import android.content.BroadcastReceiver
import android.content.Intent
import android.os.BatteryManager
import android.content.Context
import android.util.Log

class BatteryReceiver(private val onLowBattery: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = (level / scale.toFloat()) * 100

        Log.d("BATTERY", "Battery level: $batteryPct%")
        if (batteryPct <= 15) {
            onLowBattery()
        }
    }
}
