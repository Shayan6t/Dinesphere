package com.example.dinesphere

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class feedback : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var backButton: ImageButton
    private lateinit var feedbackBox: EditText
    private lateinit var submitButton: ImageButton
    private lateinit var questionText: TextView
    private lateinit var characterCount: TextView

    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var star4: ImageView
    private lateinit var star5: ImageView
    private val stars by lazy { listOf(star1, star2, star3, star4, star5) }

    private var selectedRating = 0
    private var restaurantId = -1
    private var restaurantName = ""
    private var reviewId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this)

        // Initialize views
        backButton = findViewById(R.id.back)
        feedbackBox = findViewById(R.id.feedback_box)
        submitButton = findViewById(R.id.feedback_btn_back)
        questionText = findViewById(R.id.question)
        characterCount = findViewById(R.id.characterCount) // Add this ID to your XML

        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        star4 = findViewById(R.id.star4)
        star5 = findViewById(R.id.star5)

        // Get restaurant data from intent
        restaurantId = intent.getIntExtra("RESTAURANT_ID", -1)
        restaurantName = intent.getStringExtra("RESTAURANT_NAME") ?: ""
        val existingRating = intent.getIntExtra("EXISTING_RATING", 0)
        val existingComment = intent.getStringExtra("EXISTING_COMMENT") ?: ""
        reviewId = intent.getIntExtra("REVIEW_ID", -1)

        // Set question text with restaurant name
        questionText.text = "What is your opinion for $restaurantName?"

        // Load existing review if available
        if (existingRating > 0) {
            selectedRating = existingRating
            updateStarRating(existingRating)
        }

        if (existingComment.isNotEmpty()) {
            feedbackBox.setText(existingComment)
        }

        // Update character count
        updateCharacterCount()

        // Setup star rating click listeners
        setupStarRating()

        // Character count listener
        feedbackBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharacterCount()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Submit button
        submitButton.setOnClickListener {
            submitReview()
        }
    }

    private fun setupStarRating() {
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                selectedRating = index + 1
                updateStarRating(selectedRating)
            }
        }
    }

    private fun updateStarRating(rating: Int) {
        stars.forEachIndexed { index, star ->
            if (index < rating) {
                // Filled star
                star.setImageResource(R.drawable.b_star)
                star.clearColorFilter()
            } else {
                // Empty star
                star.setImageResource(R.drawable.b_star)
                star.setColorFilter(ContextCompat.getColor(this, R.color.white))
            }
        }
    }

    private fun updateCharacterCount() {
        val length = feedbackBox.text.length
        characterCount.text = "$length Characters"
    }

    private fun submitReview() {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "Please log in to submit review", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = feedbackBox.text.toString().trim()

        val url = "${Global.BASE_URL}review(post).php"
        Log.d("FeedbackDebug", "Submitting review to: $url")

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("FeedbackDebug", "Response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val action = json.optString("action", "saved")
                        val message = if (action == "updated") {
                            "Review updated successfully!"
                        } else {
                            "Review submitted successfully!"
                        }

                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val message = json.optString("message", "Failed to submit review")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("FeedbackDebug", "Error: ${e.message}")
                    Toast.makeText(this, "Error submitting review", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("FeedbackDebug", "Network error: ${error.message}")
                Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId,
                    "restaurant_id" to restaurantId.toString(),
                    "rating" to selectedRating.toString(),
                    "comment" to comment
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}