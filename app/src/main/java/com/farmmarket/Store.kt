package com.farmmarket

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local storage (SharedPreferences): session, cached prices, listings and the sync queue. */
class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("fm", Context.MODE_PRIVATE)

    // Emulator -> use http://10.0.2.2:5000/api ; real phone -> http://<laptop-ip>:5000/api
    var baseUrl: String
        get() = sp.getString("url", "http://192.168.1.100:5000/api")!!
        set(v) { sp.edit().putString("url", v.trim().trimEnd('/')).apply() }
    var token: String?
        get() = sp.getString("token", null)
        set(v) { sp.edit().putString("token", v).apply() }
    var userName: String
        get() = sp.getString("user", "")!!
        set(v) { sp.edit().putString("user", v).apply() }
    var lang: String
        get() = sp.getString("lang", "en")!!
        set(v) { sp.edit().putString("lang", v).apply() }
    var demo: Boolean
        get() = sp.getBoolean("demo", false)
        set(v) { sp.edit().putBoolean("demo", v).apply() }
    var pricesTime: Long
        get() = sp.getLong("pricesTime", 0L)
        set(v) { sp.edit().putLong("pricesTime", v).apply() }
    var queue: JSONArray
        get() = JSONArray(sp.getString("queue", "[]"))
        set(v) { sp.edit().putString("queue", v.toString()).apply() }

    fun loadPrices(): List<CropPrice> {
        val a = JSONArray(sp.getString("prices", "[]"))
        return (0 until a.length()).map { CropPrice.fromJson(a.getJSONObject(it)) }
    }
    fun savePrices(p: List<CropPrice>) {
        sp.edit().putString("prices", JSONArray(p.map { it.toJson() }).toString()).apply()
    }

    fun loadListings(): List<Listing> {
        val a = JSONArray(sp.getString("listings", "[]"))
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Listing(o.getString("uuid"), o.getString("crop"), o.getDouble("qty"), o.getString("unit"), o.getString("status"))
        }
    }
    fun saveListings(l: List<Listing>) {
        val arr = JSONArray(l.map {
            JSONObject().put("uuid", it.uuid).put("crop", it.crop).put("qty", it.qty)
                .put("unit", it.unit).put("status", it.status)
        })
        sp.edit().putString("listings", arr.toString()).apply()
    }

    fun clearSession() {
        sp.edit().remove("token").remove("user").putBoolean("demo", false).apply()
    }
}
