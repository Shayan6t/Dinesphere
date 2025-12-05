package com.example.dinesphere

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class SavedRestaurantAdapter(
    private val restaurants: List<Restaurant>,
    private val onRestaurantClick: (Restaurant) -> Unit,
    private val onUnsaveClick: (Restaurant, Int) -> Unit
) : RecyclerView.Adapter<SavedRestaurantAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.restaurant_container)
        val restaurantImage: ImageView = itemView.findViewById(R.id.restaurant_image)
        val restaurantName: TextView = itemView.findViewById(R.id.restaurant_name)
        val restaurantAddress: TextView = itemView.findViewById(R.id.restaurant_address)
        val timeText: TextView = itemView.findViewById(R.id.time_text)
        val saveIcon: ImageView = itemView.findViewById(R.id.save_icon)
        val star1: ImageView = itemView.findViewById(R.id.star1)
        val star2: ImageView = itemView.findViewById(R.id.star2)
        val star3: ImageView = itemView.findViewById(R.id.star3)
        val star4: ImageView = itemView.findViewById(R.id.star4)
        val star5: ImageView = itemView.findViewById(R.id.star5)

        init {
            container.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRestaurantClick(restaurants[position])
                }
            }

            saveIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUnsaveClick(restaurants[position], position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val restaurant = restaurants[position]

        // Set restaurant name and address
        holder.restaurantName.text = restaurant.businessName
        holder.restaurantAddress.text = restaurant.address

        // Set distance/time
        val timeInMinutes = (restaurant.distanceKm * 2).toInt() // Rough estimate
        holder.timeText.text = "$timeInMinutes mins"

        // Load restaurant image
        if (!restaurant.imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(restaurant.imageUrl)
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .into(holder.restaurantImage)
        } else {
            holder.restaurantImage.setImageResource(R.drawable.img)
        }

        // Set rating stars
        setRating(holder, restaurant.rating)
    }

    private fun setRating(holder: ViewHolder, rating: Float) {
        val stars = listOf(holder.star1, holder.star2, holder.star3, holder.star4, holder.star5)
        val filledStars = rating.toInt()

        stars.forEachIndexed { index, star ->
            if (index < filledStars) {
                star.setColorFilter(
                    star.context.getColor(R.color.green_star),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                star.setColorFilter(
                    star.context.getColor(R.color.white),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    override fun getItemCount(): Int = restaurants.size
}