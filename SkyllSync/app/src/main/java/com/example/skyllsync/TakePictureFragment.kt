package com.example.skyllsync

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.navigation.fragment.findNavController
import com.example.skyllsync.databinding.FragmentCameraBinding
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Locale

class TakePictureFragment : Fragment(), MessageClient.OnMessageReceivedListener {

    companion object {

        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
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
    private lateinit var cameraController: LifecycleCameraController
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding =  FragmentCameraBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        userDataSharedViewModel = ViewModelProvider(requireActivity()).get(UserDataSharedViewModel::class.java)

        // Sends fragment name to smartwatch
        viewModel.sendFragmentNameToWear(activity as MainActivity, "TakePictureFragment")

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        binding.captureButton.setOnClickListener {
            takePhotoAnimation()
            takePhoto()
        }

        binding.switchCameraButton.setOnClickListener {
            switchCameraAnimation()
            if (cameraController.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            }else{
                cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }

        }

        binding.switchVideo.setOnClickListener {
            findNavController().navigate(R.id.action_takePictureFragment_to_takeVideoFragment)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
    }

    private fun takePhoto(mode:String = "default") {
        // Get a stable reference of the modifiable image capture use case
        //val imageCapture = imageCapture ?: return
        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.FRANCE)
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
        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity as MainActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"

                    if (mode == "preview") {
                        viewModel.imageUri = output.savedUri
                        Log.v("TakePictureFragment", "imageUri : ${viewModel.imageUri}")

                        if (viewModel.imageUri != null) {
                            // Do something with the URI, e.g., load it into an ImageView
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
                    }else{
                        userDataSharedViewModel.selectMediaUri(output.savedUri)
                        userDataSharedViewModel.setPopBackStackOnce()
                        findNavController().navigate(R.id.action_takePictureFragment_to_skillFragment)
                    }
                }
            }
        )

    }
    private fun startCamera() {
       val previewView: PreviewView = binding.viewFinder
        cameraController = LifecycleCameraController((activity as MainActivity).baseContext)
        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        cameraController = LifecycleCameraController((activity as MainActivity).baseContext)
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false) {
                    permissionGranted = false
                }
            }
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

    private fun takePhotoAnimation() {
        binding.captureButton.animate().apply {
            duration = 150
            scaleX(0.8f)
            scaleY(0.8f)
        }.withEndAction {

            binding.captureButton.animate().apply {
                duration = 150
                scaleX(1.0f)
                scaleY(1.0f)
            }.start()
        }
    }

    private fun switchCameraAnimation() {
        binding.switchCameraButton.animate().apply {
            duration = 500
            rotationY(180f)
        }.withEndAction{
            binding.switchCameraButton.animate().apply {
                duration = 0
                rotationY(0f)
            }
        }
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

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/command") {
            val command = String(messageEvent.data)
            Log.v("TakePictureFragment", "command : $command")
            if (command == "takePhoto") {
                takePhoto()
            }else if (command == "sendPreview"){
                takePhoto(mode = "preview")
            }
        }
    }


}