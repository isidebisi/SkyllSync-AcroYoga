package com.example.skyllsync

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage

//Speciality for this recyclerView: it can display both images and videos
//for every item it chooses between ImageView and Media3 PlayerView
class SkillPictureItemAdapter(private val context: Context,
                              var mediaItems: ArrayList<MediaItem>,
                              private val listener: OnItemClickListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var ivItem = view.findViewById<ImageView>(R.id.skillPictureImageView)
    }

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var pvItem = view.findViewById<PlayerView>(R.id.skillVideoView)
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)

    }

    override fun getItemViewType(position: Int): Int {
        return if (mediaItems[position].isVideo) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            //for videos
            VideoViewHolder(LayoutInflater.from(context).inflate(
                R.layout.item_skill_video_row,
                parent,
                false
            ))
        } else {
            //for images
            ImageViewHolder(LayoutInflater.from(context).inflate(
                R.layout.item_skill_picture_row,
                parent,
                false
            ))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = mediaItems[position]

        if (item.isVideo) {
            //for videos
            val videoHolder = holder as VideoViewHolder
            val player = ExoPlayer.Builder(context).build()
            videoHolder.pvItem.player = player

            val storageReference = FirebaseStorage.getInstance().getReference(item.url)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                val exoPlayerMediaItem = androidx.media3.common.MediaItem.fromUri(uri)
                player.setMediaItem(exoPlayerMediaItem)
                player.prepare()
                player.playWhenReady = false //prevent automatic playback
            }
        } else {
            //for images
            val imageHolder = holder as ImageViewHolder
            val storageReference = FirebaseStorage.getInstance().getReference(item.url)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(context).load(uri).into(imageHolder.ivItem)
            }
        }

        holder.itemView.setOnClickListener { listener.onItemClick(position) }
    }

    override fun getItemCount() = mediaItems.size
}