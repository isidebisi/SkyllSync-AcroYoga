package com.example.skyllsync

//class used to tranmit media information.
//It is separated in the URL and the type of media (video or image)
data class MediaItem(val url: String, val isVideo: Boolean)
