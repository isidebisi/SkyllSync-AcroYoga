package com.example.skyllsync

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.Timer
import kotlin.concurrent.timerTask


class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener, SensorEventListener,
    MessageClient.OnMessageReceivedListener{

    //heart rate sensing
    var heartRate:Int = 40
    private var timer = Timer()

    private lateinit var mSensorManager: SensorManager
    private lateinit var mHeartRateSensor: Sensor

    private lateinit var databaseRoom: HRDao

    //navigation
    private var navControllerState = mutableStateOf<NavController?>(null)
    private val navController: NavController?
        get() = navControllerState.value

    //preview
    private lateinit var preview: Bitmap

    //wack lock to keep the screen on
    private var wakeLock: PowerManager.WakeLock? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //check for permissions to use the heart rate sensor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                "android" + "" + ".permission.BODY_SENSORS"
            ) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissions(arrayOf("android.permission.BODY_SENSORS"), 0)
        }

        //initialize heart rate sensing variables
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)!!

        //get database instance
        val application = requireNotNull(this).application
        val dataSource = HRDatabase.getInstance(application).heartRateDao
        databaseRoom = dataSource

        //create notification service
        val service = NotificationService(this)

        //wake lock to keep the screen on
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "MyApp::MyWakelockTag")
        wakeLock?.acquire()

        setContent {
            val viewModel = viewModel<StopWatchViewModel>()
            val localNavController = rememberSwipeDismissableNavController()
            navControllerState.value = localNavController
            val timerState by viewModel.timerState.collectAsStateWithLifecycle()
            val stopWatchText by viewModel.stopWatchText.collectAsStateWithLifecycle()

            // Observe the showDrinkNotification LiveData and show the notification when it's true
            viewModel.showDrinkNotification.observe(this, Observer { shouldShowNotification ->
                // Check if shouldShowNotification is true
                if (shouldShowNotification) {
                    // Call the function to show the notification
                    service.showDrinkNotification()
                    // Reset the value of showDrinkNotification to false
                    viewModel.showDrinkNotification.value = false

                    vibrateWatch()
                }
            })

            // Observe the trainingDuration LiveData and send the training time to the mobile
            viewModel.trainingDuration.observe(this, Observer { duration ->
                // Check if startTime is not 0
                if (duration != 0L) {
                    // Send the start time to the mobile
                    sendTrainingTimeToMobile(duration)
                }
            })

            //Compose UI
            Scaffold(
                timeText = {
                    TimeText(
                        timeTextStyle = TimeTextDefaults.timeTextStyle(
                            fontSize = 15.sp
                        ),
                    )
                }
            ) {
                SwipeDismissableNavHost(
                    navController = localNavController,
                    startDestination = "StopWatch"
                ) {
                    composable("StopWatch") {
                        StopWatch(
                            state = timerState,
                            text = stopWatchText,
                            onToggleRunning = viewModel::toggleIsRunning,
                            onReset = viewModel::resetTimer,
                            vibrateWatch = ::vibrateWatch,
                            showNotification = { service.showStretchNotification() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    composable("PictureController") {
                        PictureController(
                            onTakePicture = {sendCommandToMobile("takePhoto")},
                            onSeePreview = { sendCommandToMobile("sendPreview") }
                        )
                    }
                    composable("VideoController") {
                        VideoController(
                            onTakeVideo = { sendCommandToMobile("takeVideo") },
                            onSeePreview = { sendCommandToMobile("sendPreview") }
                        )
                    }
                    composable("DisplayPreview") {
                        DisplayPreview(
                            navController = navController,
                            image = preview
                        )
                    }
                }
            }
        }
    }

    //when the preview image is received from the mobile, add it to DisplayPreview and go there
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.filter {it.dataItem.uri.path == "/preview" }.forEach { event ->
            val receivedImage: ByteArray? = DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray("previewImage")

            val receivedPreviewBitmap = BitmapFactory.decodeByteArray(receivedImage, 0,
                receivedImage?.size ?: 0
            )

            preview = receivedPreviewBitmap
            navController?.navigate("DisplayPreview")
        }
    }

    //when the heart rate sensor changes, load it to MainActivity
    override fun onSensorChanged(event: SensorEvent?) {
        val heartRateReceived = event!!.values[0].toInt()
        heartRate = heartRateReceived
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        //go to correct screen depending on the fragmentName received from the mobile
        if (messageEvent.path == "/fragmentName") {
            val currentFragment = String(messageEvent.data)
            if(currentFragment == "TakePictureFragment"){
                navController?.navigate("PictureController")
            }
            else if(currentFragment == "TakeVideoFragment"){
                navController?.navigate("VideoController")
            }else{
                navController?.navigate("StopWatch")
            }
        //when the mobile sends the command to start or stop the heart rate sensing, start or stop it
        }else if (messageEvent.path == "/command") {
            val receivedCommand: String = String(messageEvent.data)
            if (receivedCommand == "Start") {
                timer = Timer()
                timer.schedule(timerTask {
                    insertHRToRoom(heartRate)
                }, 0, 500)
            } else if (receivedCommand == "Stop") {
                timer.cancel()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        Wearable.getMessageClient(this).addListener(this)
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
        Wearable.getMessageClient(this).removeListener(this)
        mSensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
        wakeLock = null
    }

    //send the command to the mobile to take a preview, a photo or a video
    private fun sendCommandToMobile(command: String) {
        Thread(Runnable {
            val connectedNodes: List<String> = Tasks
                .await(
                    Wearable
                        .getNodeClient(this).connectedNodes)
                .map { it.id }
            connectedNodes.forEach {
                val messageClient: MessageClient = Wearable
                    .getMessageClient(this)
                messageClient.sendMessage(it, "/command", command.toByteArray())
            }
        }).start()
    }

    //send the training duration to the mobile
    private fun sendTrainingTimeToMobile(trainingTime: Long) {
        Thread(Runnable {
            val connectedNodes: List<String> = Tasks
                .await(
                    Wearable
                        .getNodeClient(this).connectedNodes)
                .map { it.id }
            connectedNodes.forEach {
                val messageClient: MessageClient = Wearable
                    .getMessageClient(this)
                messageClient.sendMessage(it, "/training", longToBytes(trainingTime))
            }
        }).start()
    }

    //send the heart rate array to the mobile
    private fun sendDataToMobile(heartRate: java.util.ArrayList<Int>) {
        val dataClient: DataClient = Wearable.getDataClient(this)
        val putDataReq: PutDataRequest = PutDataMapRequest.create("/heart_rate").run {
            dataMap.putInt("HEART_RATE", heartRate.average().toInt())
            asPutDataRequest()
        }
        dataClient.putDataItem(putDataReq)
    }

    private fun vibrateWatch(){
        val vibrator = getSystemService(Vibrator::class.java)
        val vibrationPattern = longArrayOf(0, 500, 50, 300)
        //-1 - don't repeat
        val indexInPatternToRepeat = -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, indexInPatternToRepeat)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat)
        }
    }

    //insert the heart rate to the database and send it to the mobile
    private fun insertHRToRoom(heartRate: Int) {
        var asyncHRToSend : List<HREntity>? = null

        GlobalScope.launch {
            val valHR = HREntity (HRValue = heartRate)
            databaseRoom.insert(valHR)
            //when 5 values are stored in the database, send them to the mobile
            if (databaseRoom.size() >= 5) {
                asyncHRToSend = databaseRoom.getAllHRValues()
                Log.i("MainActivity", "database size $asyncHRToSend")
                val HRListToSend = ArrayList(asyncHRToSend!!.map { hrEntity -> hrEntity.HRValue })
                sendDataToMobile(HRListToSend)
                databaseRoom.clear()
            }
        }
    }

    //convert a long to a byte array to send it to the mobile
    private fun longToBytes(value: Long): ByteArray {
        val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
        buffer.putLong(value)
        return buffer.array()
    }
}

//UI Composables
@Composable
private fun StopWatch(
    state: TimerState,
    text: String,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    vibrateWatch: () -> Unit,
    showNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if(state == TimerState.RUNNING) {
                text
            } else if (state == TimerState.PAUSED) {
                "Click play to resume training"
            } else {
                "Click play to start training"
            },
            fontSize =  20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ){
            Button(onClick = { onToggleRunning() }) {
                Icon(
                    imageVector = if(state == TimerState.RUNNING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onReset()
                            // Navigate to the stretchScreen after 1 minute
                            Handler(Looper.getMainLooper()).postDelayed({
                                vibrateWatch()
                                showNotification()
                            }, 30000)
                            },
                enabled = state != TimerState.RESET,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun PictureController(
    onTakePicture: () -> Unit,
    onSeePreview: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onSeePreview() },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = Color.White
            ),
            modifier = Modifier.height(90.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Preview,
                modifier = Modifier.size(100.dp),
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = { onTakePicture() },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = Color.White
            ),
            modifier = Modifier.height(80.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                modifier = Modifier.size(90.dp),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun VideoController(
    onTakeVideo: () -> Unit,
    onSeePreview: () -> Unit,
    videoOn: Boolean = false,
) {
    // Create a state for the button color
    val buttonColor = remember { mutableStateOf(Color.White) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onSeePreview() },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = Color.White
            ),
            modifier = Modifier.height(90.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Preview,
                modifier = Modifier.size(100.dp),
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = {
                onTakeVideo()
                // Toggle the button color when clicked
                if (buttonColor.value == Color.White) {
                    buttonColor.value = Color.Red
                } else {
                    buttonColor.value = Color.White
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = buttonColor.value
            ),
            modifier = Modifier.height(80.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                modifier = Modifier.size(90.dp),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun DisplayPreview(
    navController: NavController?,
    image: Bitmap,
){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Image(
            painter = BitmapPainter(image.asImageBitmap()),
            modifier = Modifier.clickable { navController?.popBackStack() },
            contentDescription = null,
        )
    }
}


