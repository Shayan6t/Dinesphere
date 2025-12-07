package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class search : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var backButton: ImageButton
    private lateinit var restaurantsRecycler: RecyclerView
    private lateinit var nearButton: Button
    private lateinit var allButton: Button
    private lateinit var recommendationsHeading: TextView

    private lateinit var restaurantsAdapter: RestaurantAdapter
    private lateinit var databaseHelper: DatabaseHelper

    private val allRestaurants = mutableListOf<Restaurant>()
    private val nearbyRestaurants = mutableListOf<Restaurant>() // 5km restaurants
    private val filteredRestaurants = mutableListOf<Restaurant>()
    private val savedRestaurantIds = mutableSetOf<Int>()

    private var userLat: Double = 0.0
    private var userLng: Double = 0.0

    // Filter state
    private var isNearSelected = true
    private var isAllSelected = true

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navSaved: LinearLayout
    private lateinit var navReview: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        databaseHelper = DatabaseHelper(this)

        // Initialize views
        searchBar = findViewById(R.id.search_bar)
        backButton = findViewById(R.id.back)
        restaurantsRecycler = findViewById(R.id.restaurants_recycler)
        nearButton = findViewById(R.id.near)
        allButton = findViewById(R.id.all)
        recommendationsHeading = findViewById(R.id.recommendations_heading)

        // Initialize bottom navigation
        navHome = findViewById(R.id.home)
        navSaved = findViewById(R.id.saved)
        navReview = findViewById(R.id.review)
        navProfile = findViewById(R.id.profile)

        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Setup RecyclerViews
        setupRecyclerViews()

        // Load user location and restaurants
        loadUserLocation()

        // Search functionality
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterRestaurants(s.toString())
            }
        })

        // Filter button listeners
        nearButton.setOnClickListener {
            isNearSelected = !isNearSelected
            updateFilterButtons()
            applyFilters()
        }

        allButton.setOnClickListener {
            isAllSelected = !isAllSelected
            updateFilterButtons()
            applyFilters()
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

    private fun updateFilterButtons() {
        // Update Near button
        if (isNearSelected) {
            nearButton.setBackgroundResource(R.drawable.button_box_o)
            nearButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.orange)
        } else {
            nearButton.setBackgroundResource(R.drawable.button_box_o)
            nearButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray)
        }

        // Update All button
        if (isAllSelected) {
            allButton.setBackgroundResource(R.drawable.button_box_o)
            allButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.orange)
        } else {
            allButton.setBackgroundResource(R.drawable.button_box_o)
            allButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray)
        }
    }

    private fun applyFilters() {
        filteredRestaurants.clear()

        if (isNearSelected && isAllSelected) {
            // Both selected: show all restaurants
            filteredRestaurants.addAll(allRestaurants)
            recommendationsHeading.text = "All Restaurants"
        } else if (isNearSelected) {
            // Only Near selected: show nearby (5km)
            filteredRestaurants.addAll(nearbyRestaurants)
            recommendationsHeading.text = "Nearby Restaurants (5km)"
        } else if (isAllSelected) {
            // Only All selected: show restaurants beyond 5km
            filteredRestaurants.addAll(allRestaurants.filter { it.distanceKm > 5.0 })
            recommendationsHeading.text = "Restaurants Beyond 5km"
        } else {
            // None selected: show nothing (or you could show all)
            // For better UX, let's show all when none selected
            filteredRestaurants.addAll(allRestaurants)
            recommendationsHeading.text = "All Restaurants"
        }

        restaurantsAdapter.notifyDataSetChanged()

        // Apply current search query if exists
        val currentQuery = searchBar.text.toString()
        if (currentQuery.isNotEmpty()) {
            filterRestaurants(currentQuery)
        }
    }

    private fun setupRecyclerViews() {
        restaurantsRecycler.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        restaurantsAdapter = RestaurantAdapter(
            filteredRestaurants,
            onItemClick = { restaurant ->
                onRestaurantClick(restaurant)
            },
            onSaveClick = { restaurant, position ->
                handleSaveClick(restaurant, position)
            }
        )
        restaurantsRecycler.adapter = restaurantsAdapter
    }

    private fun onRestaurantClick(restaurant: Restaurant) {
        val intent = Intent(this, RestaurantActivity::class.java)
        intent.putExtra("RESTAURANT_ID", restaurant.restaurantId)
        intent.putExtra("NAME", restaurant.businessName)
        intent.putExtra("ADDRESS", restaurant.address)
        intent.putExtra("IMAGE_URL", restaurant.imageUrl)
        intent.putExtra("RATING", restaurant.rating)
        intent.putExtra("DISTANCE", restaurant.distanceKm)
        intent.putExtra("IS_SAVED", restaurant.isSaved)
        intent.putExtra("LAT", restaurant.latitude)
        intent.putExtra("LNG", restaurant.longitude)
        intent.putExtra("PHONE", restaurant.phone)
        startActivity(intent)
    }

    private fun handleSaveClick(restaurant: Restaurant, position: Int) {
        if (restaurant.isSaved) {
            unsaveRestaurant(restaurant, position)
        } else {
            saveRestaurant(restaurant, position)
        }
    }

    private fun saveRestaurant(restaurant: Restaurant, position: Int) {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "Please log in to save restaurants", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}saved(post).php"
        Log.d("SearchDebug", "Saving restaurant: ${restaurant.businessName}")

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("SearchDebug", "Save response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        restaurant.isSaved = true
                        savedRestaurantIds.add(restaurant.restaurantId)
                        restaurantsAdapter.updateRestaurantSaveStatus(position, true)
                        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        val message = json.optString("message", "Failed to save")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SearchDebug", "Error parsing save response: ${e.message}")
                    Toast.makeText(this, "Error saving restaurant", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("SearchDebug", "Save error: ${error.message}")
                Toast.makeText(this, "Failed to save restaurant", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId
                params["restaurant_id"] = restaurant.restaurantId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun unsaveRestaurant(restaurant: Restaurant, position: Int) {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}saved(delete).php"
        Log.d("SearchDebug", "Unsaving restaurant: ${restaurant.restaurantId}")

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("SearchDebug", "Unsave response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        restaurant.isSaved = false
                        savedRestaurantIds.remove(restaurant.restaurantId)
                        restaurantsAdapter.updateRestaurantSaveStatus(position, false)
                        Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show()
                    } else {
                        val message = json.optString("message", "Failed to unsave")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SearchDebug", "Error parsing unsave response: ${e.message}")
                    Toast.makeText(this, "Error removing restaurant", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("SearchDebug", "Unsave error: ${error.message}")
                Toast.makeText(this, "Failed to unsave restaurant", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId
                params["restaurant_id"] = restaurant.restaurantId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadSavedRestaurantIds() {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            return
        }

        val url = "${Global.BASE_URL}saved(get).php?user_id=$userId"
        Log.d("SearchDebug", "Loading saved restaurant IDs")

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        savedRestaurantIds.clear()
                        val restaurantsArray = json.getJSONArray("restaurants")
                        for (i in 0 until restaurantsArray.length()) {
                            val item = restaurantsArray.getJSONObject(i)
                            savedRestaurantIds.add(item.getInt("restaurant_id"))
                        }
                        Log.d("SearchDebug", "Loaded ${savedRestaurantIds.size} saved restaurant IDs")
                    }
                } catch (e: Exception) {
                    Log.e("SearchDebug", "Error loading saved IDs: ${e.message}")
                }
            },
            { error ->
                Log.e("SearchDebug", "Error loading saved IDs: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadUserLocation() {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}get_user_location.php?user_id=$userId"
        Log.d("SearchDebug", "Loading user location from: $url")

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("SearchDebug", "Location response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val data = json.getJSONObject("data")
                        userLat = data.getDouble("latitude")
                        userLng = data.getDouble("longitude")

                        Log.d("SearchDebug", "User location: $userLat, $userLng")

                        loadSavedRestaurantIds()
                        loadAllRestaurants() // Load all restaurants at once
                    } else {
                        val message = json.optString("message", "Location not found")
                        Log.e("SearchDebug", "Error: $message")
                        Toast.makeText(this, "Please set your location first", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SearchDebug", "Error parsing location: ${e.message}")
                    Toast.makeText(this, "Error loading location", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("SearchDebug", "Location fetch error: ${error.message}")
                Toast.makeText(this, "Failed to load location", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadAllRestaurants() {
        if (userLat == 0.0 || userLng == 0.0) {
            Log.e("SearchDebug", "Invalid user location: $userLat, $userLng")
            return
        }

        // Load all restaurants (no distance limit)
        val url = "${Global.BASE_URL}get_all_restaurants.php?latitude=$userLat&longitude=$userLng"
        Log.d("SearchDebug", "Loading all restaurants from: $url")

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("SearchDebug", "Restaurants response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val restaurantsArray = json.getJSONArray("restaurants")
                        allRestaurants.clear()
                        nearbyRestaurants.clear()

                        for (i in 0 until restaurantsArray.length()) {
                            val item = restaurantsArray.getJSONObject(i)
                            val restaurantId = item.getInt("restaurant_id")
                            val restaurant = Restaurant(
                                restaurantId = restaurantId,
                                businessName = item.getString("business_name"),
                                address = item.getString("address"),
                                phone = item.optString("phone", null),
                                latitude = item.getDouble("latitude"),
                                longitude = item.getDouble("longitude"),
                                imageUrl = item.optString("image_url", null),
                                discount = item.optString("discount", null),
                                distanceKm = item.getDouble("distance_km"),
                                rating = item.optDouble("rating", 4.0).toFloat(),
                                isSaved = savedRestaurantIds.contains(restaurantId)
                            )
                            allRestaurants.add(restaurant)

                            // Separate nearby restaurants (5km)
                            if (restaurant.distanceKm <= 5.0) {
                                nearbyRestaurants.add(restaurant)
                            }
                        }

                        Log.d("SearchDebug", "Loaded ${allRestaurants.size} total restaurants, ${nearbyRestaurants.size} nearby")

                        // Apply current filter
                        applyFilters()

                        if (allRestaurants.isEmpty()) {
                            Toast.makeText(this, "No restaurants found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val message = json.optString("message", "Failed to load restaurants")
                        Log.e("SearchDebug", "Error: $message")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SearchDebug", "Error parsing response: ${e.message}")
                }
            },
            { error ->
                Log.e("SearchDebug", "Volley error: ${error.message}")
                Toast.makeText(this, "Failed to load restaurants", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun filterRestaurants(query: String) {
        val currentList = mutableListOf<Restaurant>()

        // Get the base list based on current filter
        if (isNearSelected && isAllSelected) {
            currentList.addAll(allRestaurants)
        } else if (isNearSelected) {
            currentList.addAll(nearbyRestaurants)
        } else if (isAllSelected) {
            currentList.addAll(allRestaurants.filter { it.distanceKm > 5.0 })
        } else {
            currentList.addAll(allRestaurants)
        }

        filteredRestaurants.clear()

        if (query.isEmpty()) {
            filteredRestaurants.addAll(currentList)
        } else {
            val lowerQuery = query.lowercase()
            filteredRestaurants.addAll(
                currentList.filter { restaurant ->
                    restaurant.businessName.lowercase().contains(lowerQuery)
                }
            )
        }

        restaurantsAdapter.notifyDataSetChanged()

        if (filteredRestaurants.isEmpty() && query.isNotEmpty()) {
            Toast.makeText(this, "No restaurants found matching '$query'", Toast.LENGTH_SHORT).show()
        }
    }
}