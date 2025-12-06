package com.example.dinesphere

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import org.json.JSONObject
import kotlin.math.ceil

class RestaurantActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private var isSaved = false
    private var restaurantId = -1
    private var distanceKm = 0.0
    private var phoneNumber: String? = null // Storage for the phone number

    // Views
    private lateinit var txtTime: TextView
    private lateinit var btnCar: LinearLayout
    private lateinit var btnCycle: LinearLayout
    private lateinit var btnWalk: LinearLayout
    private lateinit var btnCall: LinearLayout // Added call button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant)

        databaseHelper = DatabaseHelper(this)

        // 1. Initialize Views
        val imgRestaurant = findViewById<ImageView>(R.id.restaurant_img)
        val btnBack = findViewById<ImageButton>(R.id.back)
        val txtName = findViewById<TextView>(R.id.name)
        val txtLocation = findViewById<TextView>(R.id.location)
        val txtRating = findViewById<TextView>(R.id.ratingScore)
        val btnSave = findViewById<ImageView>(R.id.save_btn)

        // Time & Transport Views
        txtTime = findViewById(R.id.deliveryTime)
        btnCar = findViewById(R.id.btn_car)
        btnCycle = findViewById(R.id.btn_cycle)
        btnWalk = findViewById(R.id.btn_walk)
        btnCall = findViewById(R.id.btn_call) // Initialize call button view

        // 2. Extract Data passed from Homepage
        restaurantId = intent.getIntExtra("RESTAURANT_ID", -1)
        val name = intent.getStringExtra("NAME") ?: "Unknown Restaurant"
        val address = intent.getStringExtra("ADDRESS") ?: "Address"
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val rating = intent.getFloatExtra("RATING", 4.0f)
        distanceKm = intent.getDoubleExtra("DISTANCE", 0.0)
        isSaved = intent.getBooleanExtra("IS_SAVED", false)
        phoneNumber = intent.getStringExtra("PHONE") // Extract the phone number

        // 3. Populate Views
        txtName.text = name
        txtLocation.text = address
        txtRating.text = rating.toString()

        // Load Image using Picasso
        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(imageUrl)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.img1)
                .error(R.drawable.img1)
                .into(imgRestaurant)
        } else {
            imgRestaurant.setImageResource(R.drawable.img1)
        }

        // Set Initial Save State
        updateSaveIcon(btnSave, isSaved)

        // --- TRANSPORT LOGIC ---
        updateTimeAndSelection(btnCar, 30) // Default Mode (Car)

        btnCar.setOnClickListener { updateTimeAndSelection(btnCar, 30) }
        btnCycle.setOnClickListener { updateTimeAndSelection(btnCycle, 15) }
        btnWalk.setOnClickListener { updateTimeAndSelection(btnWalk, 5) }
        // -----------------------

        // 4. CALL BUTTON FUNCTIONALITY
        btnCall.setOnClickListener {
            if (!phoneNumber.isNullOrEmpty()) {
                // ACTION_DIAL opens the dialer app with the number pre-filled.
                // The user still has to press the call button in the dialer.
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:$phoneNumber")
                startActivity(dialIntent)
            } else {
                Toast.makeText(this, "Restaurant phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Back Button
        btnBack.setOnClickListener { finish() }

        // Save Button Logic
        btnSave.setOnClickListener {
            if (isSaved) unsaveRestaurant(btnSave) else saveRestaurant(btnSave)
        }
    }

    private fun updateTimeAndSelection(selectedBtn: LinearLayout, speedKmh: Int) {
        // 1. Reset all buttons to gray tint (Unselected state)
        val grayColor = ColorStateList.valueOf(Color.parseColor("#C7C7C7"))
        btnCar.backgroundTintList = grayColor
        btnCycle.backgroundTintList = grayColor
        btnWalk.backgroundTintList = grayColor

        // 2. Set selected button to No Tint/White (Selected state)
        selectedBtn.backgroundTintList = null

        // 3. Calculate Time: (Distance / Speed) * 60 minutes
        if (distanceKm > 0) {
            val timeInMinutes = ceil((distanceKm / speedKmh) * 60).toInt()
            txtTime.text = "$timeInMinutes mins"
        } else {
            txtTime.text = "-- mins"
        }
    }

    private fun updateSaveIcon(imageView: ImageView, saved: Boolean) {
        if (saved) {
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.orange))
        } else {
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun saveRestaurant(btnSave: ImageView) {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Toast.makeText(this, "Please log in to save", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${Global.BASE_URL}saved(post).php"

        val request = object : StringRequest(Request.Method.POST, url, { response ->
            try {
                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    isSaved = true
                    updateSaveIcon(btnSave, true)
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, {
            Toast.makeText(this, "Connection Error", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams() = mapOf("user_id" to userId, "restaurant_id" to restaurantId.toString())
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun unsaveRestaurant(btnSave: ImageView) {
        val userId = databaseHelper.getUserId() ?: return
        val url = "${Global.BASE_URL}saved(delete).php"

        val request = object : StringRequest(Request.Method.POST, url, { response ->
            try {
                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    isSaved = false
                    updateSaveIcon(btnSave, false)
                    Toast.makeText(this, "Unsaved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, {
            Toast.makeText(this, "Connection Error", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams() = mapOf("user_id" to userId, "restaurant_id" to restaurantId.toString())
        }
        Volley.newRequestQueue(this).add(request)
    }
}