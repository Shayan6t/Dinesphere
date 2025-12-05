package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class saved : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var savedRecycler: RecyclerView
    private lateinit var emptyMessage: TextView
    private lateinit var navHome: LinearLayout
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var savedAdapter: SavedRestaurantAdapter

    private val allSavedRestaurants = mutableListOf<Restaurant>()
    private val filteredRestaurants = mutableListOf<Restaurant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        databaseHelper = DatabaseHelper(this)

        // Initialize views
        searchBar = findViewById(R.id.searchbar)
        savedRecycler = findViewById(R.id.saved_recycler)
        emptyMessage = findViewById(R.id.empty_message)
        navHome = findViewById(R.id.nav_home)

        // Setup RecyclerView
        setupRecyclerView()

        // Load saved restaurants
        loadSavedRestaurants()

        // Search functionality
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterRestaurants(s.toString())
            }
        })

        // Navigation - Home button
        navHome.setOnClickListener {
            finish() // Go back to homepage
        }
    }

    private fun setupRecyclerView() {
        savedRecycler.layoutManager = LinearLayoutManager(this)
        savedAdapter = SavedRestaurantAdapter(
            filteredRestaurants,
            onRestaurantClick = { restaurant ->
                Toast.makeText(this, "Opening: ${restaurant.businessName}", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to restaurant detail
            },
            onUnsaveClick = { restaurant, position ->
                unsaveRestaurant(restaurant, position)
            }
        )
        savedRecycler.adapter = savedAdapter
    }

    private fun loadSavedRestaurants() {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Log.e("SavedDebug", "User not logged in")
            showEmptyMessage()
            Toast.makeText(this, "Please log in to view saved restaurants", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}saved(get).php?user_id=$userId"
        Log.d("SavedDebug", "Loading saved restaurants from: $url")

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("SavedDebug", "Response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val restaurantsArray = json.getJSONArray("restaurants")
                        allSavedRestaurants.clear()

                        for (i in 0 until restaurantsArray.length()) {
                            val item = restaurantsArray.getJSONObject(i)
                            val restaurant = Restaurant(
                                restaurantId = item.getInt("restaurant_id"),
                                businessName = item.getString("business_name"),
                                address = item.getString("address"),
                                latitude = item.getDouble("latitude"),
                                longitude = item.getDouble("longitude"),
                                imageUrl = item.optString("restaurant_image", null),
                                discount = item.optString("discount", null),
                                distanceKm = item.optDouble("distance_km", 0.0),
                                rating = item.optDouble("rating", 4.0).toFloat()
                            )
                            allSavedRestaurants.add(restaurant)
                        }

                        Log.d("SavedDebug", "Loaded ${allSavedRestaurants.size} saved restaurants")

                        // Show all saved restaurants
                        filteredRestaurants.clear()
                        filteredRestaurants.addAll(allSavedRestaurants)
                        savedAdapter.notifyDataSetChanged()

                        if (allSavedRestaurants.isEmpty()) {
                            showEmptyMessage()
                        } else {
                            hideEmptyMessage()
                        }
                    } else {
                        val message = json.optString("message", "Failed to load saved restaurants")
                        Log.e("SavedDebug", "Error: $message")
                        showEmptyMessage()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SavedDebug", "Error parsing response: ${e.message}")
                    showEmptyMessage()
                }
            },
            { error ->
                Log.e("SavedDebug", "Volley error: ${error.message}")
                Toast.makeText(this, "Failed to load saved restaurants", Toast.LENGTH_SHORT).show()
                showEmptyMessage()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun unsaveRestaurant(restaurant: Restaurant, position: Int) {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}save(delete).php"
        Log.d("SavedDebug", "Unsaving restaurant: ${restaurant.businessName}")

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("SavedDebug", "Unsave response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        // Remove from lists
                        allSavedRestaurants.remove(restaurant)
                        filteredRestaurants.removeAt(position)
                        savedAdapter.notifyItemRemoved(position)

                        Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show()

                        if (filteredRestaurants.isEmpty()) {
                            showEmptyMessage()
                        }
                    } else {
                        val message = json.optString("message", "Failed to unsave")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SavedDebug", "Error parsing unsave response: ${e.message}")
                    Toast.makeText(this, "Error removing restaurant", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("SavedDebug", "Unsave error: ${error.message}")
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

    private fun filterRestaurants(query: String) {
        filteredRestaurants.clear()

        if (query.isEmpty()) {
            filteredRestaurants.addAll(allSavedRestaurants)
        } else {
            val lowerQuery = query.lowercase()
            filteredRestaurants.addAll(
                allSavedRestaurants.filter { restaurant ->
                    restaurant.businessName.lowercase().contains(lowerQuery)
                }
            )
        }

        savedAdapter.notifyDataSetChanged()

        if (filteredRestaurants.isEmpty() && query.isNotEmpty()) {
            Toast.makeText(this, "No restaurants found matching '$query'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmptyMessage() {
        emptyMessage.visibility = View.VISIBLE
        savedRecycler.visibility = View.GONE
    }

    private fun hideEmptyMessage() {
        emptyMessage.visibility = View.GONE
        savedRecycler.visibility = View.VISIBLE
    }
}