package com.example.dinesphere

data class Restaurant(
    val restaurantId: Int,
    val businessName: String,
    val address: String,
    val phone: String?,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val discount: String?,
    val distanceKm: Double,
    val rating: Float,
    var isSaved: Boolean = false
)