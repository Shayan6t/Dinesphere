package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
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
    private lateinit var emptyStateImage: ImageView
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

        // Initialize views
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView)
        emptyStateImage = findViewById(R.id.emptyStateImage)
        navHome = findViewById(R.id.home)
        navSaved = findViewById(R.id.saved)
        navProfile = findViewById(R.id.profile)

        // Setup RecyclerView
        setupRecyclerView()

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

    private fun setupRecyclerView() {
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        reviewAdapter = ReviewAdapter(emptyList())
        reviewsRecyclerView.adapter = reviewAdapter
    }

    private fun loadReviewedRestaurants() {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "Please log in to view reviews", Toast.LENGTH_SHORT).show()
            showEmptyState(true)
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

                            // FIXED: Format address to show only last 3 parts
                            val fullAddress = item.optString("address", "")
                            val formattedAddress = if (fullAddress.isNotEmpty()) {
                                val addressParts = fullAddress.split(",")
                                if (addressParts.size > 3) {
                                    addressParts.takeLast(3).joinToString(",")
                                } else {
                                    fullAddress
                                }
                            } else {
                                "Address not available"
                            }

                            val review = ReviewModel(
                                reviewId = item.getInt("review_id"),
                                restaurantId = item.getInt("restaurant_id"),
                                userId = item.getString("user_id"),
                                rating = item.getInt("rating"),
                                comment = item.getString("comment"),
                                createdAt = item.getString("created_at"),
                                businessName = item.getString("business_name"),
                                address = formattedAddress,
                                restaurantImage = item.optString("restaurant_image", null)
                            )
                            reviews.add(review)
                        }

                        Log.d("ReviewsDebug", "Loaded ${reviews.size} reviews")
                        reviewAdapter.updateReviews(reviews)

                        // Show/hide empty state
                        showEmptyState(reviews.isEmpty())

                        if (reviews.isEmpty()) {
                            Toast.makeText(
                                this,
                                "No reviewed restaurants yet. Visit a restaurant to add a review!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val message = json.optString("message", "No reviews found")
                        Log.e("ReviewsDebug", "Error: $message")
                        showEmptyState(true)
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("ReviewsDebug", "Error: ${e.message}")
                    Toast.makeText(this, "Error loading reviews", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            },
            { error ->
                Log.e("ReviewsDebug", "Network error: ${error.message}")
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
                showEmptyState(true)
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            emptyStateImage.visibility = View.VISIBLE
            reviewsRecyclerView.visibility = View.GONE
        } else {
            emptyStateImage.visibility = View.GONE
            reviewsRecyclerView.visibility = View.VISIBLE
        }
    }
}