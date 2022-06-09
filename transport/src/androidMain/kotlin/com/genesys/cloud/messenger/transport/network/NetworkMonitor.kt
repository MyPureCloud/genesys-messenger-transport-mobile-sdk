package com.genesys.cloud.messenger.transport.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log

internal actual class NetworkMonitor(val context: Context) {

    private var networkStateListener: NetworkStateListener? = null
    private val connectivityManager: ConnectivityManager? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("Connectivity status", "Connected")
            networkStateListener?.onStateChanged(NetworkState.Available)
        }

        override fun onLost(network: Network) {
            Log.d("Connectivity status", "Disconnected")
            networkStateListener?.onStateChanged(NetworkState.Unavailable)
        }
    }

    actual fun setNetworkStateListener(networkStateListener: NetworkStateListener) {
        this.networkStateListener = networkStateListener
    }

    actual fun start() {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                val currentNetwork = connectivityManager.activeNetwork
                if (currentNetwork == null) {
                    networkStateListener?.onStateChanged(NetworkState.Unavailable)
                    Log.d("Connectivity status", "Disconnected")
                }
            } else {
                TODO("Handle lower level android api here.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            networkStateListener?.onStateChanged(NetworkState.Unavailable)
        }
    }

    actual fun stop() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
        networkStateListener = null
    }

}