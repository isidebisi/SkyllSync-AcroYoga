package com.example.skyllsync

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SkillListItemAdapter(private val context: Context,
                           var items: ArrayList<String>,
                           var itemsPriorityLvl: ArrayList<Int>,
                           var itemsProgressLvl: ArrayList<Int>,
                           private val listener: OnItemClickListener) :
    RecyclerView.Adapter<SkillListItemAdapter.ViewHolder>() {

    /**
     * A ViewHolder describes an item view and metadata about
    its place within the RecyclerView.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each item to
        var tvItem = view.findViewById<TextView>(R.id.skillNameTextView)
        var tvItemProgress = view.findViewById<ProgressBar>(R.id.progressBar)
        var tvItemPriority = view.findViewById<ImageView>(R.id.priorityMark)
        var deleteButton = view.findViewById<ImageView>(R.id.deleteSkillButton)
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(
                R.layout.item_skill_row,
                parent,
                false
            )
        )
    }





    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item_position = items.get(position)
        val progress = itemsProgressLvl[position] * 20
        val priorityDrawableName = SkillClass.getPriorityPathFromNumber(itemsPriorityLvl[position])
        val priorityDrawableId = context.resources.getIdentifier(priorityDrawableName, "drawable", context.packageName)


        holder.tvItem.text = item_position
        holder.tvItemProgress.progress = progress
        holder.tvItemPriority.setImageResource(priorityDrawableId)
        holder.itemView.setOnClickListener { listener.onItemClick(position) }
        holder.deleteButton.setOnClickListener { listener.onDeleteClick(position) }
    }

    override fun getItemCount() = items.size
}

