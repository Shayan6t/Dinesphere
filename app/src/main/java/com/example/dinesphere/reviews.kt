package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class reviews : AppCompatActivity() {

    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var navHome: LinearLayout
    private lateinit var navSaved: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reviews)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this)

        // Initialize RecyclerView (hidden in your current XML, need to add it)
        // For now, we'll work with the static layout you have

        // Initialize bottom navigation
        navHome = findViewById(R.id.home)
        navSaved = findViewById(R.id.saved)
        navProfile = findViewById(R.id.profile)

        // Navigation click listeners
        navHome.setOnClickListener {
            val intent = Intent(this, homepage::class.java)
            startActivity(intent)
            finish()
        }

        navSaved.setOnClickListener {
            val intent = Intent(this, saved::class.java)
            startActivity(intent)
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
        }

        // Load reviewed restaurants
        loadReviewedRestaurants()
    }

    private fun loadReviewedRestaurants() {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "Please log in to view reviews", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}review(get).php?user_id=$userId"
        Log.d("ReviewsDebug", "Loading reviews from: $url")

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("ReviewsDebug", "Response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val reviewsArray = json.getJSONArray("reviews")
                        val reviews = mutableListOf<ReviewModel>()

                        for (i in 0 until reviewsArray.length()) {
                            val item = reviewsArray.getJSONObject(i)
                            val review = ReviewModel(
                                reviewId = item.getInt("review_id"),
                                restaurantId = item.getInt("restaurant_id"),
                                userId = item.getString("user_id"),
                                rating = item.getInt("rating"),
                                comment = item.getString("comment"),
                                createdAt = item.getString("created_at"),
                                businessName = item.getString("business_name"),
                                address = item.optString("address", ""),
                                restaurantImage = item.optString("restaurant_image", null)
                            )
                            reviews.add(review)
                        }

                        Log.d("ReviewsDebug", "Loaded ${reviews.size} reviews")

                        // Show only first 5 in the static layout
                        populateStaticReviews(reviews)

                        if (reviews.isEmpty()) {
                            Toast.makeText(
                                this,
                                "No reviewed restaurants yet",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val message = json.optString("message", "No reviews found")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("ReviewsDebug", "Error: ${e.message}")
                    Toast.makeText(this, "Error loading reviews", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ReviewsDebug", "Network error: ${error.message}")
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun populateStaticReviews(reviews: List<ReviewModel>) {
        val restaurant1 = findViewById<LinearLayout>(R.id.restaurant1)
        val restaurant2 = findViewById<LinearLayout>(R.id.restaurant2)
        val restaurant3 = findViewById<LinearLayout>(R.id.restaurant3)
        val restaurant4 = findViewById<LinearLayout>(R.id.restaurant4)
        val restaurant5 = findViewById<LinearLayout>(R.id.restaurant5)

        val restaurants = listOf(restaurant1, restaurant2, restaurant3, restaurant4, restaurant5)

        // Hide all first
        restaurants.forEach { it.visibility = LinearLayout.GONE }

        // Show and populate available reviews
        reviews.take(5).forEachIndexed { index, review ->
            restaurants[index].apply {
                visibility = LinearLayout.VISIBLE

                // Set restaurant name and address
                val nameView = when(index) {
                    0 -> findViewById<android.widget.TextView>(R.id.restaurantName1)
                    1 -> findViewById<android.widget.TextView>(R.id.restaurantName2)
                    2 -> findViewById<android.widget.TextView>(R.id.restaurantName3)
                    3 -> findViewById<android.widget.TextView>(R.id.restaurantName4)
                    else -> findViewById<android.widget.TextView>(R.id.restaurantName5)
                }

                val locationView = when(index) {
                    0 -> findViewById<android.widget.TextView>(R.id.location1)
                    1 -> findViewById<android.widget.TextView>(R.id.location2)
                    2 -> findViewById<android.widget.TextView>(R.id.location3)
                    3 -> findViewById<android.widget.TextView>(R.id.location4)
                    else -> findViewById<android.widget.TextView>(R.id.location5)
                }

                nameView.text = review.businessName
                locationView.text = review.address ?: "Address not available"

                // Click to open feedback page
                setOnClickListener {
                    val intent = Intent(this@reviews, feedback::class.java)
                    intent.putExtra("RESTAURANT_ID", review.restaurantId)
                    intent.putExtra("RESTAURANT_NAME", review.businessName)
                    intent.putExtra("RESTAURANT_ADDRESS", review.address)
                    intent.putExtra("EXISTING_RATING", review.rating)
                    intent.putExtra("EXISTING_COMMENT", review.comment)
                    intent.putExtra("REVIEW_ID", review.reviewId)
                    startActivity(intent)
                }
            }
        }
    }
}