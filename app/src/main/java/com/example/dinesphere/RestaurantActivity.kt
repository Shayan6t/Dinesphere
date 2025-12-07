package com.example.dinesphere

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
    private var phoneNumber: String? = null

    private var restaurantLat: Double = 0.0
    private var restaurantLng: Double = 0.0

    private var userLat: Double? = null
    private var userLng: Double? = null

    // Views
    private lateinit var txtTime: TextView
    private lateinit var btnCar: LinearLayout
    private lateinit var btnCycle: LinearLayout
    private lateinit var btnWalk: LinearLayout
    private lateinit var btnCall: LinearLayout
    private lateinit var btnRoutes: ImageButton
    private lateinit var btnViewMenu: TextView

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navSaved: LinearLayout
    private lateinit var navReview: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant)

        databaseHelper = DatabaseHelper(this)

        // Initialize Views
        val imgRestaurant = findViewById<ImageView>(R.id.restaurant_img)
        val btnBack = findViewById<ImageButton>(R.id.back)
        val txtName = findViewById<TextView>(R.id.name)
        val txtLocation = findViewById<TextView>(R.id.location)
        val txtRating = findViewById<TextView>(R.id.ratingScore)
        val btnSave = findViewById<ImageView>(R.id.save_btn)

        txtTime = findViewById(R.id.deliveryTime)
        btnCar = findViewById(R.id.btn_car)
        btnCycle = findViewById(R.id.btn_cycle)
        btnWalk = findViewById(R.id.btn_walk)
        btnCall = findViewById(R.id.btn_call)
        btnRoutes = findViewById(R.id.routes_btn)
        btnViewMenu = findViewById(R.id.menu_text)

        // Initialize bottom navigation
        navHome = findViewById(R.id.home)
        navSaved = findViewById(R.id.saved)
        navReview = findViewById(R.id.review)
        navProfile = findViewById(R.id.profile)

        // Extract Data
        restaurantId = intent.getIntExtra("RESTAURANT_ID", -1)
        val name = intent.getStringExtra("NAME") ?: "Unknown Restaurant"
        val address = intent.getStringExtra("ADDRESS") ?: "Address"
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val rating = intent.getFloatExtra("RATING", 4.0f)
        distanceKm = intent.getDoubleExtra("DISTANCE", 0.0)
        isSaved = intent.getBooleanExtra("IS_SAVED", false)
        phoneNumber = intent.getStringExtra("PHONE")

        restaurantLat = intent.getDoubleExtra("LAT", 0.0)
        restaurantLng = intent.getDoubleExtra("LNG", 0.0)

        // Populate Views
        txtName.text = name
        txtLocation.text = address
        txtRating.text = rating.toString()
        if (!imageUrl.isNullOrEmpty()) {
            // Picasso automatically handles basic caching
            Picasso.get().load(imageUrl).fit().centerCrop().into(imgRestaurant)
        } else {
            imgRestaurant.setImageResource(R.drawable.img1)
        }
        updateSaveIcon(btnSave, isSaved)
        updateTimeAndSelection(btnCar, 30)

        // --- ONLINE / OFFLINE LOGIC ---
        if (isNetworkAvailable()) {
            loadUserLocation()
        } else {
            Toast.makeText(this, "Offline Mode: Some features limited", Toast.LENGTH_SHORT).show()
        }

        // Routes Button
        btnRoutes.setOnClickListener {
            if (isNetworkAvailable()) {
                trackRestaurantView() // Only track if online
            }
            openGoogleMapsDirections()
        }

        // View Menu Button
        btnViewMenu.setOnClickListener {
            val intent = Intent(this, menu::class.java)
            intent.putExtra("RESTAURANT_ID", restaurantId)
            userLat?.let { intent.putExtra("USER_LAT", it) }
            userLng?.let { intent.putExtra("USER_LNG", it) }
            startActivity(intent)
        }

        // Other Click Listeners
        btnCar.setOnClickListener { updateTimeAndSelection(btnCar, 30) }
        btnCycle.setOnClickListener { updateTimeAndSelection(btnCycle, 15) }
        btnWalk.setOnClickListener { updateTimeAndSelection(btnWalk, 5) }

        btnCall.setOnClickListener {
            if (!phoneNumber.isNullOrEmpty()) {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:$phoneNumber")
                startActivity(dialIntent)
            } else {
                Toast.makeText(this, "Restaurant phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            if (isSaved) unsaveRestaurant(btnSave) else saveRestaurant(btnSave)
        }

        // Bottom Navigation - HOME
        navHome.setOnClickListener {
            val intent = Intent(this, homepage::class.java)
            startActivity(intent)
            finish() // Finish this to prevent stacking
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

    // --- HELPER: Check Network ---
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun trackRestaurantView() {
        // Double check network before firing logic
        if (!isNetworkAvailable()) return

        val userId = databaseHelper.getUserId() ?: return

        val url = "${Global.BASE_URL}review(get).php?user_id=$userId&restaurant_id=$restaurantId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (!success) {
                        createInitialReviewEntry()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Ignore
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun createInitialReviewEntry() {
        if (!isNetworkAvailable()) return

        val userId = databaseHelper.getUserId() ?: return
        val url = "${Global.BASE_URL}review(post).php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        Log.d("RestaurantActivity", "Initial review entry created")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                Log.e("RestaurantActivity", "Failed to create review entry: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId,
                    "restaurant_id" to restaurantId.toString(),
                    "rating" to "1",
                    "comment" to ""
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadUserLocation() {
        val userId = databaseHelper.getUserId() ?: return
        val url = "${Global.BASE_URL}get_user_location.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        val data = json.getJSONObject("data")
                        userLat = data.getDouble("latitude")
                        userLng = data.getDouble("longitude")
                    } else {
                        Toast.makeText(this, "Current location data missing", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Ignore
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun openGoogleMapsDirections() {
        // If we don't have user location (likely due to offline mode)
        if (userLat == null || userLng == null) {
            if (restaurantLat != 0.0 && restaurantLng != 0.0) {
                // We can at least open maps to the destination, just without a specific start point
                val uri = "geo:$restaurantLat,$restaurantLng?q=$restaurantLat,$restaurantLng($restaurantLat,$restaurantLng)"
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                mapIntent.setPackage("com.google.android.apps.maps")
                try {
                    startActivity(mapIntent)
                } catch (e: Exception) {
                    mapIntent.setPackage(null)
                    startActivity(mapIntent)
                }
                return
            }

            Toast.makeText(this, "Location unavailable offline.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$userLat,$userLng" +
                "&destination=$restaurantLat,$restaurantLng" +
                "&travelmode=driving"

        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            mapIntent.setPackage(null)
            startActivity(mapIntent)
        }
    }

    private fun updateTimeAndSelection(selectedBtn: LinearLayout, speedKmh: Int) {
        val grayColor = ColorStateList.valueOf(Color.parseColor("#C7C7C7"))
        btnCar.backgroundTintList = grayColor
        btnCycle.backgroundTintList = grayColor
        btnWalk.backgroundTintList = grayColor

        selectedBtn.backgroundTintList = null

        if (distanceKm > 0) {
            val timeInMinutes = ceil((distanceKm / speedKmh) * 60).toInt()
            txtTime.text = "$timeInMinutes mins"
        } else {
            txtTime.text = "-- mins"
        }
    }

    private fun updateSaveIcon(imageView: ImageView, saved: Boolean) {
        // Always use save_w (white icon resource), just tint it
        // Ensure you have R.drawable.save_w or similar
        imageView.setImageResource(R.drawable.save_w)

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

        // --- OFFLINE CHECK ---
        if (!isNetworkAvailable()) {
            databaseHelper.addPendingAction("SAVE", restaurantId)
            isSaved = true
            updateSaveIcon(btnSave, true)
            Toast.makeText(this, "Saved (Offline)", Toast.LENGTH_SHORT).show()
            return
        }
        // ---------------------

        val url = "${Global.BASE_URL}saved(post).php"

        val request = object : StringRequest(Request.Method.POST, url, { response ->
            try {
                if (JSONObject(response).optBoolean("success")) {
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

        // --- OFFLINE CHECK ---
        if (!isNetworkAvailable()) {
            databaseHelper.addPendingAction("UNSAVE", restaurantId)
            isSaved = false
            updateSaveIcon(btnSave, false)
            Toast.makeText(this, "Unsaved (Offline)", Toast.LENGTH_SHORT).show()
            return
        }
        // ---------------------

        val url = "${Global.BASE_URL}saved(delete).php"

        val request = object : StringRequest(Request.Method.POST, url, { response ->
            try {
                if (JSONObject(response).optBoolean("success")) {
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