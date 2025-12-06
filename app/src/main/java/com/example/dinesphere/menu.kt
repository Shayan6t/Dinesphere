package com.example.dinesphere

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class menu : AppCompatActivity() {

    private lateinit var recyclerCategories: RecyclerView
    private lateinit var recyclerMenu: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var btnBack: ImageButton

    private var restaurantId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // 1. Get Restaurant ID from Intent
        restaurantId = intent.getIntExtra("RESTAURANT_ID", -1)

        // 2. Initialize Views
        btnBack = findViewById(R.id.back)
        recyclerCategories = findViewById(R.id.recycler_categories)
        recyclerMenu = findViewById(R.id.recycler_menu)

        btnBack.setOnClickListener { finish() }

        // 3. Setup RecyclerViews
        setupRecyclerViews()

        // 4. Fetch Data
        if (restaurantId != -1) {
            loadCategories()
        } else {
            Toast.makeText(this, "Error: Restaurant not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        // Categories (Horizontal)
        recyclerCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = CategoryAdapter(emptyList()) { selectedCategory ->
            // When category is clicked, load menu items for it
            loadMenuItems(selectedCategory.categoryId)
        }
        recyclerCategories.adapter = categoryAdapter

        // Menu Items (Vertical)
        recyclerMenu.layoutManager = LinearLayoutManager(this)
        menuAdapter = MenuAdapter(emptyList())
        recyclerMenu.adapter = menuAdapter
    }

    private fun loadCategories() {
        // USE RESTAURANT_URL HERE
        val url = "${Global.RESTAURANT_URL}category(get).php?restaurant_id=$restaurantId"

        android.util.Log.d("MenuDebug", "Fetching categories from: $url") // Add Log

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                android.util.Log.d("MenuDebug", "Category Response: $response") // Add Log
                try {
                    val json = JSONObject(response)
                    if (json.optString("status") == "success") {
                        val array = json.getJSONArray("data")
                        val list = mutableListOf<Category>()

                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(Category(
                                categoryId = obj.getInt("category_id"),
                                categoryName = obj.getString("category_name"),
                                categoryImage = obj.optString("category_image"),
                                itemCount = obj.optInt("item_count"),
                                isSelected = i == 0
                            ))
                        }

                        categoryAdapter.updateData(list)

                        if (list.isNotEmpty()) {
                            loadMenuItems(list[0].categoryId)
                        }
                    } else {
                        android.util.Log.e("MenuDebug", "API Status not success: ${json.optString("message")}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.e("MenuDebug", "JSON Parse Error: ${e.message}")
                }
            },
            { error ->
                android.util.Log.e("MenuDebug", "Volley Error: ${error.message}")
                Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun loadMenuItems(categoryId: Int) {
        // USE RESTAURANT_URL HERE
        val url = "${Global.RESTAURANT_URL}menu(get).php?category_id=$categoryId"

        android.util.Log.d("MenuDebug", "Fetching menu from: $url") // Add Log

        // Clear list so user knows it's reloading
        menuAdapter.updateData(emptyList())

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                android.util.Log.d("MenuDebug", "Menu Response: $response") // Add Log
                try {
                    val json = JSONObject(response)
                    if (json.optString("status") == "success") {
                        val array = json.getJSONArray("data")
                        val list = mutableListOf<MenuItem>()

                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(MenuItem(
                                menuId = obj.getInt("menu_id"),
                                menuName = obj.getString("menu_name"),
                                menuDescription = obj.optString("menu_description"),
                                menuPrice = obj.getDouble("menu_price"),
                                menuImage = obj.optString("menu_image")
                            ))
                        }
                        menuAdapter.updateData(list)
                    } else {
                        // Some categories might be empty, that's okay
                        android.util.Log.d("MenuDebug", "No items or status error")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                android.util.Log.e("MenuDebug", "Volley Error Menu: ${error.message}")
            }
        )
        Volley.newRequestQueue(this).add(request)
    }
}