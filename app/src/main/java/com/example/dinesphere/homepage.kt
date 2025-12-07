package com.example.dinesphere

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
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
    private lateinit var navSaved: LinearLayout
    private lateinit var navProfile: LinearLayout
    private lateinit var navReview: LinearLayout

    private var userLat: Double = 0.0
    private var userLng: Double = 0.0
    private val savedRestaurantIds = mutableSetOf<Int>()
    private val within5kmRestaurantIds = mutableSetOf<Int>() // Track 5km restaurants

    @SuppressLint("MissingInflatedId")
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

        val userId = databaseHelper.getUserId()
        if (userId != null) {
            FCMTokenManager.initializeFCM(this, userId)
        }

        val notificationButton: ImageButton = findViewById(R.id.notification_icon)

        // Initialize views
        currAddress = findViewById(R.id.curr_address)
        addressIcon = findViewById(R.id.address_icon)
        searchBar = findViewById(R.id.search_bar)
        restaurantRecyclerView = findViewById(R.id.restaurantRecyclerView)
        allRestaurantRecyclerView = findViewById(R.id.allRestaurantRecyclerView)

        // Initialize bottom navigation
        navSaved = findViewById(R.id.save)
        navReview = findViewById(R.id.review)
        navProfile = findViewById(R.id.profile)

        // Setup RecyclerViews
        setupRecyclerViews()

        // Load user location from database
        loadUserLocationFromDatabase()

        // Address icon click - navigate to location1
        addressIcon.setOnClickListener {
            val intent = Intent(this, location1::class.java)
            startActivity(intent)
        }

        // Search bar click - navigate to search activity
        searchBar.setOnClickListener {
            val intent = Intent(this, search::class.java)
            startActivity(intent)
        }

        // Disable focus on search bar
        searchBar.isFocusable = false
        searchBar.isClickable = true

        // Saved navigation click
        navSaved.setOnClickListener {
            val intent = Intent(this, saved::class.java)
            startActivity(intent)
        }

        // Review navigation click
        navReview.setOnClickListener {
            val intent = Intent(this, reviews::class.java)
            startActivity(intent)
        }

        // Notification button click
        notificationButton.setOnClickListener {
            val intent = Intent(this, notifications::class.java)
            startActivity(intent)
        }

        // Profile navigation click
        navProfile.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            val userId = databaseHelper.getUserId()
            if (userId != null) {
                intent.putExtra("USER_ID", userId)
            }
            intent.putExtra("ADDRESS", currAddress.text.toString())
            intent.putExtra("LAT", userLat)
            intent.putExtra("LNG", userLng)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedRestaurantIds()
    }

    private fun setupRecyclerViews() {
        // Setup restaurants within 5km RecyclerView
        restaurantRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        restaurantAdapter = RestaurantAdapter(
            emptyList(),
            onItemClick = { restaurant ->
                onRestaurantClick(restaurant)
            },
            onSaveClick = { restaurant, position ->
                handleSaveClick(restaurant, position, restaurantAdapter)
            }
        )
        restaurantRecyclerView.adapter = restaurantAdapter

        // Setup all restaurants RecyclerView
        allRestaurantRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        allRestaurantAdapter = RestaurantAdapter(
            emptyList(),
            onItemClick = { restaurant ->
                onRestaurantClick(restaurant)
            },
            onSaveClick = { restaurant, position ->
                handleSaveClick(restaurant, position, allRestaurantAdapter)
            }
        )
        allRestaurantRecyclerView.adapter = allRestaurantAdapter
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

    private fun handleSaveClick(restaurant: Restaurant, position: Int, adapter: RestaurantAdapter) {
        if (restaurant.isSaved) {
            unsaveRestaurant(restaurant, position, adapter)
        } else {
            saveRestaurant(restaurant, position, adapter)
        }
    }

    private fun saveRestaurant(restaurant: Restaurant, position: Int, adapter: RestaurantAdapter) {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "Please log in to save restaurants", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}saved(post).php"
        Log.d("HomepageDebug", "Saving restaurant: ${restaurant.businessName}")

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("HomepageDebug", "Save response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        restaurant.isSaved = true
                        savedRestaurantIds.add(restaurant.restaurantId)
                        adapter.updateRestaurantSaveStatus(position, true)
                        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        val message = json.optString("message", "Failed to save")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("HomepageDebug", "Error parsing save response: ${e.message}")
                    Toast.makeText(this, "Error saving restaurant", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("HomepageDebug", "Save error: ${error.message}")
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

    private fun unsaveRestaurant(restaurant: Restaurant, position: Int, adapter: RestaurantAdapter) {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}saved(delete).php"
        Log.d("HomepageDebug", "Unsaving restaurant: ${restaurant.businessName}")

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("HomepageDebug", "Unsave response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        restaurant.isSaved = false
                        savedRestaurantIds.remove(restaurant.restaurantId)
                        adapter.updateRestaurantSaveStatus(position, false)
                        Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show()
                    } else {
                        val message = json.optString("message", "Failed to unsave")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("HomepageDebug", "Error parsing unsave response: ${e.message}")
                    Toast.makeText(this, "Error removing restaurant", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("HomepageDebug", "Unsave error: ${error.message}")
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
        Log.d("HomepageDebug", "Loading saved restaurant IDs")

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
                        Log.d("HomepageDebug", "Loaded ${savedRestaurantIds.size} saved restaurant IDs")
                    }
                } catch (e: Exception) {
                    Log.e("HomepageDebug", "Error loading saved IDs: ${e.message}")
                }
            },
            { error ->
                Log.e("HomepageDebug", "Error loading saved IDs: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
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

                        // Display only last 3 parts of address
                        val addressParts = address.split(",")
                        val displayAddress = if (addressParts.size > 3) {
                            addressParts.takeLast(3).joinToString(",")
                        } else {
                            address
                        }
                        currAddress.text = displayAddress
                        Log.d("HomepageDebug", "User location loaded: $userLat, $userLng")

                        // Load saved restaurant IDs first, then restaurants
                        loadSavedRestaurantIds()
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
                        within5kmRestaurantIds.clear() // Clear before adding new ones

                        for (i in 0 until restaurantsArray.length()) {
                            val item = restaurantsArray.getJSONObject(i)
                            val restaurantId = item.getInt("restaurant_id")

                            // Track this restaurant ID
                            within5kmRestaurantIds.add(restaurantId)

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
                            val restaurantId = item.getInt("restaurant_id")

                            // FILTER OUT: Skip restaurants that are already in the 5km list
                            if (within5kmRestaurantIds.contains(restaurantId)) {
                                continue
                            }

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
                            restaurants.add(restaurant)
                        }

                        Log.d("HomepageDebug", "Loaded ${restaurants.size} total restaurants (filtered)")
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