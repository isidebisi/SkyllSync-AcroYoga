package com.example.skyllsync

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//RecyclerView Adapter for the categories. Inspired from labs.
class CategoryItemAdapter(val context: Context,
                          var itemsCategories: ArrayList<String>,
                          var itemsNoSkills: ArrayList<Int>,
                          private val listener: OnItemClickListener) :
    RecyclerView.Adapter<CategoryItemAdapter.ViewHolder>()
{

    /**
     * A ViewHolder describes an item view and metadata about
    its place within the RecyclerView.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each item to
        var tvItem = view.findViewById<TextView>(R.id.categoryNameTextView)
        var tvItemNoSkills = view.findViewById<TextView>(R.id.categoryNoSkillsTextView)
        var deleteButton = view.findViewById<ImageView>(R.id.deleteCategoryButton)
    }

    //Two OnClickListeners are implemented: One for the whole item and one for the delete button.
    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    /**
     * Inflates the item views which is designed in xml layout file
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_categories_row,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item_position = itemsCategories.get(position)
        val item_position_no_skills = itemsNoSkills.get(position).toString() +" skills"

        holder.tvItemNoSkills.text = item_position_no_skills
        holder.tvItem.text = item_position
        holder.itemView.setOnClickListener { listener.onItemClick(position) }
        holder.deleteButton.setOnClickListener { listener.onDeleteClick(position) }
    }
    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return itemsCategories.size}
}

