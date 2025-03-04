package com.github.teranes10.androidutils.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellIdentity
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrength
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections


object NetworkUtil {
    private const val NETWORK_UTIL_CONNECTION_TAG = 123123
    private const val TAG = "NetworkUtil"

    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getSSID(wifiInfo: WifiInfo): String = wifiInfo.ssid

    fun getBSSID(wifiInfo: WifiInfo): String = wifiInfo.bssid

    fun getLinkSpeed(wifiInfo: WifiInfo): String = "${wifiInfo.linkSpeed}Mbps"

    @SuppressLint("DefaultLocale")
    suspend fun getIpAddress(connectivityManager: ConnectivityManager, wifiInfo: WifiInfo?): String = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val network = connectivityManager.activeNetwork ?: return@withContext ""
            val linkProperties: LinkProperties = connectivityManager.getLinkProperties(network) ?: return@withContext ""

            for (linkAddress in linkProperties.linkAddresses) {
                val address = linkAddress.address
                if (address is Inet4Address) {
                    return@withContext address.hostAddress ?: ""
                }
            }
        } else {
            wifiInfo?.let {
                val ip = it.ipAddress
                return@withContext String.format(
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    ip shr 8 and 0xff,
                    ip shr 16 and 0xff,
                    ip shr 24 and 0xff
                )
            }
        }

        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in networkInterfaces) {
                val inetAddresses = Collections.list(networkInterface.inetAddresses)
                for (address in inetAddresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return@withContext address.hostAddress ?: ""
                    }
                }
            }
        } catch (ignored: Exception) {}

        return@withContext ""
    }

    fun getWifiInfo(context: Context): WifiInfo? {
        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val network = connectivityManager.activeNetwork ?: return null
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
            return networkCapabilities.transportInfo as? WifiInfo
        } else {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wifiManager.connectionInfo
        }
    }

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getSignalStrength(wifiInfo: WifiInfo): String = "${wifiInfo.rssi}dB"

    fun getCellInfo(context: Context): CellTowerInfo? {
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val cellInfoList = telephonyManager.allCellInfo ?: return null

        for (info in cellInfoList) {
            if (!info.isRegistered) continue

            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info is CellInfoGsm -> {
                    val identity = info.cellIdentity
                    createCellTowerInfo(
                        "GSM",
                        identity.mccString,
                        identity.mncString,
                        identity.cid,
                        identity.lac,
                        info.cellSignalStrength.dbm,
                        -1
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info is CellInfoLte -> {
                    val identity = info.cellIdentity
                    createCellTowerInfo(
                        "LTE",
                        identity.mccString,
                        identity.mncString,
                        identity.ci,
                        identity.tac,
                        info.cellSignalStrength.dbm,
                        identity.pci
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && info is CellInfoNr -> {
                    val identity = info.cellIdentity as CellIdentityNr
                    createCellTowerInfo("NR",
                        identity.mccString,
                        identity.mncString,
                        identity.nci.toInt(),
                        identity.tac,
                        info.cellSignalStrength.dbm,
                        identity.pci)
                }

                else -> null
            }
        }
        return null
    }

    private fun createCellTowerInfo(
        type: String,
        mcc: String?,
        mnc: String?,
        id: Int,
        area: Int,
        signalStrength: Int,
        pci: Int
    ): CellTowerInfo {
        return CellTowerInfo(type, id.toString(), "$signalStrength dBm", area.toString(), mcc ?: "", mnc ?: "", pci)
    }

    suspend fun isReachable(url: String, timeout: Int): Boolean = withContext(Dispatchers.IO) {
        TrafficStats.setThreadStatsTag(NETWORK_UTIL_CONNECTION_TAG)
        return@withContext try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.connect()
            isResponseSuccessful(connection.responseCode)
        } catch (e: Exception) {
            false
        } finally {
            TrafficStats.clearThreadStatsTag()
        }
    }

    private fun isResponseSuccessful(responseCode: Int): Boolean = responseCode in 200..299

    suspend fun getPublicIpAddress(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("https://api.ipify.org")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode == 200) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readLine() }
            } else "0.0.0.0"
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    data class CellTowerInfo(
        val type: String,
        val towerId: String,
        val signalStrength: String,
        val lac: String,
        val mcc: String = "",
        val mnc: String = "",
        val pci: Int = -1
    )
}
