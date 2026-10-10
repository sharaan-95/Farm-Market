package com.farmmarket

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppVm(app: Application) : AndroidViewModel(app) {
    val store = Store(app)
    private val api = Api(store)

    var loggedIn by mutableStateOf(store.token != null || store.demo)
    var demo by mutableStateOf(store.demo)
    var userName by mutableStateOf(store.userName)
    var lang by mutableStateOf(store.lang)
    var prices by mutableStateOf(if (store.demo) demoPrices() else store.loadPrices())
    var lastUpdated by mutableLongStateOf(store.pricesTime)
    var listings by mutableStateOf(store.loadListings())
    var offline by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var serverUrl by mutableStateOf(store.baseUrl)

    val pending get() = listings.count { it.status == "PENDING" }

    fun t(s: String) = if (lang == "ta") TA[s] ?: s else s

    init {
        if (loggedIn) refresh()
        // Auto sync: every 20 s while the app is open (pending changes, or detect the server coming back).
        viewModelScope.launch {
            while (true) {
                delay(20_000)
                if (loggedIn && !demo) {
                    doSync()
                    if (offline) doRefresh()
                }
            }
        }
    }

    fun setLang(l: String) { lang = l; store.lang = l }

    fun login(phone: String, pw: String) {
        viewModelScope.launch {
            loading = true; error = null
            try {
                val name = withContext(Dispatchers.IO) { api.login(phone, pw) }
                userName = name; store.userName = name
                store.demo = false; demo = false; loggedIn = true
                doRefresh(); doSync()
            } catch (e: ApiException) {
                error = e.message
            } finally { loading = false }
        }
    }

    fun demoLogin() {
        store.demo = true; demo = true; loggedIn = true
        userName = "Demo Farmer"; prices = demoPrices(); offline = false
    }

    fun logout() {
        store.clearSession(); loggedIn = false; demo = false; userName = ""
        prices = store.loadPrices()
    }

    fun setServer(url: String) { store.baseUrl = url; serverUrl = store.baseUrl; refresh() }

    fun refresh() { viewModelScope.launch { doRefresh() } }

    private suspend fun doRefresh() {
        if (demo) { prices = demoPrices(); return }
        try {
            val p = withContext(Dispatchers.IO) { api.prices() }
            prices = p; store.savePrices(p)
            lastUpdated = System.currentTimeMillis(); store.pricesTime = lastUpdated
            offline = false
        } catch (e: ApiException) {
            if (e.code == 401) logout() else if (e.unavailable) offline = true
        }
    }

    fun createListing(cropId: Int, cropName: String, qty: Double, unit: String, price: Double?,
                      variety: String, location: String, desc: String) {
        val uuid = UUID.randomUUID().toString()  // generated on the phone -> retries never duplicate on the server
        listings = listOf(Listing(uuid, cropName, qty, unit, if (demo) "LOCAL" else "PENDING")) + listings
        store.saveListings(listings)
        if (demo) return
        val payload = JSONObject().put("crop_id", cropId).put("quantity", qty).put("unit", unit)
            .put("expected_price", price).put("variety", variety.ifBlank { null })
            .put("location", location.ifBlank { null }).put("description", desc.ifBlank { null })
            .put("quality_grade", "A")
        val op = JSONObject().put("id", UUID.randomUUID().toString()).put("entity_type", "crop_listing")
            .put("entity_id", uuid).put("operation", "CREATE").put("payload", payload)
        val q = store.queue; q.put(op); store.queue = q
        viewModelScope.launch { doSync() }
    }

    private suspend fun doSync() {
        if (demo) return
        val q = store.queue
        if (q.length() == 0) return
        try {
            val res = withContext(Dispatchers.IO) { api.upload(q) }
            val status = HashMap<String, String>()
            for (i in 0 until res.length()) res.getJSONObject(i).let { status[it.getString("id")] = it.getString("status") }
            val keep = JSONArray()
            var ls = listings
            for (i in 0 until q.length()) {
                val op = q.getJSONObject(i)
                val uuid = op.getString("entity_id")
                if (status[op.getString("id")] == "OK") {
                    ls = ls.map { if (it.uuid == uuid) it.copy(status = "PUBLISHED") else it }
                } else {
                    val rc = op.optInt("retry_count", 0) + 1
                    if (rc >= 5) ls = ls.map { if (it.uuid == uuid) it.copy(status = "FAILED") else it }
                    else keep.put(op.put("retry_count", rc))
                }
            }
            store.queue = keep; listings = ls; store.saveListings(ls); offline = false
        } catch (e: ApiException) {
            if (e.code == 401) logout() else if (e.unavailable) offline = true
        }
    }

    private fun demoPrices(): List<CropPrice> {
        val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        fun p(id: Int, crop: String, m: String, type: String, price: Double, km: Double, tr: String) =
            CropPrice(id, crop, m, type, price, "quintal", km, tr, d)
        return listOf(
            p(1, "Tomato", "Tiruppur Mandi", "MANDI", 1850.0, 12.0, "UP"),
            p(1, "Tomato", "ABC Foods", "PROCESSOR", 2050.0, 30.0, "FLAT"),
            p(2, "Onion", "Coimbatore Mandi", "MANDI", 2400.0, 45.0, "DOWN"),
            p(2, "Onion", "Metro Retail", "INSTITUTIONAL", 2600.0, 60.0, "UP"),
            p(3, "Banana", "Erode Mandi", "MANDI", 3100.0, 55.0, "FLAT"),
        )
    }
}
