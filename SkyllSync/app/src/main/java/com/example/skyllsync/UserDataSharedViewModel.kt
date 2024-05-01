package com.example.skyllsync

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// This class is used to share data between fragments
//This helps in going through the catalogue of skills and categories of a user
class UserDataSharedViewModel : ViewModel() {

    private val _userImageReady = MutableLiveData<Boolean>()
    val userImageReady: LiveData<Boolean>
        get() = _userImageReady

    val loggedInUser = MutableLiveData<String>()
    val loggedInUserKey = MutableLiveData<String>()
    val loggedInUserImage = MutableLiveData<Uri?>()
    val chosenCategory = MutableLiveData<String>()
    val chosenSkill = MutableLiveData<String>()

    val mediaUri = MutableLiveData<Uri?>()
    val popBackStackOnce = MutableLiveData<Boolean>()


    private val database = FirebaseDatabase.getInstance()

    private var myRef: DatabaseReference? = null
    private var notificationListener: ValueEventListener? = null
    var numberOfNotifications = MutableLiveData<Int?>()
    var pairUpList = ArrayList<String>()

    init {
        loggedInUser.observeForever { user ->
            // Remove old listener
            myRef?.removeEventListener(notificationListener!!)

            // Add new listener
            myRef = database.getReference("Profiles").child(user).child("notifications")
            notificationListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    pairUpList.clear()

                    for (notifSnapshot in dataSnapshot.children) {
                        val pairUpRequest = notifSnapshot.value.toString()
                        pairUpList.add(pairUpRequest)
                    }

                    if(pairUpList.isNotEmpty()) pairUpList.removeLast()
                    Log.v("UserDataSharedViewModel", "Children : $pairUpList")

                    val value = dataSnapshot.child("numberOfNotifications").getValue(Int::class.java)
                    numberOfNotifications.value = value
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.v("UserDataSharedViewModel", "Failed to read value.", error.toException())
                }
            }
            myRef?.addValueEventListener(notificationListener!!)
            Log.v("UserDataSharedViewModel", "Logged in user changed to $user")
        }
    }

    fun addTraining(duration: Long){
        val user = loggedInUser.value

        if (user != null && user != "") {
            val sdf = SimpleDateFormat("yyyy:MM:dd:hh:mm:ss:SSS", Locale.getDefault())
            val currentDate = sdf.format(Date(System.currentTimeMillis()))
            database.getReference("Profiles").child(user).child("trainings")
                .child(currentDate).setValue(duration)

        }

    }

    fun getUserImage(context: Context?){
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("ProfileImages").child(loggedInUserKey.value.toString()+".jpg")
        imageRef.downloadUrl.addOnSuccessListener {
            //download image from cloud
            loggedInUserImage.value = it
            _userImageReady.value = true
        }.addOnFailureListener {
            // Handle any errors
            Toast.makeText(context, "Error downloading image", Toast.LENGTH_SHORT).show()
        }
    }

    fun selectUser(user: String) {
        loggedInUser.value = user
    }

    fun selectUserKey(userKey: String) {
        loggedInUserKey.value = userKey
    }

    fun selectUserImage(userImage: Uri) {
        loggedInUserImage.value = userImage
    }

    fun selectCategory(category: String) {
        chosenCategory.value = category
    }

    fun selectSkill(skill: String) {
        chosenSkill.value = skill
    }

    fun setPopBackStackOnce() {
        popBackStackOnce.value = true
    }
    fun resetPopBackStackOnce() {
        popBackStackOnce.value = false
    }
    fun selectMediaUri(uri: Uri?) {
        mediaUri.value = uri
    }
    fun resetMediaUri() {
        mediaUri.value = null
    }

    fun resetUserImageReady() {
        _userImageReady.value = false
    }
    fun resetUserData() {
        loggedInUser.value = ""
        loggedInUserKey.value = ""
        loggedInUserImage.value = null
        chosenCategory.value = ""
        chosenSkill.value = ""
    }

}