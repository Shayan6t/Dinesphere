package com.example.dinesphere

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import org.json.JSONObject

class edit_profile : AppCompatActivity() {

    private lateinit var firstText: EditText
    private lateinit var lastText: EditText
    private lateinit var emailText: EditText
    private lateinit var genderText: EditText
    private lateinit var phoneText: EditText
    private lateinit var profileImage: ImageView
    private lateinit var saveBtn: RelativeLayout
    private lateinit var backBtn: ImageButton
    private lateinit var saveBtnBack: ImageButton

    private var currentUserId: String? = null
    // This variable will hold the valid URL (either old one from DB or new simulated one).
    private var currentProfileImageUrl: String? = null

    // Activity launcher for picking images
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                // --- CRITICAL: OVERWRITE URL WITH NEW SIMULATED URL ---
                // This simulates the actual upload response.
                val simulatedCloudinaryUrl = "https://res.cloudinary.com/dinesphere/image/upload/v1/users/profile_new_selected_${currentUserId}_${System.currentTimeMillis()}"
                currentProfileImageUrl = simulatedCloudinaryUrl

                // Display new image locally
                profileImage.setImageURI(imageUri)

                Toast.makeText(this, "Image selected. Press Save to finalize.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        currentUserId = intent.getStringExtra("USER_ID")

        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: User ID not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize Views
        firstText = findViewById(R.id.firstText)
        lastText = findViewById(R.id.lastText)
        emailText = findViewById(R.id.emailText)
        genderText = findViewById(R.id.genderText)
        phoneText = findViewById(R.id.phoneText)
        saveBtn = findViewById(R.id.save_btn)
        backBtn = findViewById(R.id.back)
        saveBtnBack = findViewById(R.id.save_btn_back)
        profileImage = findViewById(R.id.profileImage)
        val cameraIcon = findViewById<ImageView>(R.id.cameraIcon)


        saveBtnBack.isClickable = false
        emailText.isEnabled = false

        // Load existing data
        loadProfileDetails(currentUserId!!)

        // Click listeners
        backBtn.setOnClickListener { finish() }

        saveBtn.setOnClickListener {
            handleSaveChanges()
        }

        // Image Picker Listener
        cameraIcon.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun loadProfileDetails(userId: String) {
        val url = "${Global.BASE_URL}get_user_details.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        val user = json.getJSONObject("user")

                        // Fill text fields
                        firstText.setText(user.optString("first_name", ""))
                        lastText.setText(user.optString("last_name", ""))
                        emailText.setText(user.optString("email", ""))
                        genderText.setText(user.optString("gender", ""))
                        phoneText.setText(user.optString("phone", ""))

                        // Load Profile Image
                        val imageUrl = user.optString("profile_image_url")
                        if (!imageUrl.isNullOrEmpty()) {
                            // Store existing URL into the current URL holder
                            currentProfileImageUrl = imageUrl
                            Picasso.get().load(imageUrl)
                                .placeholder(R.drawable.avatar)
                                .error(R.drawable.avatar)
                                .into(profileImage)
                        } else {
                            // If no URL exists, ensure our holder is null
                            currentProfileImageUrl = null
                        }
                    } else {
                        Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing server response.", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Connection error loading profile.", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun handleSaveChanges() {
        val firstName = firstText.text.toString().trim()
        val lastName = lastText.text.toString().trim()
        val phone = phoneText.text.toString().trim()
        val gender = genderText.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "First Name, Last Name, and Phone are required.", Toast.LENGTH_LONG).show()
            return
        }

        // Pass the CURRENT value of currentProfileImageUrl (new or old)
        updateProfile(firstName, lastName, phone, gender, currentProfileImageUrl)
    }

    private fun updateProfile(firstName: String, lastName: String, phone: String, gender: String, imageUrl: String?) {
        val url = "${Global.BASE_URL}update_profile.php"
        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        Toast.makeText(this, "Profile Saved!", Toast.LENGTH_LONG).show()
                        finish() // Go back to the Profile display page
                    } else {
                        Toast.makeText(this, "Failed to save: ${json.optString("message")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing save response.", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Connection failed during save.", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = currentUserId!!
                params["first_name"] = firstName
                params["last_name"] = lastName
                params["phone"] = phone
                params["gender"] = gender
                // Send the current URL. If null, it sends empty string, which PHP treats as null.
                params["profile_image_url"] = imageUrl ?: ""
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }
}