package com.example.mediquick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                val status = getNetworkStatus(context)
                val local = Intent(ACTION_NETWORK_CHANGED).apply {
                    setPackage(context.packageName)   // ← add this
                    putExtra(EXTRA_IS_CONNECTED, status.isConnected)
                    putExtra(EXTRA_TYPE, status.type)
                }
                context.sendBroadcast(local)
            }
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isOn = intent.getBooleanExtra("state", false)
                val local = Intent(ACTION_AIRPLANE_MODE_CHANGED).apply {
                    setPackage(context.packageName)   // ← add this
                    putExtra(EXTRA_AIRPLANE_ON, isOn)
                }
                context.sendBroadcast(local)
            }
        }
    }

    data class NetworkStatus(val isConnected: Boolean, val type: String)

    companion object {
        const val ACTION_NETWORK_CHANGED       = "com.example.mediquick.NETWORK_CHANGED"
        const val ACTION_AIRPLANE_MODE_CHANGED = "com.example.mediquick.AIRPLANE_MODE_CHANGED"
        const val EXTRA_IS_CONNECTED           = "is_connected"
        const val EXTRA_TYPE                   = "network_type"
        const val EXTRA_AIRPLANE_ON            = "airplane_on"

        fun getNetworkStatus(context: Context): NetworkStatus {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return NetworkStatus(false, "None")
            val caps    = cm.getNetworkCapabilities(network) ?: return NetworkStatus(false, "None")
            return when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> NetworkStatus(true, "WiFi")
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkStatus(true, "Mobile Data")
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus(true, "Ethernet")
                else -> NetworkStatus(false, "None")
            }
        }
    }
}