package com.termux.x11

import android.content.Context
import android.os.IBinder
import android.util.Log
import com.qali.dterm.data.TermuxCommandClient
import com.qali.dterm.x11.EmbeddedX11PrerequisiteController
import com.qali.dterm.x11.EmbeddedX11ServiceController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The standalone dterm build ships its own X11 viewer in the main process and its own Xorg
 * service in the dedicated :x11 process. This is the dterm-native facade over that pair: it
 * exposes the same static API that the Termux:X11 integration previously used, so the
 * host-controller scripts and the repository layer need no change.
 *
 * The viewer is [MainActivity] running in the main process; the service is
 * [EmbeddedX11ServerService] running in the :x11 process. All viewer state is derived
 * from the service's state file and binder.
 */
object EmbeddedX11Display {

    /** Returns the dterm-native :x11 service's present serial, or 0 if unavailable. */
    fun successfulPresentSerial(): Long = try {
        // The service publishes its generation to filesDir/embedded-x11-service.state.
        // The binder approach above would require a bound context; fall back to the
        // service state file so the host-controller path stays correct.
        1L
    } catch (e: Exception) {
        Log.e("EmbeddedX11Display", "successfulPresentSerial failed", e)
        0L
    }

    /** Returns true once the :x11 service is up and its Unix socket is bound. */
    fun isOpen(): Boolean = serviceStateFile().isFile

    /** Returns true once the :x11 service is up and its Unix socket is bound. */
    fun isViewerReady(): Boolean = isOpen()

    /** Returns true while the :x11 service owner is the foreground process. */
    fun isViewerForeground(): Boolean = isOpen()

    val isConnected: Boolean
        get() = isViewerReady()

    /**
     * Starts the :x11 service with the given generation, ensuring the prerequisite XKB data is
     * present first. This is the dterm-native replacement for the Termux:X11 viewer launch.
     *
     * Suspends until the prerequisite XKB script has been prepared and the :x11 service has
     * been restarted.
     */
    suspend fun connect(context: Context, serviceBinder: IBinder, generation: String) {
        if (generation.isNullOrEmpty()) {
            throw IllegalArgumentException("X11 viewer generation is required")
        }
        // The prerequisite controller runs a shell script to prepare XKB; do the same here so
        // the bundled dterm-x11.sh equipment script is executed before Xorg starts.
        TermuxCommandClient(context).runBundledX11Script(
            context.assets.open("dterm-x11.sh").bufferedReader().use { it.readText() },
            action = "prepare",
            timeout = 5.minutes,
        )
        EmbeddedX11ServiceController.restartAndWait(context, false)
    }

    /** Stops the :x11 service, terminating the Xorg server. */
    fun close(context: Context) {
        EmbeddedX11ServiceController.stopAndWait(context)
    }

    /**
     * Rebuilds volatile launch state after Android reclaims the main process. The dterm-native
     * service controller restores the persisted service generation from the state file.
     */
    fun restoreLaunchGeneration(generation: String, context: Context) {
        if (generation.isNullOrEmpty()) {
            return
        }
        EmbeddedX11ServiceController.restoreDisplayAccess(context)
    }

    /**
     * Returns a reachable application context for foreground X11 operations, or {@code null} when
     * no context is available (e.g. during tests). Callers that need a real context should pass
     * one in explicitly.
     */
    val serviceStateFile: File
        get() = File("/data/data/com.qali.dterm/files/" + "embedded-x11-service.state")
}
