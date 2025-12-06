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
    private var phoneNumber: String? = null

    // Coordinates for Restaurant
    private var restaurantLat: Double = 0.0
    private var restaurantLng: Double = 0.0

    // Coordinates for User's Current Location (Fetched on creation)
    private var userLat: Double? = null
    private var userLng: Double? = null

    // Views
    private lateinit var txtTime: TextView
    private lateinit var btnCar: LinearLayout
    private lateinit var btnCycle: LinearLayout
    private lateinit var btnWalk: LinearLayout
    private lateinit var btnCall: LinearLayout
    private lateinit var btnRoutes: ImageButton
    private lateinit var btnViewMenu: TextView // ADDED: View Menu button

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

        txtTime = findViewById(R.id.deliveryTime)
        btnCar = findViewById(R.id.btn_car)
        btnCycle = findViewById(R.id.btn_cycle)
        btnWalk = findViewById(R.id.btn_walk)
        btnCall = findViewById(R.id.btn_call)
        btnRoutes = findViewById(R.id.routes_btn)
        btnViewMenu = findViewById(R.id.menu_text) // INITIALIZED: View Menu button

        // 2. Extract Data passed from Homepage
        restaurantId = intent.getIntExtra("RESTAURANT_ID", -1)
        val name = intent.getStringExtra("NAME") ?: "Unknown Restaurant"
        val address = intent.getStringExtra("ADDRESS") ?: "Address"
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val rating = intent.getFloatExtra("RATING", 4.0f)
        distanceKm = intent.getDoubleExtra("DISTANCE", 0.0)
        isSaved = intent.getBooleanExtra("IS_SAVED", false)
        phoneNumber = intent.getStringExtra("PHONE")

        // Extract Restaurant Location
        restaurantLat = intent.getDoubleExtra("LAT", 0.0)
        restaurantLng = intent.getDoubleExtra("LNG", 0.0)

        // NOTE: If user coordinates (USER_LAT, USER_LNG) were passed from homepage,
        // they should be extracted here to avoid the extra network call in loadUserLocation().
        // Example: userLat = intent.getDoubleExtra("USER_LAT", 0.0)

        // 3. Load User Location (Required for Directions if not passed in Intent)
        loadUserLocation()

        // 4. Populate Views & Set Initial State
        txtName.text = name
        txtLocation.text = address
        txtRating.text = rating.toString()
        if (!imageUrl.isNullOrEmpty()) { Picasso.get().load(imageUrl).fit().centerCrop().into(imgRestaurant) } else { imgRestaurant.setImageResource(R.drawable.img1) }
        updateSaveIcon(btnSave, isSaved)
        updateTimeAndSelection(btnCar, 30) // Default Mode (Car)

        // --- 5. ROUTES BUTTON LOGIC ---
        btnRoutes.setOnClickListener {
            openGoogleMapsDirections()
        }
        // --- END ROUTES LOGIC ---

        // --- 6. VIEW MENU BUTTON LOGIC ---
        btnViewMenu.setOnClickListener {
            // Launch the menu activity, passing necessary IDs and location data
            val intent = Intent(this, menu::class.java)
            intent.putExtra("RESTAURANT_ID", restaurantId)
            // Pass user coordinates if available
            userLat?.let { intent.putExtra("USER_LAT", it) }
            userLng?.let { intent.putExtra("USER_LNG", it) }
            startActivity(intent)
        }
        // --- END MENU LOGIC ---


        // --- Other Click Listeners ---
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
        btnSave.setOnClickListener { if (isSaved) unsaveRestaurant(btnSave) else saveRestaurant(btnSave) }
    }

    /**
     * Fetches the user's saved location (lat/lng) from the database to use as the starting point
     * for directions.
     */
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
                        // User location not critical for viewing menu/details, but toast is useful.
                        Toast.makeText(this, "Current location data missing", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Do nothing if connection fails, user won't be able to open map directions precisely.
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Opens Google Maps showing directions from the user's saved location to the restaurant.
     */
    private fun openGoogleMapsDirections() {
        if (userLat == null || userLng == null || restaurantLat == 0.0 || restaurantLng == 0.0) {
            Toast.makeText(this, "Waiting for location data. Try again in a moment.", Toast.LENGTH_LONG).show()
            return
        }

        // Construct the Google Maps directions URL (Using intent for better compatibility)
        val uri = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$userLat,$userLng" +
                "&destination=$restaurantLat,$restaurantLng" +
                "&travelmode=driving"

        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        mapIntent.setPackage("com.google.android.apps.maps") // Force it to open Google Maps

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            // Fallback if Google Maps app is not installed (opens in browser)
            mapIntent.setPackage(null)
            startActivity(mapIntent)
        }
    }


    // --- UTILITY AND API FUNCTIONS (Unchanged) ---

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