package com.farmmarket

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(msg: String, val unavailable: Boolean = false, val code: Int = 0) : Exception(msg)

/** Same REST API as the Node backend. Blocking calls: run them on Dispatchers.IO. */
class Api(private val store: Store) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build()
    private val json = "application/json".toMediaType()

    private fun call(method: String, path: String, body: JSONObject? = null): JSONObject {
        try {
            val b = Request.Builder().url(store.baseUrl + path)
            store.token?.let { b.header("Authorization", "Bearer $it") }
            if (method == "POST") b.post((body ?: JSONObject()).toString().toRequestBody(json)) else b.get()
            http.newCall(b.build()).execute().use { r ->
                val text = r.body?.string().orEmpty()
                val j = try { JSONObject(text) } catch (e: Exception) { null }
                if (!r.isSuccessful) {
                    val m = j?.optString("message").orEmpty()
                    throw ApiException(m.ifEmpty { "Server error ${r.code}" }, false, r.code)
                }
                return j ?: throw ApiException("Bad response")
            }
        } catch (e: IOException) {
            throw ApiException("Server unavailable", true)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid server address", true)
        }
    }

    /** Returns the user's name and stores the JWT. */
    fun login(phone: String, password: String): String {
        val d = call("POST", "/auth/login", JSONObject().put("phone", phone).put("password", password)).getJSONObject("data")
        store.token = d.getString("token")
        return d.getJSONObject("user").getString("name")
    }

    fun prices(days: Int = 30): List<CropPrice> {
        val a = call("GET", "/market/prices?days=$days").getJSONArray("data")
        return (0 until a.length()).map { CropPrice.fromJson(a.getJSONObject(it)) }
    }

    /** ops: [{id, entity_type, entity_id, operation, payload}] -> [{id, status: OK|FAILED, message}] */
    fun upload(ops: JSONArray): JSONArray =
        call("POST", "/sync/upload", JSONObject().put("operations", ops)).getJSONObject("data").getJSONArray("results")
}
