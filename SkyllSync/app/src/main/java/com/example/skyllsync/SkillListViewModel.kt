package com.example.skyllsync

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class SkillListViewModel : ViewModel() {

    //one single skill class for the skillList
    //This allows us to sort the ArrayList to any of the attributes we want
    data class Skill(
        var name: String,
        var progression: Int = SkillClass.SkillProgression.LVL0.level,
        var priority: Int = SkillClass.SkillPriority.LVL0.level,
        var priorityColor: String = SkillClass.getPriorityColorFromNumber(0),
        var progressionImageSource: String = "@drawable/progress0",
        var pairUpStatus: Int = SkillClass.pairUpStatus.NOT_PAIRED.status,
        var pairUpName: String = "",
        var instagramLink: String = ""
    )

    enum class SortBy(val sort: Int, val description: String){
        NAME(0, "NAME"),
        PRIORITY_UP(1, "PRIORITY UP"),
        PRIORITY_DOWN(2, "PRIORITY DOWN"),
        PROGRESSION_UP(3, "PROGRESSION UP"),
        PROGRESSION_DOWN(4, "PROGRESSION DOWN"),
        PAIRUPSTATUS(5, "PAIRUPSTATUS"),
        PAIRUPNAME(6, "PAIRUPNAME")
    }
    private val _updateSkillsAdapter = MutableLiveData<Boolean>()
    val updateSkillsAdapter: LiveData<Boolean>
        get() = _updateSkillsAdapter

    private var _deletionCompleted = MutableLiveData<Boolean>()
    val deletionCompleted: LiveData<Boolean>
        get() = _deletionCompleted

    var key: String? = ""
    var skills: ArrayList<Skill> = ArrayList()
    var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")
    var skillToDelete: Int? = null
    var sortBy: SortBy = SortBy.NAME

    //change sorting of the skills
    fun changeSort(){
        when(sortBy){
            SortBy.NAME -> sortBy = SortBy.PRIORITY_UP
            SortBy.PRIORITY_UP -> sortBy = SortBy.PRIORITY_DOWN
            SortBy.PRIORITY_DOWN -> sortBy = SortBy.PROGRESSION_UP
            SortBy.PROGRESSION_UP -> sortBy = SortBy.PROGRESSION_DOWN
            SortBy.PROGRESSION_DOWN -> sortBy = SortBy.NAME
            else -> sortBy = SortBy.NAME
        }
        sort()
    }

    fun sort(){
        when(sortBy){
            SortBy.NAME -> skills.sortBy { it.name }
            SortBy.PRIORITY_UP -> skills.sortBy { it.priority }
            SortBy.PRIORITY_DOWN -> skills.sortByDescending { it.priority }
            SortBy.PROGRESSION_UP -> skills.sortBy { it.progression }
            SortBy.PROGRESSION_DOWN -> skills.sortByDescending { it.progression }
            else -> skills.sortBy { it.name }
        }
        _updateSkillsAdapter.value = true
    }




    //get all the skills inside the category from the database
    fun getSkills(context: Context?, chosenCategory: String) {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                skills.clear()
                for (skillSnapshot in dataSnapshot.child(key.toString()).child("categories")
                    .child(chosenCategory).child("skills").children) {
                    val skill = Skill(
                        name = skillSnapshot.key ?: "",
                        progression = skillSnapshot.child("progression").value?.toString()?.toIntOrNull() ?: 0,
                        priority = skillSnapshot.child("priority").value?.toString()?.toIntOrNull() ?: 0,
                        priorityColor = skillSnapshot.child("priorityColor").value?.toString() ?: "",
                        progressionImageSource = skillSnapshot.child("imageSource").value?.toString() ?: "",
                        pairUpStatus = skillSnapshot.child("pairUpStatus").value?.toString()?.toIntOrNull() ?: 0,
                        pairUpName = skillSnapshot.child("pairUpName").value?.toString() ?: "",
                        instagramLink = skillSnapshot.child("instagramLink").value?.toString() ?: ""
                    )
                    skills.add(skill)
                    }


                for(iter in skills){
                    iter.progressionImageSource = SkillClass.getProgressionPathFromNumber(iter.progression)
                    iter.priorityColor = SkillClass.getPriorityColorFromNumber(iter.priority)
                }
                sort()
                _updateSkillsAdapter.value = true
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun checkSkill(skill: String, context: Context?): Boolean {
        return skills.any { it.name == skill }
    }

    fun addSkill(skill: String, category: String, context: Context?): Boolean {
        //capitalize skill name
        val skill = skill.replaceFirstChar {it.titlecase()}

        //check if skill already exists
        if (checkSkill(skill, context) != true) {
            //add skill to cloud and local
            val skillRef = profileRef.child(key.toString()).child("categories").child(category).child("skills").child(skill)
            skills.add(Skill(skill))
            skillRef.child("progression").setValue(0)
            skillRef.child("priority").setValue(0)
            skillRef.child("pairUpStatus").setValue(0)
            skillRef.child("pairUpName").setValue("")
            skillRef.child("instagramLink").setValue("")
            profileRef.child(key.toString()).child("categories").child(category).child("numberOfSkills").setValue(skills.size)
            sort()
            _updateSkillsAdapter.value = true
            return true
        } else {
            return false
        }
    }

    fun deleteSkill(context: Context?, chosenCategory: String) {
        val skillsRef = profileRef.child(key.toString()).child("categories").child(chosenCategory).child("skills")

        if (skillToDelete != null) {
            //delete pairUp information with paired up user from cloud
            if (skills[skillToDelete!!].pairUpStatus >= SkillClass.pairUpStatus.NOT_PAIRED.status) {
                removeNotification(chosenCategory, skills[skillToDelete!!].pairUpName)

                skillsRef.child(skills[skillToDelete!!].name).child("pairUpStatus").setValue(0)
                skillsRef.child(skills[skillToDelete!!].name).child("pairUpName").setValue("")
            }

            //delete skill from cloud and local
            val position = skillToDelete!!
            skillsRef.child(skills[position].name).removeValue()

            //delete pictures from firebase Storage
            val storageRef = FirebaseStorage.getInstance().reference
            val folderRef = storageRef.child("images").child(key.toString()).child(chosenCategory).child(skills[position].name)
            folderRef.listAll().addOnSuccessListener { listResult ->
                // Delete each file in the folder
                listResult.items.forEach { item ->
                    item.delete()
                }
            }

            skills.removeAt(position)
            profileRef.child(key.toString()).child("categories").child(chosenCategory)
                .child("numberOfSkills").setValue(skills.size)

            _deletionCompleted.value = true
            skillToDelete = null
        } else {
            Toast.makeText(context, "Error deleting skill", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeNotification(
        chosenCategory: String,
        pairUpName: String
    ){
        //search for this pairUp notification in database and removes it
        val query = profileRef.child(key.toString()).child("notifications").orderByValue()
            .equalTo(pairUpName+" "+chosenCategory+" "+skills[skillToDelete!!].name)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Node with the desired value exists
                    for (snapshot in dataSnapshot.children) {
                        //decrement number of notifications once notification is removed
                        profileRef.child(key.toString()).child("notifications").child("numberOfNotifications")
                            .runTransaction(object : Transaction.Handler {
                                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                    val currentValue = mutableData.getValue(Int::class.java)
                                    if ((currentValue == null) || (currentValue < 0)) {
                                        mutableData.value = 0
                                    } else {
                                        mutableData.value = currentValue - 1
                                    }
                                    return Transaction.success(mutableData)
                                }

                                override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                                    // Transaction completed
                                    Log.d(ContentValues.TAG, "Transaction:onComplete:$databaseError")
                                }
                            })
                        // snapshot contains the node with the desired value
                        snapshot.ref.removeValue() // remove the node
                    }
                } else {
                    // Node with the desired value does not exist
                    Log.e(ContentValues.TAG, "Notification to remove does not exist")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
                Log.e(ContentValues.TAG, "Error when looking for notification to remove", databaseError.toException())
            }
        })
    }



    fun resetData() {
        key = ""
        skills.clear()
    }

    fun resetUpdate() {_updateSkillsAdapter.value = false}
    fun resetDelete() {_deletionCompleted.value = false}

}