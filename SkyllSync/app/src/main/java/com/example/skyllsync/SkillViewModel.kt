package com.example.skyllsync

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Patterns
import android.webkit.URLUtil
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage


class SkillViewModel : ViewModel() {

    private val _pairUpRequestSent = MutableLiveData<Boolean>()
    val pairUpRequestSent: MutableLiveData<Boolean>
        get() = _pairUpRequestSent

    private val _updateBinding = MutableLiveData<Boolean>()
    val updateBinding: MutableLiveData<Boolean>
        get() = _updateBinding

    val _mediaUploadSuccess = MutableLiveData<Boolean>()
    val mediaUploadSuccess: MutableLiveData<Boolean>
        get() = _mediaUploadSuccess

    var pairUpAccepted: Boolean = false

    var key: String? = ""
    var chosenSkill: String = ""
    var chosenCategory: String = ""
    var imageURLs: ArrayList<String> = ArrayList()
    var videoURLs: ArrayList<String> = ArrayList()
    var mediaURLs: ArrayList<MediaItem> = ArrayList()
    var skillProgression: Int = 0
    var skillPriority: Int = 0
    var skillPairUpStatus : Int = 0
    var skillPairUpName: String = ""
    var skillInstagramLink: String = ""

    var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")


    var itemAdapter: SkillPictureItemAdapter? = null

    //get skill data from database
    //This function can take information from other user in case of PairUp.
    fun getSkillData(context: Context?, loggedInUser: Boolean=true, otherUser: String="") {
        //check if data is taken from loggedIn user or paired up partner
        var localKey = key
        if(!loggedInUser){
            localKey = otherUser
        }

        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Fetch skill information
                val progressionValue = dataSnapshot.child(localKey.toString()).child("categories")
                    .child(chosenCategory).child("skills").child(chosenSkill)
                    .child("progression").value
                skillProgression = progressionValue?.toString()?.toIntOrNull() ?: 0

                val priorityValue = dataSnapshot.child(localKey.toString()).child("categories")
                    .child(chosenCategory).child("skills").child(chosenSkill)
                    .child("priority").value
                skillPriority = priorityValue?.toString()?.toIntOrNull() ?: 0

                skillPairUpStatus = dataSnapshot.child(localKey.toString()).child("categories")
                    .child(chosenCategory).child("skills").child(chosenSkill)
                    .child("pairUpStatus").value.toString().toInt()
                skillPairUpName = dataSnapshot.child(key.toString()).child("categories")
                    .child(chosenCategory).child("skills").child(chosenSkill)
                    .child("pairUpName").value.toString()
                skillInstagramLink = dataSnapshot.child(key.toString()).child("categories")
                    .child(chosenCategory).child("skills").child(chosenSkill)
                    .child("instagramLink").value.toString()

                // Fetch image URLs
                imageURLs.clear()
                for (iterImageURLs in localKey?.let {
                    dataSnapshot.child(it).child("categories").child(chosenCategory)
                        .child("skills").child(chosenSkill).child("images").children
                }!!) {
                    imageURLs.add(iterImageURLs.value.toString())
                }

                // Fetch video URLs
                videoURLs.clear()
                for (iterVideoURLs in localKey.let {
                    dataSnapshot.child(it).child("categories").child(chosenCategory)
                        .child("skills").child(chosenSkill).child("videos").children
                }) {
                    videoURLs.add(iterVideoURLs.value.toString())
                }

                // Notify that all data has been fetched
                _updateBinding.value = true
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addInstagramLink(context: Context?, link: String){
        if (link != "") {
            //add link if URL is valid
            val urlPattern = Patterns.WEB_URL
            if (URLUtil.isValidUrl(link)) {
                profileRef.child(key.toString()).child("categories").child(chosenCategory)
                    .child("skills").child(chosenSkill).child("instagramLink").setValue(link)
                skillInstagramLink = link
                //add for paired up friend
                if(skillPairUpStatus == SkillClass.pairUpStatus.PAIRED.status){
                    profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
                        .child("skills").child(chosenSkill).child("instagramLink").setValue(link)
                }
            } else {
                Toast.makeText(context, "Error: Invalid URL, Link not added", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            //remove link if empty
            profileRef.child(key.toString()).child("categories").child(chosenCategory)
                .child("skills").child(chosenSkill).child("instagramLink").setValue(link)
            skillInstagramLink = link
            //remove for paired up friend
            if(skillPairUpStatus == SkillClass.pairUpStatus.PAIRED.status){
                profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
                    .child("skills").child(chosenSkill).child("instagramLink").setValue(link)
            }
        }
    }


    //Add picture or video to database
    fun addMedia(mediaType: String, media: Any, context: Context?): Boolean {
        var mediaURL: String = ""
        //create mediaURL
        if (mediaType == "image") {
            mediaURL =
                "images/" + key.toString() + "/" + chosenCategory + "/" + chosenSkill + "/" + System.currentTimeMillis()
                    .toString()
        } else {
            mediaURL =
                "videos/" + key.toString() + "/" + chosenCategory + "/" + chosenSkill + "/" + System.currentTimeMillis()
                    .toString()
        }

        //convert media to Uri
        val mediaUri = media as? Uri
        if (mediaUri == null) {
            Toast.makeText(context, "Error: Media wrong format or null", Toast.LENGTH_SHORT).show()
            return false
        } else {
            val skillRef = profileRef.child(key.toString()).child("categories").child(chosenCategory)
                .child("skills").child(chosenSkill)

            //add media to firebase database and storage
            if (mediaType == "image") {
                //add imageURL to database and database of paired up partner
                if(skillPairUpStatus == SkillClass.pairUpStatus.PAIRED.status){
                    profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
                        .child("skills").child(chosenSkill).child("images").push().setValue(mediaURL)
                }
                skillRef.child("images").push().setValue(mediaURL)
                imageURLs.add(mediaURL)
                //upload image to firebase storage
                val storageRef = FirebaseStorage.getInstance().reference
                val uploadTask = storageRef.child(mediaURL).putFile(mediaUri)
                uploadTask.addOnFailureListener {
                    Toast.makeText(context, "Error: Failed to upload image", Toast.LENGTH_SHORT)
                        .show()
                }.addOnSuccessListener { taskSnapshot ->
                    assembleMediaURLs()
                    _updateBinding.value = true
                    _mediaUploadSuccess.value = true
                }
                return true
            } else {

                //add videoURL to database and database of paired up partner
                if(skillPairUpStatus == SkillClass.pairUpStatus.PAIRED.status){
                    profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
                        .child("skills").child(chosenSkill).child("videos").push().setValue(mediaURL)
                }
                skillRef.child("videos").push().setValue(mediaURL)
                videoURLs.add(mediaURL)
                //upload video to firebase storage
                val storageRef = FirebaseStorage.getInstance().reference
                val uploadTask = storageRef.child(mediaURL).putFile(mediaUri)
                uploadTask.addOnFailureListener {
                    Toast.makeText(context, "Error: Failed to upload video", Toast.LENGTH_SHORT)
                        .show()
                }.addOnSuccessListener { taskSnapshot ->
                    assembleMediaURLs()
                    _updateBinding.value = true
                    _mediaUploadSuccess.value = true
                }
                return true
            }
        }
    }


    //assemble list of media URLs sorted by date added for itemAdapter (recyclerView)
    fun assembleMediaURLs(){
        mediaURLs.clear()
        for(items in imageURLs){
            mediaURLs.add(MediaItem(items, false))
        }
        for(items in videoURLs){
            mediaURLs.add(MediaItem(items, true))
        }
        //sort media by date added to Firebase instead of type
        mediaURLs = ArrayList(mediaURLs.sortedBy {it.url.takeLast(10)})
    }

    //delete media from skill and from firebase database/storage
    fun deleteMedia(position: Int){
        val mediaItem = mediaURLs[position]

        if(mediaItem.isVideo == false){
            //delete image from database and database of paired up partner
            if(skillPairUpStatus == SkillClass.pairUpStatus.PAIRED.status){
                profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
                    .child("skills").child(chosenSkill).child("images").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (iterImageURLs in dataSnapshot.children) {
                                if(iterImageURLs.value.toString() == mediaItem.url){
                                    iterImageURLs.ref.removeValue()
                                    break
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            profileRef.child(key.toString()).child("categories").child(chosenCategory)
                .child("skills").child(chosenSkill).child("images").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (iterImageURLs in dataSnapshot.children) {
                            if(iterImageURLs.value.toString() == mediaItem.url){
                                iterImageURLs.ref.removeValue()
                                break
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

        } else {
            //delete video from database and database of paired up partner
            if(skillPairUpStatus == SkillClass.pairUpStatus.PAIRED.status){
                profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
                    .child("skills").child(chosenSkill).child("videos").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (iterVideoURLs in dataSnapshot.children) {
                                if(iterVideoURLs.value.toString() == mediaItem.url){
                                    iterVideoURLs.ref.removeValue()
                                    break
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            profileRef.child(key.toString()).child("categories").child(chosenCategory)
                .child("skills").child(chosenSkill).child("videos").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (iterVideoURLs in dataSnapshot.children) {
                            if(iterVideoURLs.value.toString() == mediaItem.url){
                                iterVideoURLs.ref.removeValue()
                                break
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
        //delete media from firebase storage
        val storageRef = FirebaseStorage.getInstance().reference
        val desertRef = storageRef.child(mediaItem.url)
        desertRef.delete().addOnSuccessListener {
            imageURLs.remove(mediaItem.url)
            videoURLs.remove(mediaItem.url)
            assembleMediaURLs()
            _updateBinding.value = true
        }
    }


    //Add Pair Up Partner to database (Send a partner request)
    fun addPairUpPartner(context: Context?, pairUpPartner: String){
        //search for username in database
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var found = false
                for (iterUser in dataSnapshot.children) {
                    if (iterUser.key.toString() == pairUpPartner) {
                        found = true
                        break
                    }
                }
                if (found) {
                    //add pairUp information to cloud
                    val skillRef = profileRef.child(key.toString()).child("categories").child(chosenCategory)
                        .child("skills").child(chosenSkill)
                    //update logged in user
                    skillRef.child("pairUpStatus").setValue(SkillClass.pairUpStatus.PENDING_SENT.status)
                    skillRef.child("pairUpName").setValue(pairUpPartner)

                    //add notification to partner
                    profileRef.child(pairUpPartner).child("notifications").push().setValue(key+" "+chosenCategory+" "+chosenSkill)
                    profileRef.child(pairUpPartner).child("notifications").child("numberOfNotifications")
                        .setValue(ServerValue.increment(1))

                    //update pairUp partner
                    val skillPartnerRef = profileRef.child(pairUpPartner).child("categories").child(chosenCategory)
                        .child("skills").child(chosenSkill)

                    profileRef.child(pairUpPartner).child("categories").child(chosenCategory)
                        .child("numberOfSkills").setValue(ServerValue.increment(1))
                    skillPartnerRef.child("pairUpStatus").setValue(SkillClass.pairUpStatus.PENDING_RECEIVED.status)
                    skillPartnerRef.child("pairUpName").setValue(key.toString())
                    skillPartnerRef.child("priority").setValue(SkillClass.SkillPriority.FRIENDREQUEST.level)
                    skillPartnerRef.child("progression").setValue(skillProgression)
                    skillPartnerRef.child("instagramLink").setValue(skillInstagramLink)

                    //add local variables
                    skillPairUpName = pairUpPartner
                    skillPairUpStatus = SkillClass.pairUpStatus.PENDING_SENT.status
                    _pairUpRequestSent.value = true

                } else {
                    Toast.makeText(context, "Error: User not found", Toast.LENGTH_SHORT).show()
                }

            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    //Accept Pair Up Partner to database (Accept a partner request)
    fun acceptPairUpRequest(context: Context ?){
        //get all skill data from paired up user
        pairUpAccepted = true
        getSkillData(context, false, skillPairUpName)
        skillPairUpStatus = SkillClass.pairUpStatus.PAIRED.status
        profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpStatus").setValue(SkillClass.pairUpStatus.PAIRED.status)
        profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpStatus").setValue(SkillClass.pairUpStatus.PAIRED.status)

        removeNotification()
    }

    //decline PairUp request and reset variables accordingly
    fun declinePairUpRequest(context: Context ?) {
        val skillRef = profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill)

        removeNotification()

        profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpStatus").setValue(SkillClass.pairUpStatus.NOT_PAIRED.status)
        profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpName").setValue("")
        profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpStatus").setValue(SkillClass.pairUpStatus.NOT_PAIRED.status)
        profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpName").setValue("")
        skillPairUpStatus = SkillClass.pairUpStatus.NOT_PAIRED.status
        skillPairUpName = ""
    }

    private fun removeNotification(){
        //search for this pairUp notification in database and removes it
        val query = profileRef.child(key.toString()).child("notifications").orderByValue()
            .equalTo(skillPairUpName+" "+chosenCategory+" "+chosenSkill)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Node with the desired value exists
                    for (snapshot in dataSnapshot.children) {
                        // snapshot contains the node with the desired value
                        snapshot.ref.removeValue() // remove the node
                        //decrement number of notifications once notification is removed
                        profileRef.child(key.toString()).child("notifications").child("numberOfNotifications")
                            .runTransaction(object : Transaction.Handler {
                                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                    val currentValue = mutableData.getValue(Int::class.java)
                                    if (currentValue == null) {
                                        mutableData.value = 0
                                    } else {
                                        mutableData.value = currentValue - 1
                                    }
                                    return Transaction.success(mutableData)
                                }

                                override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                                    // Transaction completed
                                    Log.d(TAG, "Transaction:onComplete:$databaseError")
                                }
                            })
                    }
                } else {
                    // Node with the desired value does not exist
                    Log.e(TAG, "Notification to remove does not exist")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
                Log.e(TAG, "Error when looking for notification to remove", databaseError.toException())
            }
        })
    }

    //once pairUp request is accepted and all skill data from partner is downloaded:
    //Push all data to logged in user's cloud.
    fun pushPairedSkillToLoggedInCloud(context: Context?){
        val skillRef = profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill)

        skillRef.child("pairUpStatus").setValue(SkillClass.pairUpStatus.PAIRED.status)
        skillRef.child("pairUpName").setValue(skillPairUpName)
        skillRef.child("priority").setValue(skillPriority)
        skillRef.child("progression").setValue(skillProgression)
        skillRef.child("instagramLink").setValue(skillInstagramLink)

        for(items in imageURLs){
            skillRef.child("images").push().setValue(items)
        }
        for(items in videoURLs){
            skillRef.child("videos").push().setValue(items)
        }

    }

    //unPair skill and reset variables PairUp variables accordingly
    fun unpairSkill(context: Context ?){
        //unpair loggedIn user
        profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpStatus").setValue(SkillClass.pairUpStatus.NOT_PAIRED.status)
        profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpName").setValue("")

        //unpair partner
        profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpStatus").setValue(SkillClass.pairUpStatus.NOT_PAIRED.status)
        profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("pairUpName").setValue("")

        //reset variables
        skillPairUpStatus = SkillClass.pairUpStatus.NOT_PAIRED.status
        skillPairUpName = ""

        _updateBinding.value = true
    }

    //update changes in Progression and priority to database
    fun updateProgressionPriority(context: Context?){
        profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("progression").setValue(skillProgression)
        profileRef.child(key.toString()).child("categories").child(chosenCategory)
            .child("skills").child(chosenSkill).child("priority").setValue(skillPriority)
        if(skillPairUpStatus == SkillClass.pairUpStatus.PAIRED.status){
            profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
                .child("skills").child(chosenSkill).child("progression").setValue(skillProgression)
            profileRef.child(skillPairUpName).child("categories").child(chosenCategory)
                .child("skills").child(chosenSkill).child("priority").setValue(skillPriority)
        }
    }

    //reset data in viewmodel
    fun resetData(){
        key = ""
        chosenSkill = ""
        chosenCategory = ""
        imageURLs.clear()
        videoURLs.clear()
        skillProgression = 0
        skillPriority = 0
        skillPairUpStatus = 0
        skillPairUpName = ""
    }

    // reset update variables after completed update

    fun resetPairUpRequest(){
        _pairUpRequestSent.value = false
    }

    fun setUpdateBinding(){
        _updateBinding.value = true
    }
    fun resetUpdateBinding(){
        _updateBinding.value = false
    }

    fun resetMediaUploadSuccess(){
        _mediaUploadSuccess.value = false
    }
}