package com.example.dinesphere

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class ReviewAdapter(
    private var reviews: List<ReviewModel>
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val restaurantImage: ImageView = itemView.findViewById(R.id.restaurantImage)
        val restaurantName: TextView = itemView.findViewById(R.id.restaurantName)
        val location: TextView = itemView.findViewById(R.id.location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        // Reuse the restaurant item layout from your static XML
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        holder.restaurantName.text = review.businessName
        holder.location.text = review.address ?: "Address not available"

        // Load restaurant image
        if (!review.restaurantImage.isNullOrEmpty()) {
            Picasso.get()
                .load(review.restaurantImage)
                .placeholder(R.drawable.fork)
                .error(R.drawable.fork)
                .into(holder.restaurantImage)
        } else {
            holder.restaurantImage.setImageResource(R.drawable.fork)
        }

        // Click to open feedback page
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, feedback::class.java)
            intent.putExtra("RESTAURANT_ID", review.restaurantId)
            intent.putExtra("RESTAURANT_NAME", review.businessName)
            intent.putExtra("RESTAURANT_ADDRESS", review.address)
            intent.putExtra("EXISTING_RATING", review.rating)
            intent.putExtra("EXISTING_COMMENT", review.comment)
            intent.putExtra("REVIEW_ID", review.reviewId)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = reviews.size

    fun updateReviews(newReviews: List<ReviewModel>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
}