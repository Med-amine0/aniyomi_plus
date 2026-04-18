package eu.kanade.tachiyomi.torrentServer

import eu.kanade.tachiyomi.torrentServer.model.FileStat
import eu.kanade.tachiyomi.torrentServer.model.Torrent
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder

object TorrentServerUtils {
    private var _preferences: TorrentServerPreferences? = null
    private var _port: String = "8090"

    fun init(preferences: TorrentServerPreferences) {
        _preferences = preferences
        _port = preferences.port().get()
    }

    private fun getPreferences(): TorrentServerPreferences {
        return _preferences ?: throw IllegalStateException("TorrentServerUtils not initialized. Call init() first.")
    }

    val hostUrl: String
        get() = "http://${getLocalIpAddress()}:$_port"

    private val animeTrackers: String
        get() = getPreferences().trackers().get().split("\n").joinToString(",\n")

    fun setTrackersList() {
        try {
            torrServer.TorrServer.addTrackers(animeTrackers)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Failed to set trackers: ${e.message}" }
        }
    }

    fun getTorrentPlayLink(torr: Torrent, index: Int): String {
        val file = findFile(torr, index)
        val name = file?.let { File(it.path).name } ?: torr.title
        return "$hostUrl/stream/${name.urlEncode()}?link=${torr.hash}&index=$index&play"
    }

    private fun findFile(torrent: Torrent, index: Int): FileStat? {
        torrent.file_stats?.forEach {
            if (it.id == index) {
                return it
            }
        }
        return null
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (ex: Exception) {
            logcat(LogPriority.DEBUG) { "Error getting local IP address" }
        }
        return "127.0.0.1"
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "utf8")
}