package com.example.dinesphere

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class location2 : AppCompatActivity() {

    private lateinit var editStreet: EditText
    private lateinit var editHouse: EditText
    private lateinit var saveBtn: ImageButton
    private lateinit var backBtn: ImageButton
    private lateinit var map: MapView // Changed to MapView
    private lateinit var databaseHelper: DatabaseHelper

    private var lat: Double = 0.0
    private var lng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize OSM Configuration (Critical step!)
        // This handles caching and user agent settings for OpenStreetMap
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))

        enableEdgeToEdge()
        setContentView(R.layout.activity_location2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this)

        // Initialize Views
        editStreet = findViewById(R.id.edit_street)
        editHouse = findViewById(R.id.edit_house)
        saveBtn = findViewById(R.id.save_btn)
        backBtn = findViewById(R.id.back)
        map = findViewById(R.id.map) // This is now a MapView

        // Hide static placeholders
        findViewById<TextView>(R.id.street_text).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.house_text).visibility = android.view.View.GONE

        // Get Data
        val addressFromLoc1 = intent.getStringExtra("CURRENT_ADDRESS") ?: ""
        lat = intent.getDoubleExtra("LAT", 0.0)
        lng = intent.getDoubleExtra("LNG", 0.0)

        // 2. Setup the Map
        if (lat != 0.0 && lng != 0.0) {
            setupMap(lat, lng)
        }

        // Logic to Split Address
        if (addressFromLoc1.contains(",")) {
            val parts = addressFromLoc1.split(",", limit = 2)
            editHouse.setText(parts[0].trim())
            editStreet.setText(parts[1].trim())
        } else {
            editStreet.setText(addressFromLoc1)
        }

        // Back Button
        backBtn.setOnClickListener {
            finish()
        }

        // Save Button Logic
        saveBtn.setOnClickListener {
            val street = editStreet.text.toString().trim()
            val house = editHouse.text.toString().trim()

            if (street.isEmpty()) {
                Toast.makeText(this, "Please enter street address", Toast.LENGTH_SHORT).show()
            } else {
                val fullAddress = if (house.isNotEmpty()) "$house, $street" else street
                saveLocationToDB(fullAddress)
            }
        }
    }

    private fun setupMap(latitude: Double, longitude: Double) {
        map.setTileSource(TileSourceFactory.MAPNIK) // Standard OSM map style
        map.setMultiTouchControls(true) // Allow zooming with fingers

        val mapController = map.controller
        mapController.setZoom(18.0) // Set zoom level (18 is good for streets)

        val startPoint = GeoPoint(latitude, longitude)
        mapController.setCenter(startPoint)

        // Add a Red Marker
        val marker = Marker(map)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Selected Location"
        marker.icon = resources.getDrawable(R.drawable.map_pin_o, null)

        map.overlays.add(marker)
        map.invalidate() // Refresh map
    }

    private fun saveLocationToDB(address: String) {
        val userId = databaseHelper.getUserId()
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val url = Global.BASE_URL + "update_location.php"

        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    if (success) {
                        Toast.makeText(this, "Location Saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId
                params["latitude"] = lat.toString()
                params["longitude"] = lng.toString()
                params["address"] = address
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    // Required for OSMdroid lifecycle management
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}