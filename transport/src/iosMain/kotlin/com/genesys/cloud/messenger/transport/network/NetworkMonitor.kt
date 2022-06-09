package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_monitor_update_handler_t
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_invalid
import platform.Network.nw_path_status_satisfiable
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_status_unsatisfied
import platform.Network.nw_path_t
import platform.darwin.dispatch_get_main_queue

internal actual class NetworkMonitor(
    val log: Log,
) {

    private var networkStateListener: NetworkStateListener? = null
    private var networkPathMonitor: nw_path_monitor_t = null

    actual fun setNetworkStateListener(networkStateListener: NetworkStateListener) {
        this.networkStateListener = networkStateListener
    }

    actual fun start() {
        val updateHandler = object : nw_path_monitor_update_handler_t {
            override fun invoke(path: nw_path_t) {
                log.i { "Network update: $path" }
                when (nw_path_get_status(path)) {
                    nw_path_status_satisfied -> {
                        // The path is available to establish connections and send data.
                        networkStateListener?.onStateChanged(NetworkState.Available)
                    }
                    nw_path_status_satisfiable -> {
                        // The path is not currently available, but establishing a new connection may activate the path.
                        networkStateListener?.onStateChanged(NetworkState.Unavailable)
                    }
                    nw_path_status_unsatisfied -> {
                        // The path is not available for use.
                        networkStateListener?.onStateChanged(NetworkState.Unavailable)
                    }
                    nw_path_status_invalid -> {
                        // The path is not valid.
                        networkStateListener?.onStateChanged(NetworkState.Unavailable)
                    }
                }
            }
        }

        networkPathMonitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(
            monitor = networkPathMonitor,
            queue = dispatch_get_main_queue()
        )
        nw_path_monitor_set_update_handler(
            monitor = networkPathMonitor,
            update_handler = updateHandler
        )
        nw_path_monitor_start(networkPathMonitor)
    }

    actual fun stop() {
        networkStateListener = null
        nw_path_monitor_cancel(networkPathMonitor)
    }

}
