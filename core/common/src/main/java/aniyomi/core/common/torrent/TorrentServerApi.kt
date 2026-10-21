package aniyomi.core.common.torrent

import aniyomi.core.common.torrent.model.Torrent
import aniyomi.core.common.torrent.model.TorrentRequest
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.system.logcat
import java.io.InputStream

class TorrentServerApi(
    private val network: NetworkHelper,
    private val json: Json,
    private val preferences: TorrentPreferences,
) {
    /**
     * TorrServer binds `":<port>"`, i.e. every interface, so loopback is always reachable.
     *
     * The device's LAN address used to be resolved here, but an interface that is up without being
     * routable (VPN, stale Wi-Fi) made [echo] fail, which made the service start the server a second
     * time - and TorrServer kills the process with `os.Exit(1)` when its port is already in use.
     */
    val hostUrl: String
        get() = "http://127.0.0.1:${preferences.torrServerPort().get()}"

    suspend fun shutdown(): String {
        return try {
            network.client.newCall(GET("$hostUrl/shutdown")).awaitSuccess().body.string()
        } catch (e: Exception) {
            logcat(LogPriority.DEBUG, e) { "Error sending shutdown" }
            ""
        }
    }

    suspend fun echo(): String {
        return try {
            network.client.newCall(GET("$hostUrl/echo")).awaitSuccess().body.string()
        } catch (e: Exception) {
            logcat(LogPriority.DEBUG, e) { "Error sending echo" }
            ""
        }
    }

    // / Torrents
    suspend fun addTorrent(
        link: String,
        title: String,
        poster: String = "",
        data: String = "",
        save: Boolean,
    ): Torrent {
        val req = json.encodeToString(
            TorrentRequest(
                "add",
                link = link,
                title = title,
                poster = poster,
                data = data,
                saveToDb = save,
            ),
        )
        val resp = network.client.newCall(
            POST(
                "$hostUrl/torrents",
                body = req.toRequestBody("application/json".toMediaTypeOrNull()),
            ),
        ).awaitSuccess()
        return resp.use { json.decodeFromStream<Torrent>(it.body.byteStream()) }
    }

    suspend fun uploadTorrent(
        file: InputStream,
        title: String,
        save: Boolean = false,
    ): Torrent {
        val bytes = file.use { it.readBytes() }
        val fileRequestBody = bytes.toRequestBody("application/x-bittorrent".toMediaTypeOrNull())

        val requestBody = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            addFormDataPart("file", title, fileRequestBody)
            addFormDataPart("save", save.toString())
            addFormDataPart("title", title)
        }.build()

        val resp = network.client.newCall(
            POST(
                "$hostUrl/torrent/upload",
                body = requestBody,
            ),
        ).awaitSuccess()

        return resp.use { json.decodeFromStream<Torrent>(it.body.byteStream()) }
    }
}
