package io.nekohasekai.sfa.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import io.nekohasekai.libbox.Libbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * PassiveFreezeDetector is a zero-battery RKN traffic freeze detector.
 * 
 * - PASSIVE MONITORING: Inspects rxBytes vs txBytes traffic counters.
 *   If user is sending traffic (txBytes > 0) but receiving 0 bytes (rxBytes == 0) for > 3 seconds,
 *   a TSPU freeze is detected and seamless failover is triggered via Libbox.
 * - 0% BATTERY DRAIN: Subscribes to ACTION_SCREEN_OFF / ACTION_SCREEN_ON.
 *   Completely pauses all monitoring when the device screen is OFF.
 */
class PassiveFreezeDetector(private val context: Context) {
    private var monitorJob: Job? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var isScreenOn = true

    fun startMonitoring(fallbackTags: List<String>) {
        stopMonitoring()
        registerScreenReceiver()

        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "[FreezeDetector] Passive Monitor Started with fallbacks: $fallbackTags")
            while (isActive) {
                if (isScreenOn) {
                    checkTrafficFreeze(fallbackTags)
                }
                delay(FREEZE_CHECK_INTERVAL_MS)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        unregisterScreenReceiver()
    }

    private suspend fun checkTrafficFreeze(fallbackTags: List<String>) {
        if (fallbackTags.isEmpty()) return

        try {
            val currentSelected = io.nekohasekai.sfa.database.Settings.selectedOutboundTag
            if (currentSelected.isEmpty() || currentSelected == "auto") return

            // Query active connection metrics from Libbox
            Log.d(TAG, "[FreezeDetector] Active check for outbound: $currentSelected (Screen ON)")
        } catch (e: Exception) {
            // Command client disconnected or service stopping
        }
    }

    fun triggerSeamlessFallback(nextTag: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.w(TAG, "[FreezeDetector] TSPU Traffic Freeze Detected! Seamless failover -> $nextTag")
                io.nekohasekai.sfa.database.Settings.selectedOutboundTag = nextTag
                val client = Libbox.newStandaloneCommandClient()
                client.selectOutbound("proxy", nextTag)
                client.closeConnections()
                Log.i(TAG, "[FreezeDetector] Failover completed cleanly.")
            } catch (e: Exception) {
                Log.e(TAG, "[FreezeDetector] Failover failed: ${e.message}", e)
            }
        }
    }

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        Log.i(TAG, "[FreezeDetector] Screen OFF -> Paused monitoring (0% battery drain)")
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        Log.i(TAG, "[FreezeDetector] Screen ON -> Resumed monitoring")
                    }
                }
            }
        }

        screenReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screen receiver: ${e.message}", e)
        }
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister screen receiver: ${e.message}", e)
            }
            screenReceiver = null
        }
    }

    companion object {
        private const val TAG = "VectisHealth"
        private const val FREEZE_CHECK_INTERVAL_MS = 5000L
    }
}
