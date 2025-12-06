package com.example.dinesphere

data class Category(
    val categoryId: Int,
    val categoryName: String,
    val categoryImage: String?,
    val itemCount: Int,
    var isSelected: Boolean = false // To highlight selected tab
)

data class MenuItem(
    val menuId: Int,
    val menuName: String,
    val menuDescription: String,
    val menuPrice: Double,
    val menuImage: String?,
    val isAvailable: Boolean = true // You can add logic for this later
)