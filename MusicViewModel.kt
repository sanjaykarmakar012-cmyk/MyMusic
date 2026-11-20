package com.mymusic.app

import android.app.Application
import android.content.Context
import android.text.Html
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Track(val id: String, val title: String, val channel: String, val thumb: String)

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val sp = app.getSharedPreferences("mymusic", Context.MODE_PRIVATE)

    // ---- customisation (persisted) ----
    var themeMode by mutableIntStateOf(sp.getInt("mode", 0)); private set      // 0 system, 1 light, 2 dark
    var accent by mutableLongStateOf(sp.getLong("accent", 0xFFE53935)); private set
    var amoled by mutableStateOf(sp.getBoolean("amoled", false)); private set
    var dynamic by mutableStateOf(sp.getBoolean("dynamic", false)); private set
    var apiKey by mutableStateOf(sp.getString("key", "") ?: ""); private set
    var region by mutableStateOf(sp.getString("region", "US") ?: "US"); private set

    // ---- content ----
    var home by mutableStateOf<List<Track>>(emptyList()); private set
    var results by mutableStateOf<List<Track>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    // ---- playback queue ----
    var queue by mutableStateOf<List<Track>>(emptyList()); private set
    var index by mutableIntStateOf(-1); private set
    val current: Track? get() = queue.getOrNull(index)

    init { loadHome() }

    fun updateThemeMode(v: Int) { themeMode = v; sp.edit().putInt("mode", v).apply() }
    fun updateAccent(v: Long) { accent = v; sp.edit().putLong("accent", v).apply() }
    fun updateAmoled(v: Boolean) { amoled = v; sp.edit().putBoolean("amoled", v).apply() }
    fun updateDynamic(v: Boolean) { dynamic = v; sp.edit().putBoolean("dynamic", v).apply() }
    fun saveApi(key: String, reg: String) {
        apiKey = key
        region = reg.ifBlank { "US" }
        sp.edit().putString("key", apiKey).putString("region", region).apply()
        loadHome()
    }

    fun play(list: List<Track>, i: Int) { queue = list; index = i }
    fun next() { if (index + 1 < queue.size) index++ }
    fun previous() { if (index > 0) index-- }

    fun loadHome() = fetch(
        "videos?part=snippet&chart=mostPopular&videoCategoryId=10&maxResults=30&regionCode=$region"
    ) { home = it }

    fun search(q: String) {
        if (q.isBlank()) return
        fetch(
            "search?part=snippet&type=video&videoCategoryId=10&maxResults=30&q=" +
                URLEncoder.encode(q, "UTF-8")
        ) { results = it }
    }

    private fun fetch(path: String, onOk: (List<Track>) -> Unit) {
        if (apiKey.isBlank()) { error = "Add your YouTube Data API key in Settings."; return }
        viewModelScope.launch {
            loading = true; error = null
            try { onOk(withContext(Dispatchers.IO) { request(path) }) }
            catch (e: Exception) { error = e.message ?: "Network error" }
            loading = false
        }
    }

    private fun request(path: String): List<Track> {
        val url = "https://www.googleapis.com/youtube/v3/$path&key=" + URLEncoder.encode(apiKey, "UTF-8")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000; conn.readTimeout = 10000
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            val msg = runCatching { JSONObject(body).getJSONObject("error").getString("message") }
                .getOrDefault("HTTP $code")
            throw Exception(msg)
        }
        val items = JSONObject(body).getJSONArray("items")
        return (0 until items.length()).mapNotNull { i ->
            val o = items.getJSONObject(i)
            val idv = o.opt("id")
            val vid = if (idv is JSONObject) idv.optString("videoId") else idv?.toString() ?: ""
            if (vid.isBlank()) return@mapNotNull null
            val sn = o.getJSONObject("snippet")
            Track(
                vid,
                Html.fromHtml(sn.optString("title"), Html.FROM_HTML_MODE_LEGACY).toString(),
                sn.optString("channelTitle"),
                sn.optJSONObject("thumbnails")?.optJSONObject("medium")?.optString("url") ?: ""
            )
        }.distinctBy { it.id }
    }
}
