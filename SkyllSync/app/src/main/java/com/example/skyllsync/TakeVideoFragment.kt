package com.example.skyllsync

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.skyllsync.databinding.FragmentCameraBinding
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TakeVideoFragment : Fragment(), MessageClient.OnMessageReceivedListener {

    companion object {
        fun newInstance() = TakeVideoFragment()

        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
    private lateinit var userDataSharedViewModel: UserDataSharedViewModel
    private lateinit var viewModel: CameraViewModel
    private lateinit var binding: FragmentCameraBinding

    //Variables for camera
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment, set UI elements and viewModels
        binding =  FragmentCameraBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        userDataSharedViewModel = ViewModelProvider(requireActivity()).get(UserDataSharedViewModel::class.java)

        //send fragment name to wear
        viewModel.sendFragmentNameToWear(activity as MainActivity, "TakeVideoFragment")

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        //change buttons icon
        binding.captureButton.setImageDrawable(resources.getDrawable(R.drawable.circle_recording))
        binding.switchVideo.setImageDrawable(resources.getDrawable(R.drawable.photo_camera))

        // Set up the listeners for take photo and video capture buttons
        binding.captureButton.setOnClickListener { captureVideo() }
        binding.switchVideo.setOnClickListener {
            findNavController().navigate(R.id.action_takeVideoFragment_to_takePictureFragment)
        }

        binding.switchCameraButton.setOnClickListener {

            binding.switchCameraButton.animate().apply {
                duration = 500
                rotationY(180f)
            }.withEndAction{
                binding.switchCameraButton.animate().apply {
                    duration = 0
                    rotationY(0f)
                }
            }

            //if camera is front camera, change to back camera
            // Toggle between front and back cameras
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            // Rebind the use cases with the new camera selector
            cameraExecutor.shutdown()
            startCamera()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        // TODO: Use the ViewModel
    }

    private fun takePreview() {
        // Get a stable reference of the modifiable image capture use case
        // If is case is null, exit out of the function to avoid crashing
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder((activity as MainActivity).contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity as MainActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText((activity as MainActivity).baseContext, msg, Toast.LENGTH_SHORT).show()

                    viewModel.imageUri = output.savedUri

                    if (viewModel.imageUri != null) {
                        // Send preview to smartwatch
                        val dataClient: DataClient =
                            Wearable.getDataClient(activity as AppCompatActivity)
                        viewModel.sendDataToWear(activity?.applicationContext, dataClient)
                        Toast.makeText(
                            (activity as MainActivity).baseContext,
                            getString(R.string.image_sent_to_sw), Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            (activity as MainActivity).baseContext,
                            getString(R.string.msg_no_uri), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }


    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        binding.captureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder((activity as MainActivity).contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(activity as MainActivity, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission((activity as MainActivity),
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor((activity as MainActivity))) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.captureButton.apply {
                            //change icon
                            binding.captureButton.setImageDrawable(resources.getDrawable(R.drawable.round_square_red))
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText((activity as MainActivity).baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        binding.captureButton.apply {
                            //change icon
                            setImageDrawable(resources.getDrawable(R.drawable.circle_recording))
                            isEnabled = true
                        }
                        userDataSharedViewModel.selectMediaUri(recordEvent.outputResults.outputUri)
                        userDataSharedViewModel.setPopBackStackOnce()
                        findNavController().navigate(R.id.action_takeVideoFragment_to_skillFragment)
                    }
                }
            }
    }


    private fun startCamera() {
        // Create an instance of the ProcessCameraProvider to bind lifecycle of cameras to the lifecycle owner
        // This eliminates the task of opening and closing the camera since CameraX is lifecycle aware
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity as MainActivity)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner within the application's process
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(activity as MainActivity))

    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            //loops through all permissions and checks if they are granted
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            //if one is not granted, show toast. Otherwise, start camera
            if (!permissionGranted) {
                Toast.makeText((activity as MainActivity).baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            (activity as MainActivity).baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(activity as MainActivity).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(activity as MainActivity).removeListener(this)
        viewModel.sendFragmentNameToWear(activity as MainActivity, "BackToDefault")
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        viewModel.sendFragmentNameToWear(activity as MainActivity, "BackToDefault")
    }

    // Receives command from wear to take video or send preview
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/command") {
            val command = String(messageEvent.data)

            if (command == "takeVideo") {
                captureVideo()
            }else if (command == "sendPreview"){
                takePreview()
            }
        }
    }

}