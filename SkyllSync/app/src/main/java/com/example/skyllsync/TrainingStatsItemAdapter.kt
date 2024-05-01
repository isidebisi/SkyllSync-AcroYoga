package com.example.skyllsync

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

//RecyclerView Adapter for the categories. Inspired from labs.
class TrainingStatsItemAdapter( val context: Context,
                                var itemsDate: ArrayList<String>,
                                var itemsDuration: ArrayList<Long>,
                                private val listener: OnItemClickListener) :
    RecyclerView.Adapter<TrainingStatsItemAdapter.ViewHolder>()
{
    /**
     * A ViewHolder describes an item view and metadata about
    its place within the RecyclerView.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each item to
        var tvItem = view.findViewById<TextView>(R.id.trainingDateTextView)
        var tvItemDuration = view.findViewById<TextView>(R.id.trainingDurationTextView)
        var deleteButton = view.findViewById<ImageView>(R.id.deleteTrainingButton)
    }

    //Two OnClickListeners are implemented: One for the whole item and one for the delete button.
    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy MM dd")
    @RequiresApi(Build.VERSION_CODES.O)
    private val durationFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Inflates the item views which is designed in xml layout file
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_training_row,
                parent,
                false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item_position = itemsDate.get(position)
        val item_position_duration = itemsDuration.get(position)

        // Split the date string
        val dateParts = item_position.split(":")
        val year = dateParts[0]
        val month = dateParts[1]
        val day = dateParts[2]

        // Create a date string in the format "yyyy MM dd"
        val dateString = "$year $month $day"

        // Parse the date string to a Date object
        val inputFormat = SimpleDateFormat("yyyy MM dd", Locale.getDefault())
        val date = inputFormat.parse(dateString)

        // Format the Date object to the desired format
        val outputFormat = SimpleDateFormat("MMMM dd yyyy", Locale.ENGLISH)
        var formattedDate = outputFormat.format(date)
        formattedDate = formattedDate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val durationParts = LocalTime.ofNanoOfDay(item_position_duration * 1_000_000)
                            .format(durationFormatter).toString().split(":")
        val hours = durationParts[0]
        val minutes = durationParts[1]
        val formattedDuration = "$hours h $minutes min"

        holder.tvItem.text = formattedDate
        holder.tvItemDuration.text = formattedDuration
        holder.deleteButton.setOnClickListener { listener.onDeleteClick(position) }
    }
    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return itemsDate.size}
}
