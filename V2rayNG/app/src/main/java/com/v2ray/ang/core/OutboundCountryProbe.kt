package com.v2ray.ang.core

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.SpeedtestManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

/**
 * Owns only the short-lived Xray instances used to observe the egress country of one real-ping
 * config. `CoreServiceManager` owns the long-lived VPN/proxy/root daemon; instances here are
 * started and closed inside a single probe and never change that daemon's state.
 *
 * The country comes from a real request to the configured IP API routed through that config's
 * outbounds. It is never derived from the config's address, remarks, or subscription name.
 */
object OutboundCountryProbe {
    private const val PROBE_INBOUND_TAG = "real-ping-country-probe"
    private const val PROBE_PORT_BASE = 20808
    private const val PROBE_PORT_SPAN = 256

    private val portCursor = AtomicInteger(0)
    private val portLock = Any()
    private val portsInUse = HashSet<Int>()
    // gomobile's NewCoreController registers a process-wide log handler; serialize its creation.
    private val controllerLock = Any()

    /**
     * Returns the raw country reported by the IP API for `speedtestConfigJson`, or null when the
     * probe cannot be completed. A failure leaves no country rather than a guessed one.
     */
    fun probe(speedtestConfigJson: String, guid: String): String? {
        val port = acquirePort() ?: return null
        var controller: CoreController? = null
        try {
            val probeConfig = buildProbeConfig(speedtestConfigJson, port)
            if (probeConfig == null) {
                LogUtil.w(AppConfig.TAG, "RealPing egress-country probe: unusable speedtest config for guid=$guid")
                return null
            }
            val probeController = synchronized(controllerLock) {
                CoreNativeManager.newCoreController(ProbeCallback())
            }
            controller = probeController
            probeController.startLoop(probeConfig, 0)
            return SpeedtestManager.getRemoteIPInfo(port)
                ?.country
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        } catch (error: Exception) {
            LogUtil.e(AppConfig.TAG, "RealPing egress-country probe failed for guid=$guid", error)
            return null
        } finally {
            try {
                controller?.stopLoop()
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "RealPing egress-country probe cleanup failed for guid=$guid", error)
            }
            releasePort(port)
        }
    }

    /**
     * Replaces every inbound with a loopback HTTP inbound, so the probe request is dispatched by
     * this config's outbounds. Normal speedtest configs already have no inbounds or routing rules;
     * custom configs keep their own routing rules, which is the routing a real user would get.
     */
    internal fun buildProbeConfig(speedtestConfigJson: String, port: Int): String? {
        val root = runCatching {
            JsonParser.parseString(speedtestConfigJson).takeIf { it.isJsonObject }?.asJsonObject
        }.getOrNull() ?: return null
        val inbound = JsonObject().apply {
            addProperty("tag", PROBE_INBOUND_TAG)
            addProperty("listen", AppConfig.LOOPBACK)
            addProperty("port", port)
            addProperty("protocol", "http")
            add("settings", JsonObject())
        }
        root.add("inbounds", JsonArray().apply { add(inbound) })
        return JsonUtil.toJson(root)
    }

    private fun acquirePort(): Int? {
        synchronized(portLock) {
            repeat(PROBE_PORT_SPAN) {
                val offset = Math.floorMod(portCursor.getAndIncrement(), PROBE_PORT_SPAN)
                val port = PROBE_PORT_BASE + offset
                if (port !in portsInUse && isPortFree(port)) {
                    portsInUse.add(port)
                    return port
                }
            }
            return null
        }
    }

    private fun isPortFree(port: Int): Boolean = try {
        ServerSocket(port, 1, InetAddress.getByName(AppConfig.LOOPBACK)).use { true }
    } catch (_: IOException) {
        false
    }

    private fun releasePort(port: Int) {
        synchronized(portLock) {
            portsInUse.remove(port)
        }
    }

    private class ProbeCallback : CoreCallbackHandler {
        override fun startup(): Long = 0
        override fun shutdown(): Long = 0
        override fun onEmitStatus(l: Long, s: String?): Long = 0
    }
}
