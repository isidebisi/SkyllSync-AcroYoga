package com.example.skyllsync

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

//What could be improved:
//-instead of several arrayLists create one class with categories and numberOfSkills
// --> then create one single arrayList of this class
// --> This would make it easier to sort the categories by name or numberOfSkills
class CategoriesViewModel : ViewModel() {


    data class Category(
        var name: String,
        var numberOfSkills: Int = 0
    )

    private val _updateCategoriesAdapter = MutableLiveData<Boolean>()
    val updateCategoriesAdapter: LiveData<Boolean>
        get() = _updateCategoriesAdapter

    var key: String? = ""
    var categories: ArrayList<Category> = ArrayList()
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")
    var categoryToDelete: Int? = null


    var itemAdapter: CategoryItemAdapter? = null

    //get all data on the categories from the online database
    fun getCategories() {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                categories.clear()
                for (iterCategories in key?.let { dataSnapshot.child(it).child("categories").children }!!) {
                    val category = Category(
                        iterCategories.key.toString(),
                        iterCategories.child("numberOfSkills").value.toString().toInt())
                    categories.add(category)
                }
                //sort categories by name
                categories.sortBy { it.name }
                //update the ItemAdapter with the new Category list
                _updateCategoriesAdapter.value = true
        }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    //check if category already exists
    fun checkCategory(category: String, context: Context?): Boolean {
        return categories.any { it.name == category }
    }

    //add new category and upload it to the cloud
    fun addCategory(category: String, context: Context?): Boolean {
        //capitalize category name
        val category = category.replaceFirstChar {it.titlecase()}

        //check if category already exists
        if (checkCategory(category, context) != true ) {
            //add category to cloud and local
            categories.add(Category(category, 0))

            profileRef.child(key.toString()).child("categories").child(category).child("numberOfSkills").setValue(0)

            //sort categories by name
            categories.sortBy { it.name }


            _updateCategoriesAdapter.value = true
            return true
        } else {
            return false
        }
    }

    // recursively deletes all Folder contents of StorageReference
    // because Firebase Storage does not have a delete folder function
    // which sucks very hard. This dump took like 3 hours to figure out right.
    fun deleteStorageFolderContents(folderRef: StorageReference){
        folderRef.listAll().addOnSuccessListener { listResult ->
            // Delete each file in the folder
            listResult.items.forEach { item ->
                item.delete()
            }

            // Recursively delete files in each subfolder
            listResult.prefixes.forEach { subfolder ->
                deleteStorageFolderContents(subfolder)
            }
        }
    }


    //delete category locally and on cloud
    fun deleteCategory(context: Context?) {
        if(categoryToDelete != null) {
            val position = categoryToDelete!!
            //delete category from cloud and local
            profileRef.child(key.toString()).child("categories").child(categories[position].name)
                .removeValue()

            //delete all images and videos from cloud linked to this category
            val storageRef = FirebaseStorage.getInstance().reference
            val folderRefImages =
                storageRef.child("images").child(key.toString()).child(categories[position].name)
            val folderRefVideos =
                storageRef.child("videos").child(key.toString()).child(categories[position].name)
            //Delete associated images and videos from Firebase Storage
            deleteStorageFolderContents(folderRefImages)
            deleteStorageFolderContents(folderRefVideos)

            //delete category from local ArrayLists
            categories.removeAt(position)

            //update the ItemAdapter with the new Category list
            _updateCategoriesAdapter.value = true
            categoryToDelete = null
        } else {
            Toast.makeText(context, "Error deleting category", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetData() {
        key = ""
        categories.clear()
    }

    fun resetUpdate() {_updateCategoriesAdapter.value = false}
}