package com.example.dinesphere

data class ReviewModel(
    val reviewId: Int,
    val restaurantId: Int,
    val userId: String,
    val rating: Int,
    val comment: String,
    val createdAt: String,
    val businessName: String,
    val address: String?,
    val restaurantImage: String?
)