package com.example.dinesphere

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class RestaurantAdapter(
    private var restaurants: List<Restaurant>,
    private val onItemClick: (Restaurant) -> Unit,
    private val onSaveClick: ((Restaurant, Int) -> Unit)? = null
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    inner class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val restaurantImage: ImageView = itemView.findViewById(R.id.restaurantImage)
        val restaurantName: TextView = itemView.findViewById(R.id.restaurantName)
        val distance: TextView = itemView.findViewById(R.id.distance)
        val location: TextView = itemView.findViewById(R.id.location)
        val discount: TextView = itemView.findViewById(R.id.discount)
        val saveBtn: ImageView = itemView.findViewById(R.id.save)
        val star1: ImageView = itemView.findViewById(R.id.star1)
        val star2: ImageView = itemView.findViewById(R.id.star2)
        val star3: ImageView = itemView.findViewById(R.id.star3)
        val star4: ImageView = itemView.findViewById(R.id.star4)
        val star5: ImageView = itemView.findViewById(R.id.star5)

        fun bind(restaurant: Restaurant, position: Int) {
            restaurantName.text = restaurant.businessName
            distance.text = String.format("%.1f km away", restaurant.distanceKm)
            location.text = restaurant.address

            // Load image using Picasso with Cloudinary URL
            if (!restaurant.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(restaurant.imageUrl)
                    .resize(400, 340)
                    .centerCrop()
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .into(restaurantImage)
            } else {
                restaurantImage.setImageResource(R.drawable.img)
            }

            // Show discount if available
            if (!restaurant.discount.isNullOrEmpty()) {
                discount.visibility = View.VISIBLE
                discount.text = "${restaurant.discount}% off"
            } else {
                discount.visibility = View.GONE
            }

            // Set rating stars
            setRating(restaurant.rating)

            // Update save icon based on saved status
            updateSaveIcon(restaurant.isSaved)

            // Click listener for restaurant item
            itemView.setOnClickListener {
                onItemClick(restaurant)
            }

            // Save button click listener
            saveBtn.setOnClickListener {
                onSaveClick?.invoke(restaurant, position)
            }
        }

        private fun setRating(rating: Float) {
            val stars = arrayOf(star1, star2, star3, star4, star5)
            val greenColor = 0xFF45B884.toInt()
            val whiteColor = ContextCompat.getColor(itemView.context, R.color.white)

            for (i in stars.indices) {
                if (i < rating.toInt()) {
                    stars[i].setColorFilter(greenColor)
                } else {
                    stars[i].setColorFilter(whiteColor)
                }
            }
        }

        private fun updateSaveIcon(isSaved: Boolean) {
            // Always use save_w icon, just change the tint color
            saveBtn.setImageResource(R.drawable.save_w)

            if (isSaved) {
                // Tint to orange when saved
                saveBtn.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.orange)
                )
            } else {
                // Tint to white when not saved
                saveBtn.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(view)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        holder.bind(restaurants[position], position)
    }

    override fun getItemCount(): Int = restaurants.size

    fun updateRestaurants(newRestaurants: List<Restaurant>) {
        restaurants = newRestaurants
        notifyDataSetChanged()
    }

    // Update the save status of a specific restaurant
    fun updateRestaurantSaveStatus(position: Int, isSaved: Boolean) {
        if (position in restaurants.indices) {
            (restaurants as? MutableList)?.get(position)?.isSaved = isSaved
            notifyItemChanged(position)
        }
    }
}