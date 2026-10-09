package com.replaymac.companion.data.network

import java.io.EOFException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Turns socket exceptions into wording a person can act on. */
object ConnectionErrors {

    fun describe(error: Throwable, wasConnected: Boolean): String = when {
        wasConnected -> "The connection to your Mac was interrupted."
        error is EOFException -> "ReplayCap closed the connection."
        error is SocketTimeoutException -> "Your Mac didn't respond. Check it's awake and on the same network."
        error is ConnectException -> "Nothing is listening there. Check Companion Server is on in ReplayCap."
        error is NoRouteToHostException -> "Your Mac isn't reachable from this network."
        error is UnknownHostException -> "That address couldn't be found."
        else -> error.message?.takeIf { it.isNotBlank() } ?: "Connection failed"
    }
}
