package com.example.dinesphere

data class NotificationModel(
    val notificationId: Int,
    val restaurantId: Int,
    val title: String,
    val message: String,
    val createdAt: String,
    val businessName: String,
    val restaurantImage: String?
)