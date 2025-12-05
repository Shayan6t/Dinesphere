package com.example.dinesphere

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.util.Locale

class location1 : AppCompatActivity() {

    private lateinit var addressText: TextView
    private lateinit var editLocation: EditText
    private lateinit var backBtn: ImageButton
    private lateinit var changeBtn: ImageButton
    private lateinit var listDownBtn: ImageButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseHelper: DatabaseHelper

    // Store current coordinates
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location1)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Views
        addressText = findViewById(R.id.address_text)
        editLocation = findViewById(R.id.edit_location)
        backBtn = findViewById(R.id.back)
        changeBtn = findViewById(R.id.change_btn)
        listDownBtn = findViewById(R.id.list_down)

        // Back Button
        backBtn.setOnClickListener {
            finish()
        }

        // Change/Save Button (Manual save)
        changeBtn.setOnClickListener {
            val manualAddress = editLocation.text.toString().trim()
            if (manualAddress.isNotEmpty()) {
                val lat = currentLat ?: 0.0
                val lng = currentLng ?: 0.0
                updateLocationInDB(lat, lng, manualAddress)
            } else {
                Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
            }
        }

        // List Down Button - Show Nearby Locations
        listDownBtn.setOnClickListener { view ->
            if (currentLat != null && currentLng != null) {
                showNearbyPlaces(view, currentLat!!, currentLng!!)
            } else {
                Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show()
            }
        }

        // Start GPS Fetch
        checkPermissionsAndGetLocation()
    }

    private fun checkPermissionsAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        fetchLocation()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                fetchLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                addressText.text = "Permission denied"
            }
        }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        addressText.text = "Fetching location..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude

                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0].getAddressLine(0)

                        addressText.text = address
                        editLocation.setText(address)

                        // Save automatically
                        updateLocationInDB(location.latitude, location.longitude, address)
                    } else {
                        addressText.text = "Address not found"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    addressText.text = "Error fetching address"
                }
            } else {
                addressText.text = "Unable to get location. Turn on GPS."
            }
        }
    }

    private fun showNearbyPlaces(view: View, lat: Double, lng: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                // Fetch 5 nearby address variations
                val addresses = geocoder.getFromLocation(lat, lng, 5)

                runOnUiThread {
                    if (!addresses.isNullOrEmpty()) {
                        val listAddresses = addresses.map { it.getAddressLine(0) }

                        val listPopupWindow = ListPopupWindow(this)

                        // Anchor directly to the edit text box
                        listPopupWindow.anchorView = findViewById(R.id.edit_location)

                        // Force width to match the input box so it looks centered and full width
                        listPopupWindow.width = findViewById<View>(R.id.edit_location).width

                        // Set Background Black
                        listPopupWindow.setBackgroundDrawable(ColorDrawable(Color.BLACK))

                        // Use Custom Item Layout (Black bg, Centered text)
                        val adapter = ArrayAdapter(this, R.layout.item_dropdown, listAddresses)
                        listPopupWindow.setAdapter(adapter)

                        // Optional: Ensure it overlaps nicely
                        listPopupWindow.isModal = true

                        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
                            val selectedAddress = listAddresses[position]
                            addressText.text = selectedAddress
                            editLocation.setText(selectedAddress)

                            updateLocationInDB(lat, lng, selectedAddress)
                            listPopupWindow.dismiss()
                        }

                        listPopupWindow.show()

                    } else {
                        Toast.makeText(this, "No specific locations found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error finding places", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updateLocationInDB(lat: Double, lng: Double, address: String) {
        val userId = databaseHelper.getUserId()
        if (userId == null) return

        val url = Global.BASE_URL + "update_location.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    if (success) {
                        Log.d("LocationDebug", "Location saved to DB")
                    } else {
                        Log.e("LocationDebug", "Failed to save location")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener {
                Log.e("LocationDebug", "Connection failed")
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
}