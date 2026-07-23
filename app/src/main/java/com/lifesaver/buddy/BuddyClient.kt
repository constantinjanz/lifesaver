package com.lifesaver.buddy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the "buddy" Supabase edge function (the only gateway to the pairing/unlock tables).
 * The anon key is publishable (safe to embed); all real authority is the buddy's PIN, checked
 * server-side. No third-party SDK — plain HttpURLConnection + org.json.
 */
object BuddyConfig {
    const val SUPABASE_URL = "https://lrpkvxqdsaavxmuxnjqi.supabase.co"
    const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxycGt2eHFkc2FhdnhtdXhuanFpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ3OTcxMzAsImV4cCI6MjEwMDM3MzEzMH0.t-SdF2LIFNAqAL__nRxiBKFdTJShy4Hga1VDlALU0cU"
    const val FUNCTION = "$SUPABASE_URL/functions/v1/buddy"
}

data class PairingResult(val pairingId: String, val setupUrl: String)
data class RequestResult(val requestId: String, val approveUrl: String)

object BuddyClient {

    suspend fun createPairing(label: String): PairingResult = withContext(Dispatchers.IO) {
        val o = post("create_pairing", JSONObject().put("label", label))
        PairingResult(o.getString("pairing_id"), o.getString("setup_url"))
    }

    suspend fun createRequest(
        pairingId: String,
        appId: String,
        appLabel: String,
        minutes: Int,
        reason: String,
    ): RequestResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("pairing_id", pairingId).put("app_id", appId).put("app_label", appLabel)
            .put("minutes", minutes).put("reason", reason)
        val o = post("create_request", body)
        if (o.has("error")) error(o.getString("error"))
        RequestResult(o.getString("request_id"), o.getString("approve_url"))
    }

    /** pending | approved | denied | not_found. */
    suspend fun status(requestId: String): String = withContext(Dispatchers.IO) {
        val o = get("status", "req=$requestId")
        o.optString("status", o.optString("error", "unknown"))
    }

    private fun post(action: String, body: JSONObject): JSONObject {
        val conn = (URL("${BuddyConfig.FUNCTION}?action=$action").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("content-type", "application/json")
            setRequestProperty("apikey", BuddyConfig.ANON_KEY)
            setRequestProperty("authorization", "Bearer ${BuddyConfig.ANON_KEY}")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        return readJson(conn)
    }

    private fun get(action: String, query: String): JSONObject {
        val conn = (URL("${BuddyConfig.FUNCTION}?action=$action&$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("apikey", BuddyConfig.ANON_KEY)
            setRequestProperty("authorization", "Bearer ${BuddyConfig.ANON_KEY}")
        }
        return readJson(conn)
    }

    private fun readJson(conn: HttpURLConnection): JSONObject {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
        return runCatching { JSONObject(text) }.getOrElse { JSONObject().put("error", "bad_response") }
    }
}
