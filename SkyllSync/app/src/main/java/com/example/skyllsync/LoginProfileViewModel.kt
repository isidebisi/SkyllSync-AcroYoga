package com.example.skyllsync

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

//In our implementation the key is the same String as the username.
//Every username has to be unique so we can use it as a key.
//This decision was taken later on during the project.
// That's why we rely on the username and the key even if they are the same.
// This structure was mostly inspired from the labs.
class LoginProfileViewModel : ViewModel() {
    // USER DATA
    var imageUri: Uri?
    var username: String
    var key: String = ""
    var password: String = ""
    private val _uploadSuccess = MutableLiveData<Boolean?>()
    val uploadSuccess: LiveData<Boolean?>
        get() = _uploadSuccess

    private val _profilePresent = MutableLiveData<Boolean?>()
    val profilePresent: LiveData<Boolean?>
        get() = _profilePresent

    private val _usernameExist = MutableLiveData<Boolean?>()
    val usernameExist: LiveData<Boolean?>
        get() = _usernameExist

    // FIREBASE
    var storageRef = FirebaseStorage.getInstance().getReference()
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")


    init {
        imageUri = null
        username = ""
    }

    fun sendDataToFireBase(context: Context?) {
        var uploadSuccess: Boolean = false

        profileRef.child(username).child("username").setValue(username)
        profileRef.child(username).child("password").setValue(password)
        profileRef.child(username).child("notifications").child("numberOfNotifications").setValue(0)

        val profileImageRef = storageRef.child("ProfileImages/" + username + ".jpg")

        if (imageUri != null) {
            val imageByteArray = context?.contentResolver?.openInputStream(imageUri!!)?.readBytes()

            // Upload the image to Firebase
            val uploadTask = imageByteArray?.let { profileImageRef.putBytes(it) }

            uploadTask?.addOnFailureListener {
                _uploadSuccess.value = false
            }?.addOnSuccessListener { taskSnapshot ->
                profileRef.child(key).child("photo URL").setValue(
                    (FirebaseStorage.getInstance()
                        .getReference()).toString() + "ProfileImages/" + username + ".jpg"
                )
                _uploadSuccess.value = true
            }
        } else {
            _uploadSuccess.value = false
        }
    }

    fun checkUsername(){
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            var exists: Boolean = false
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username").getValue(String::class.java)
                    if (usernameDatabase!= null && username == usernameDatabase) {
                        exists = true
                        break
                    }
                }
                _usernameExist.value = exists != false
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun fetchProfile() {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username").getValue(String::class.java)
                    if (usernameDatabase!= null && username == usernameDatabase) {
                        val passwordDatabase = user.child("password").getValue(String::class.java)
                        if (passwordDatabase!= null && password == passwordDatabase) {
                            key = user.key.toString()
                            _profilePresent.value = true
                            break
                        }
                    }
                }
                if(_profilePresent.value != true){
                    _profilePresent.value = false
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun resetUserData(){
        //username = ""
        //password = ""
        //imageUri = null
        //key = ""
        _uploadSuccess.value = null
        _profilePresent.value = null
        _usernameExist.value = null
    }

    fun createDefaultCategories(context: Context) {
        profileRef.child(key).child("categories").child(context.getString(R.string.flows)).child("numberOfSkills").setValue(0)
        profileRef.child(key).child("categories").child(context.getString(R.string.poses)).child("numberOfSkills").setValue(0)
        profileRef.child(key).child("categories").child(context.getString(R.string.whips)).child("numberOfSkills").setValue(0)
        profileRef.child(key).child("categories").child(context.getString(R.string.pops)).child("numberOfSkills").setValue(0)
    }
}