package com.example.skyllsync

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.skyllsync.databinding.ActivityMainBinding
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var userDataSharedViewModel: UserDataSharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout

        //initialize navigation controller and menus
        val navController = this.findNavController(R.id.mainFragment)
        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
        NavigationUI.setupWithNavController(binding.navView, navController)

        bottomNavigationView = binding.bottomMenuView
        bottomNavigationView.setupWithNavController(navController)
        setBottomNavigationVisibility(View.GONE)

        //initialize notification service
        val service = NotificationService(this)

        //initialize shared view model
        userDataSharedViewModel = ViewModelProvider(this).get(UserDataSharedViewModel::class.java)

        // Observe the numberOfNotifications LiveData
        userDataSharedViewModel.numberOfNotifications.observe(this, Observer { numberOfNotifications ->
            // Send notification when number of notifications changes and is not null
            val message = userDataSharedViewModel.pairUpList.mapIndexed { index, request ->
                val splitRequest = request.split(" ")
                if (splitRequest.size >= 3) {
                    "Request ${index + 1} from User ${splitRequest[0]} : Skill ${splitRequest[2]} from ${splitRequest[1]}"
                } else {
                    "Request ${index + 1} : $request"
                }
            }.joinToString(separator = "\n")
            if (numberOfNotifications != null && numberOfNotifications > 0 && message.isNotEmpty()) {
                service.showNotification(
                    "You have $numberOfNotifications pair up requests",
                    message
                )
            }
        })

        supportActionBar?.title = getString(R.string.app_name)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.mainFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    public fun setBottomNavigationVisibility (visibility: Int){
        bottomNavigationView.visibility = visibility
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/training") {
            val duration = bytesToLong(messageEvent.data)
            Log.v("Training time", "duration : $duration")
            userDataSharedViewModel.addTraining(duration)
        }
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    private fun bytesToLong(bytes: ByteArray): Long {
        val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
        buffer.put(bytes)
        buffer.flip() //need flip
        return buffer.long
    }


}
