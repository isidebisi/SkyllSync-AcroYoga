package com.example.skyllsync

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class MyProfileViewModel : ViewModel() {

    val _uploadSuccess = MutableLiveData<Boolean?>()
    val uploadSuccess : LiveData<Boolean?>
        get() = _uploadSuccess

    var key : String = ""
    var imageUri : Uri? = null

    //Firebase
    val database = FirebaseDatabase.getInstance()
    val profileRef = database.getReference("Profiles")
    var storageRef = FirebaseStorage.getInstance().getReference()


    fun modifyProfileImage(context : Context?){
        if(imageUri != null){

            val imageByteArray = context?.contentResolver?.openInputStream(imageUri!!)?.readBytes()
            val profileImageRef = storageRef.child("ProfileImages/" + key + ".jpg")

            // Upload the image to Firebase
            val uploadTask = imageByteArray?.let { profileImageRef.putBytes(it) }

            uploadTask?.addOnFailureListener {
                _uploadSuccess.value = false
            }?.addOnSuccessListener { taskSnapshot ->
                profileRef.child(key).child("photo URL").setValue(
                    (FirebaseStorage.getInstance()
                        .getReference()).toString() + "ProfileImages/" + key + ".jpg"
                )
                _uploadSuccess.value = true
            }
        } else {
            _uploadSuccess.value = false
        }
    }

    fun resetUploadSuccess(){
        _uploadSuccess.value = null
    }
}