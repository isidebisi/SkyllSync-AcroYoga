package com.example.skyllsync

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TrainingStatsViewModel : ViewModel(), DataClient.OnDataChangedListener {
    //Key is the username of the user
    var key: String? = ""
    //Variable to store the trainings of the user
    var trainings: ArrayList<Trainings> = ArrayList()
    var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")
    var trainingToDelete: Int? = null

    private val _updateTrainingsAdapter = MutableLiveData<Boolean>()
    val updateTrainingsAdapter: LiveData<Boolean>
        get() = _updateTrainingsAdapter

    private var _deletionCompleted = MutableLiveData<Boolean>()
    val deletionCompleted: LiveData<Boolean>
        get() = _deletionCompleted

    //Variables for the HR plot
    private val _heartRate = MutableLiveData<Int>()
    val heartRate: LiveData<Int>
        get() = _heartRate

    private lateinit var xArray: IntArray
    private lateinit var yArray: IntArray
    private var _xyList: ArrayList<Number> = ArrayList()
    private val _xyListMutable = MutableLiveData<ArrayList<Number>>()
    val xyList: LiveData<ArrayList<Number>>
        get() = _xyListMutable

    val MAX_HR = 200
    val MIN_HR = 40
    val NUMBER_OF_POINTS = 100

    init {
        _heartRate.value = 0
        initializeSeriesAndAddToList()
    }

    //Initializes HR plot with array of MIN_HR values
    private fun initializeSeriesAndAddToList() {
        xArray = IntArray(NUMBER_OF_POINTS) { i -> i }
        yArray = IntArray(NUMBER_OF_POINTS) { i -> MIN_HR}
        for (i in yArray.indices) {
            _xyList.add(xArray[i])
            _xyList.add(yArray[i])
        }
        _xyListMutable.value = _xyList
    }

    //Updates HR array with new data
    private fun updateSeries(data: Int) {
        _xyList.clear()
        val lastIndex = yArray.size-1
        for (i in 0 until lastIndex) {
            yArray[i] = yArray[i + 1]
            _xyList.add(xArray[i])
            _xyList.add(yArray[i])
        }
        yArray[lastIndex] = data
        _xyList.add(xArray[lastIndex])
        _xyList.add(yArray[lastIndex])

        _xyListMutable.value = _xyList
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter {it.dataItem.uri.path == "/heart_rate" }
            .forEach { event ->
                val heartRateReceived: Int =
                    DataMapItem.fromDataItem(event.dataItem).dataMap.getInt(
                        "HEART_RATE"
                    )
                _heartRate.value = heartRateReceived

                // Update HR plot series
                updateSeries(heartRateReceived)
            }
    }

    //get all the trainings inside the category from the database
    fun getTrainings(context: Context?) {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                trainings.clear()
                for (trainingSnapshot in dataSnapshot.child(key.toString()).child("trainings").children) {
                    var training: Trainings? = null
                    if (trainingSnapshot.key != null) {
                        training = Trainings(
                            date = trainingSnapshot.key!!,
                            duration = trainingSnapshot.value as Long,
                        )
                    }
                    if (training != null){
                        trainings.add(training)
                    }
                }
                _updateTrainingsAdapter.value = true
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun deleteTraining(context: Context?) {
        val trainingsRef = profileRef.child(key.toString()).child("trainings")

        if (trainingToDelete != null) {
            //delete skill from cloud and local
            val position = trainingToDelete!!
            trainingsRef.child(trainings[position].date.toString()).removeValue()

            _deletionCompleted.value = true
            trainingToDelete = null
        } else {
            Toast.makeText(context, "Error deleting training", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetUpdate() {_updateTrainingsAdapter.value = false}
    fun resetDelete() {_deletionCompleted.value = false}

}