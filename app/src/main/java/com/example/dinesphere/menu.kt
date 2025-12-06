package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
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
    private lateinit var databaseHelper: DatabaseHelper

    private var restaurantId = -1

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navSaved: LinearLayout
    private lateinit var navReview: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        databaseHelper = DatabaseHelper(this)

        // Get Restaurant ID from Intent
        restaurantId = intent.getIntExtra("RESTAURANT_ID", -1)

        // Initialize Views
        btnBack = findViewById(R.id.back)
        recyclerCategories = findViewById(R.id.recycler_categories)
        recyclerMenu = findViewById(R.id.recycler_menu)

        // Initialize bottom navigation
        navHome = findViewById(R.id.home)
        navSaved = findViewById(R.id.saved)
        navReview = findViewById(R.id.review)
        navProfile = findViewById(R.id.profile)

        btnBack.setOnClickListener { finish() }

        // Setup RecyclerViews
        setupRecyclerViews()

        // Fetch Data
        if (restaurantId != -1) {
            loadCategories()
        } else {
            Toast.makeText(this, "Error: Restaurant not found", Toast.LENGTH_SHORT).show()
        }

        // Bottom Navigation - HOME
        navHome.setOnClickListener {
            val intent = Intent(this, homepage::class.java)
            startActivity(intent)
            finish()
        }

        // Bottom Navigation - SAVED
        navSaved.setOnClickListener {
            val intent = Intent(this, saved::class.java)
            startActivity(intent)
        }

        // Bottom Navigation - REVIEW
        navReview.setOnClickListener {
            val intent = Intent(this, reviews::class.java)
            startActivity(intent)
        }

        // Bottom Navigation - PROFILE
        navProfile.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            val userId = databaseHelper.getUserId()
            if (userId != null) {
                intent.putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }
    }

    private fun setupRecyclerViews() {
        // Categories (Horizontal)
        recyclerCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = CategoryAdapter(emptyList()) { selectedCategory ->
            loadMenuItems(selectedCategory.categoryId)
        }
        recyclerCategories.adapter = categoryAdapter

        // Menu Items (Vertical)
        recyclerMenu.layoutManager = LinearLayoutManager(this)
        menuAdapter = MenuAdapter(emptyList())
        recyclerMenu.adapter = menuAdapter
    }

    private fun loadCategories() {
        val url = "${Global.RESTAURANT_URL}category(get).php?restaurant_id=$restaurantId"
        android.util.Log.d("MenuDebug", "Fetching categories from: $url")

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                android.util.Log.d("MenuDebug", "Category Response: $response")
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
        val url = "${Global.RESTAURANT_URL}menu(get).php?category_id=$categoryId"
        android.util.Log.d("MenuDebug", "Fetching menu from: $url")

        menuAdapter.updateData(emptyList())

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                android.util.Log.d("MenuDebug", "Menu Response: $response")
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