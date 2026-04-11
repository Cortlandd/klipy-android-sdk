package com.klipy.klipy_ui.picker

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun Throwable.isConnectivityFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (
            current is UnknownHostException ||
            current is ConnectException ||
            current is SocketTimeoutException ||
            current is NoRouteToHostException
        ) {
            return true
        }
        current = current.cause
    }
    return false
}
