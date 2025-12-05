package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class homepage : AppCompatActivity() {

    private lateinit var currAddress: TextView
    private lateinit var addressIcon: ImageButton
    private lateinit var searchBar: EditText
    private lateinit var restaurantRecyclerView: RecyclerView
    private lateinit var allRestaurantRecyclerView: RecyclerView
    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var allRestaurantAdapter: RestaurantAdapter
    private lateinit var databaseHelper: DatabaseHelper

    private var userLat: Double = 0.0
    private var userLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_homepage)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this)

        // Initialize views
        currAddress = findViewById(R.id.curr_address)
        addressIcon = findViewById(R.id.address_icon)
        searchBar = findViewById(R.id.search_bar)
        restaurantRecyclerView = findViewById(R.id.restaurantRecyclerView)
        allRestaurantRecyclerView = findViewById(R.id.allRestaurantRecyclerView)

        // Setup RecyclerViews with horizontal layout
        setupRecyclerViews()

        // Load user location from database
        loadUserLocationFromDatabase()

        // Address icon click listener - navigate to change location
        addressIcon.setOnClickListener {
            // TODO: Navigate to location change screen
            Toast.makeText(this, "Change location", Toast.LENGTH_SHORT).show()
        }

        // Search bar click listener - navigate to search activity
        searchBar.setOnClickListener {
            val intent = Intent(this, search::class.java)
            startActivity(intent)
        }

        // Disable focus on search bar so it opens search screen instead of keyboard
        searchBar.isFocusable = false
        searchBar.isClickable = true
    }

    private fun setupRecyclerViews() {
        // Setup restaurants within 5km RecyclerView
        restaurantRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        restaurantAdapter = RestaurantAdapter(emptyList()) { restaurant ->
            onRestaurantClick(restaurant)
        }
        restaurantRecyclerView.adapter = restaurantAdapter

        // Setup all restaurants RecyclerView
        allRestaurantRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        allRestaurantAdapter = RestaurantAdapter(emptyList()) { restaurant ->
            onRestaurantClick(restaurant)
        }
        allRestaurantRecyclerView.adapter = allRestaurantAdapter
    }

    private fun onRestaurantClick(restaurant: Restaurant) {
        // Handle restaurant item click
        Toast.makeText(this, "Clicked: ${restaurant.businessName}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to restaurant detail screen
    }

    private fun loadUserLocationFromDatabase() {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Log.e("HomepageDebug", "User not logged in")
            currAddress.text = "Not logged in"
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}get_user_location.php?user_id=$userId"
        Log.d("HomepageDebug", "Loading user location from: $url")

        currAddress.text = "Loading location..."

        val request = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    Log.d("HomepageDebug", "Location response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val data = json.getJSONObject("data")
                        userLat = data.getDouble("latitude")
                        userLng = data.getDouble("longitude")
                        val address = data.getString("address")

                        currAddress.text = address
                        Log.d("HomepageDebug", "User location loaded: $userLat, $userLng")

                        // Now load restaurants with user's location
                        loadRestaurantsWithin5km()
                        loadAllRestaurants()
                    } else {
                        val message = json.optString("message", "Location not found")
                        Log.e("HomepageDebug", "Location not available: $message")
                        currAddress.text = "Location not set"
                        Toast.makeText(
                            this,
                            "Please set your location in profile settings",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("HomepageDebug", "Error parsing location: ${e.message}")
                    currAddress.text = "Error loading location"
                    Toast.makeText(
                        this,
                        "Failed to load location. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e("HomepageDebug", "Location fetch error: ${error.message}")
                currAddress.text = "Error loading location"

                val errorMsg = when {
                    error.networkResponse?.statusCode == 404 -> "Location not set. Please update your profile."
                    error.networkResponse?.statusCode == 400 -> "Invalid user ID"
                    error.networkResponse?.statusCode == 500 -> "Server error. Please try again later."
                    else -> "Network error. Please check your connection."
                }

                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadRestaurantsWithin5km() {
        if (userLat == 0.0 || userLng == 0.0) {
            Log.e("HomepageDebug", "Invalid user location: $userLat, $userLng")
            return
        }

        val url = "${Global.BASE_URL}get_restaurants.php?latitude=$userLat&longitude=$userLng&max_distance=5"

        Log.d("HomepageDebug", "Loading restaurants within 5km from: $url")

        val request = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    Log.d("HomepageDebug", "Response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val restaurantsArray = json.getJSONArray("restaurants")
                        val restaurants = mutableListOf<Restaurant>()

                        for (i in 0 until restaurantsArray.length()) {
                            val item = restaurantsArray.getJSONObject(i)
                            val restaurant = Restaurant(
                                restaurantId = item.getInt("restaurant_id"),
                                businessName = item.getString("business_name"),
                                address = item.getString("address"),
                                latitude = item.getDouble("latitude"),
                                longitude = item.getDouble("longitude"),
                                imageUrl = item.optString("image_url", null),
                                discount = item.optString("discount", null),
                                distanceKm = item.getDouble("distance_km"),
                                rating = item.optDouble("rating", 4.0).toFloat()
                            )
                            restaurants.add(restaurant)
                        }

                        Log.d("HomepageDebug", "Loaded ${restaurants.size} restaurants within 5km")
                        restaurantAdapter.updateRestaurants(restaurants)

                        if (restaurants.isEmpty()) {
                            Toast.makeText(
                                this,
                                "No restaurants found within 5km",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val message = json.optString("message", "Failed to load restaurants")
                        Log.e("HomepageDebug", "Error: $message")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("HomepageDebug", "Error parsing response: ${e.message}")
                }
            },
            Response.ErrorListener { error ->
                Log.e("HomepageDebug", "Volley error: ${error.message}")
                Toast.makeText(this, "Failed to load nearby restaurants", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadAllRestaurants() {
        if (userLat == 0.0 || userLng == 0.0) {
            Log.e("HomepageDebug", "Invalid user location: $userLat, $userLng")
            return
        }

        val url = "${Global.BASE_URL}get_all_restaurants.php?latitude=$userLat&longitude=$userLng"

        Log.d("HomepageDebug", "Loading all restaurants from: $url")

        val request = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    Log.d("HomepageDebug", "All restaurants response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val restaurantsArray = json.getJSONArray("restaurants")
                        val restaurants = mutableListOf<Restaurant>()

                        for (i in 0 until restaurantsArray.length()) {
                            val item = restaurantsArray.getJSONObject(i)
                            val restaurant = Restaurant(
                                restaurantId = item.getInt("restaurant_id"),
                                businessName = item.getString("business_name"),
                                address = item.getString("address"),
                                latitude = item.getDouble("latitude"),
                                longitude = item.getDouble("longitude"),
                                imageUrl = item.optString("image_url", null),
                                discount = item.optString("discount", null),
                                distanceKm = item.getDouble("distance_km"),
                                rating = item.optDouble("rating", 4.0).toFloat()
                            )
                            restaurants.add(restaurant)
                        }

                        Log.d("HomepageDebug", "Loaded ${restaurants.size} total restaurants")
                        allRestaurantAdapter.updateRestaurants(restaurants)
                    } else {
                        val message = json.optString("message", "Failed to load restaurants")
                        Log.e("HomepageDebug", "Error: $message")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("HomepageDebug", "Error parsing response: ${e.message}")
                }
            },
            Response.ErrorListener { error ->
                Log.e("HomepageDebug", "Volley error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}