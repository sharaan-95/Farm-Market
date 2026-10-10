package com.farmmarket

import org.json.JSONObject

data class CropPrice(
    val cropId: Int, val cropName: String, val marketName: String, val marketType: String,
    val price: Double, val unit: String, val distanceKm: Double, val trend: String, val priceDate: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("crop_id", cropId).put("crop_name", cropName).put("market_name", marketName)
        .put("market_type", marketType).put("price", price).put("unit", unit)
        .put("distance_km", distanceKm).put("trend", trend).put("price_date", priceDate)

    companion object {
        fun fromJson(j: JSONObject) = CropPrice(
            j.getInt("crop_id"), j.getString("crop_name"), j.getString("market_name"),
            j.getString("market_type"), j.getDouble("price"), j.optString("unit", "quintal"),
            j.optDouble("distance_km", 0.0), j.optString("trend", "FLAT"), j.getString("price_date").take(10),
        )
    }
}

/** status: PENDING (queued for sync) | PUBLISHED | FAILED | LOCAL (demo mode) */
data class Listing(val uuid: String, val crop: String, val qty: Double, val unit: String, val status: String)
