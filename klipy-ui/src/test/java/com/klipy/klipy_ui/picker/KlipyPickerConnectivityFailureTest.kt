package com.klipy.klipy_ui.picker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class KlipyPickerConnectivityFailureTest {

    @Test
    fun `known connectivity exceptions are detected`() {
        assertTrue(UnknownHostException().isConnectivityFailure())
        assertTrue(ConnectException().isConnectivityFailure())
        assertTrue(SocketTimeoutException().isConnectivityFailure())
        assertTrue(NoRouteToHostException().isConnectivityFailure())
    }

    @Test
    fun `nested connectivity exceptions are detected`() {
        assertTrue(IllegalStateException(UnknownHostException()).isConnectivityFailure())
    }

    @Test
    fun `non connectivity exceptions are ignored`() {
        assertFalse(IOException("generic io").isConnectivityFailure())
        assertFalse(IllegalArgumentException("bad request").isConnectivityFailure())
    }
}
