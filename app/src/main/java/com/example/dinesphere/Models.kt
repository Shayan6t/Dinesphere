package com.example.dinesphere

// Category Data Class
data class Category(
    val categoryId: Int,
    val categoryName: String,
    val categoryImage: String?,
    val itemCount: Int = 0,
    var isSelected: Boolean = false
)

// Menu Item Data Class
data class MenuItem(
    val menuId: Int,
    val menuName: String,
    val menuDescription: String?,
    val menuPrice: Double,
    val menuImage: String?,
    val categoryId: Int = -1 // Add category ID for search filtering
)