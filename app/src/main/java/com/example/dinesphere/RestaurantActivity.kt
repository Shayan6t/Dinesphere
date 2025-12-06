package com.example.dinesphere

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso

class RestaurantActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant)

        // 1. Initialize Views from XML
        val imgRestaurant = findViewById<ImageView>(R.id.restaurant_img)
        val btnBack = findViewById<ImageButton>(R.id.back)
        val txtName = findViewById<TextView>(R.id.name)
        val txtLocation = findViewById<TextView>(R.id.location)
        val txtRating = findViewById<TextView>(R.id.ratingScore)
        val btnSave = findViewById<ImageView>(R.id.save_btn)

        // 2. Extract Data passed from Homepage
        val name = intent.getStringExtra("NAME") ?: "Unknown Restaurant"
        val address = intent.getStringExtra("ADDRESS") ?: "No Address"
        val imageUrl = intent.getStringExtra("IMAGE_URL") // This URL comes from your PHP API
        val rating = intent.getFloatExtra("RATING", 4.0f)
        val isSaved = intent.getBooleanExtra("IS_SAVED", false)

        // 3. Populate Views
        txtName.text = name
        txtLocation.text = address
        txtRating.text = rating.toString()

        // 4. Load Image using Picasso (Same way as Homepage)
        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(imageUrl)
                .fit()           // Resize to fit the ImageView
                .centerCrop()    // Crop center if needed
                .placeholder(R.drawable.img1) // Show while loading
                .error(R.drawable.img1)       // Show if URL fails
                .into(imgRestaurant)
        } else {
            // Fallback if no URL
            imgRestaurant.setImageResource(R.drawable.img1)
        }

        // 5. Set Save Icon Color
        if (isSaved) {
            btnSave.setColorFilter(ContextCompat.getColor(this, R.color.orange))
        } else {
            btnSave.setColorFilter(ContextCompat.getColor(this, R.color.white))
        }

        // 6. Back Button Logic
        btnBack.setOnClickListener {
            finish()
        }
    }
}