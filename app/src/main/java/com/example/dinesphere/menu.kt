package com.example.dinesphere

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
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
    private lateinit var searchBar: EditText
    private lateinit var databaseHelper: DatabaseHelper

    private var restaurantId = -1

    // Data lists
    private val allCategories = mutableListOf<Category>()
    private val allMenuItems = mutableListOf<MenuItem>()
    private val filteredMenuItems = mutableListOf<MenuItem>()
    private var currentCategoryId = -1

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
        searchBar = findViewById(R.id.search_bar)
        recyclerCategories = findViewById(R.id.recycler_categories)
        recyclerMenu = findViewById(R.id.recycler_menu)

        // Initialize bottom navigation
        navHome = findViewById(R.id.home)
        navSaved = findViewById(R.id.saved)
        navReview = findViewById(R.id.review)
        navProfile = findViewById(R.id.profile)

        btnBack.setOnClickListener { finish() }

        // Setup Search
        setupSearch()

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

    private fun setupSearch() {
        // Real-time search as user types
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterMenuItems(s.toString())
            }
        })
    }

    private fun filterMenuItems(query: String) {
        filteredMenuItems.clear()

        if (query.isEmpty()) {
            // If no search query, show items from current category
            if (currentCategoryId != -1) {
                filteredMenuItems.addAll(allMenuItems.filter { it.categoryId == currentCategoryId })
            } else {
                filteredMenuItems.addAll(allMenuItems)
            }
        } else {
            // Search across all menu items
            val lowerQuery = query.lowercase()
            filteredMenuItems.addAll(
                allMenuItems.filter { item ->
                    item.menuName.lowercase().contains(lowerQuery) ||
                            item.menuDescription?.lowercase()?.contains(lowerQuery) == true
                }
            )
        }

        menuAdapter.updateData(filteredMenuItems)

        if (filteredMenuItems.isEmpty() && query.isNotEmpty()) {
            Toast.makeText(this, "No items found matching '$query'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        // Categories (Horizontal)
        recyclerCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = CategoryAdapter(emptyList()) { selectedCategory ->
            currentCategoryId = selectedCategory.categoryId
            loadMenuItems(selectedCategory.categoryId)
            // Clear search when switching categories
            searchBar.text.clear()
        }
        recyclerCategories.adapter = categoryAdapter

        // Menu Items (Vertical)
        recyclerMenu.layoutManager = LinearLayoutManager(this)
        menuAdapter = MenuAdapter(emptyList())
        recyclerMenu.adapter = menuAdapter
    }

    // --- Helper: Network Check ---
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun loadCategories() {
        // 1. Load Local Cache First (Instant)
        val cachedCategories = databaseHelper.getCachedCategories(restaurantId)
        if (cachedCategories.isNotEmpty()) {
            cachedCategories[0].isSelected = true // Auto-select first category
            allCategories.clear()
            allCategories.addAll(cachedCategories)
            categoryAdapter.updateData(cachedCategories)
            currentCategoryId = cachedCategories[0].categoryId
            loadMenuItems(cachedCategories[0].categoryId) // Load menu for it
        }

        // 2. If Offline, stop here
        if (!isNetworkAvailable()) {
            if (cachedCategories.isEmpty()) {
                Toast.makeText(this, "Offline and no menu data.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Showing cached menu", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // 3. If Online, fetch from API
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

                        // Save to Cache
                        databaseHelper.cacheCategories(restaurantId, list)

                        // Update data
                        allCategories.clear()
                        allCategories.addAll(list)

                        // Update UI
                        categoryAdapter.updateData(list)

                        if (list.isNotEmpty()) {
                            currentCategoryId = list[0].categoryId
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
                // No toast here if we already showed cached data
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun loadMenuItems(categoryId: Int) {
        currentCategoryId = categoryId

        // 1. Load Local Cache First
        val cachedItems = databaseHelper.getCachedMenuItems(categoryId)
        if (cachedItems.isNotEmpty()) {
            // Ensure cached items have the correct categoryId
            val itemsWithCategory = cachedItems.map { it.copy(categoryId = categoryId) }

            // Update the all items list with cached data
            updateAllMenuItems(itemsWithCategory, categoryId)
            filteredMenuItems.clear()
            filteredMenuItems.addAll(itemsWithCategory)
            menuAdapter.updateData(filteredMenuItems)
        } else {
            // Only clear list if we have no cached data, to avoid flickering
            menuAdapter.updateData(emptyList())
        }

        // 2. If Offline, stop
        if (!isNetworkAvailable()) return

        // 3. If Online, fetch fresh data
        val url = "${Global.RESTAURANT_URL}menu(get).php?category_id=$categoryId"
        android.util.Log.d("MenuDebug", "Fetching menu from: $url")

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
                                menuImage = obj.optString("menu_image"),
                                categoryId = categoryId // Store category ID for filtering
                            ))
                        }

                        // Save to Cache
                        databaseHelper.cacheMenuItems(categoryId, list)

                        // Update the all items list
                        updateAllMenuItems(list, categoryId)

                        // Update UI only if no search is active
                        if (searchBar.text.isEmpty()) {
                            filteredMenuItems.clear()
                            filteredMenuItems.addAll(list)
                            menuAdapter.updateData(filteredMenuItems)
                        } else {
                            // Re-apply search with new data
                            filterMenuItems(searchBar.text.toString())
                        }
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

    private fun updateAllMenuItems(items: List<MenuItem>, categoryId: Int) {
        // Remove old items from this category
        allMenuItems.removeAll { it.categoryId == categoryId }

        // Ensure all items have the correct categoryId before adding
        val itemsWithCategory = items.map { it.copy(categoryId = categoryId) }

        // Add new items
        allMenuItems.addAll(itemsWithCategory)

        android.util.Log.d("MenuDebug", "Total items in allMenuItems: ${allMenuItems.size}")
    }
}