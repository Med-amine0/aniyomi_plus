package eu.kanade.tachiyomi.torrentServer

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.torrentServer.model.Torrent
import eu.kanade.tachiyomi.torrentServer.model.TorrentRequest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.io.InputStream

object TorrentServerApi {
    private var networkHelper: NetworkHelper? = null

    fun init(network: NetworkHelper) {
        networkHelper = network
    }

    private fun getNetwork(): NetworkHelper {
        return networkHelper ?: throw IllegalStateException("TorrentServerApi not initialized. Call init() first.")
    }

    @Suppress("TooGenericExceptionCaught")
    fun echo(): String {
        return try {
            getNetwork().client.newCall(okhttp3.Request.Builder().url("${TorrentServerUtils.hostUrl}/echo").build()).execute().body.string()
        } catch (e: Exception) {
            ""
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun shutdown(): String {
        return try {
            getNetwork().client.newCall(okhttp3.Request.Builder().url("${TorrentServerUtils.hostUrl}/shutdown").build()).execute().body.string()
        } catch (e: Exception) {
            ""
        }
    }

    fun addTorrent(
        link: String,
        title: String,
        poster: String = "",
        data: String = "",
        save: Boolean,
    ): Torrent {
        val req = TorrentRequest(
            action = "add",
            link = link,
            title = title,
            poster = poster,
            data = data,
            saveToDb = save,
        ).toString()
        val resp = getNetwork().client.newCall(
            okhttp3.Request.Builder()
                .url("${TorrentServerUtils.hostUrl}/torrents")
                .post(req.toRequestBody("application/json".toMediaTypeOrNull()))
                .build(),
        ).execute()
        return Json.decodeFromString(Torrent.serializer(), resp.body.string())
    }

    fun getTorrent(hash: String): Torrent {
        val req = TorrentRequest(action = "get", hash = hash).toString()
        val resp = getNetwork().client.newCall(
            okhttp3.Request.Builder()
                .url("${TorrentServerUtils.hostUrl}/torrents")
                .post(req.toRequestBody("application/json".toMediaTypeOrNull()))
                .build(),
        ).execute()
        return Json.decodeFromString(Torrent.serializer(), resp.body.string())
    }

    fun remTorrent(hash: String) {
        val req = TorrentRequest(action = "rem", hash = hash).toString()
        getNetwork().client.newCall(
            okhttp3.Request.Builder()
                .url("${TorrentServerUtils.hostUrl}/torrents")
                .post(req.toRequestBody("application/json".toMediaTypeOrNull()))
                .build(),
        ).execute()
    }

    fun listTorrent(): List<Torrent> {
        val req = TorrentRequest(action = "list").toString()
        val resp = getNetwork().client.newCall(
            okhttp3.Request.Builder()
                .url("${TorrentServerUtils.hostUrl}/torrents")
                .post(req.toRequestBody("application/json".toMediaTypeOrNull()))
                .build(),
        ).execute()
        return Json.decodeFromString<List<Torrent>>(resp.body.string())
    }

    fun uploadTorrent(
        file: InputStream,
        title: String,
        poster: String,
        data: String,
        save: Boolean,
    ): Torrent {
        val resp = Jsoup.connect("${TorrentServerUtils.hostUrl}/torrent/upload")
            .data("title", title)
            .data("poster", poster)
            .data("data", data)
            .data("save", save.toString())
            .data("file1", "filename", file)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)
            .post()
        return Json.decodeFromString(Torrent.serializer(), resp.body().text())
    }
}