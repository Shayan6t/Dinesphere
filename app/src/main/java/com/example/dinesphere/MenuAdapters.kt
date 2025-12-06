package com.example.dinesphere

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

// 1. Adapter for Categories
class CategoryAdapter(
    private var categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.category_name)
        val card: CardView = itemView.findViewById(R.id.card_container)

        fun bind(category: Category) {
            name.text = category.categoryName

            // Highlight selected category
            if (category.isSelected) {
                card.setCardBackgroundColor(Color.parseColor("#F36600")) // Orange
                name.setTextColor(Color.WHITE)
            } else {
                card.setCardBackgroundColor(Color.parseColor("#333333")) // Dark Gray
                name.setTextColor(Color.LTGRAY)
            }

            itemView.setOnClickListener {
                // Update selection logic
                categories.forEach { it.isSelected = false }
                category.isSelected = true
                notifyDataSetChanged()

                onCategoryClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    fun updateData(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}

// 2. Adapter for Menu Items
class MenuAdapter(
    private var menuItems: List<MenuItem>
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.menu_image)
        val name: TextView = itemView.findViewById(R.id.menu_name)
        val desc: TextView = itemView.findViewById(R.id.menu_desc)
        val price: TextView = itemView.findViewById(R.id.menu_price)

        fun bind(item: MenuItem) {
            name.text = item.menuName
            desc.text = item.menuDescription
            price.text = "$ ${item.menuPrice}"

            if (!item.menuImage.isNullOrEmpty()) {
                Picasso.get().load(item.menuImage).placeholder(R.drawable.img).into(image)
            } else {
                image.setImageResource(R.drawable.img)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount() = menuItems.size

    fun updateData(newItems: List<MenuItem>) {
        menuItems = newItems
        notifyDataSetChanged()
    }
}