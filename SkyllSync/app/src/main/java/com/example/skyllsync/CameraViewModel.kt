package com.example.skyllsync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import java.io.ByteArrayOutputStream


class CameraViewModel : ViewModel() {

    var imageUri: Uri? = null
    fun sendDataToWear(context: Context?, dataClient: DataClient) {

        val matrix = Matrix()

        var imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri)
        val ratio: Float = 13F

        val imageBitmapScaled = Bitmap.createScaledBitmap(
            imageBitmap,
            (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(),
            false
        )
        imageBitmap = Bitmap.createBitmap(
            imageBitmapScaled,
            0,
            0,
            (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(),
            matrix,
            true
        )

        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        val request: PutDataRequest = PutDataMapRequest.create("/preview").run {
            dataMap.putByteArray("previewImage", imageByteArray)
            dataMap.putString("uri", imageUri.toString())
            asPutDataRequest()
        }

        Log.v("CameraViewModel", "sendDataToWear: $imageUri")

        //delete preview Image
        context?.contentResolver?.delete(imageUri!!, null, null)

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
    }

    fun sendFragmentNameToWear(activity: AppCompatActivity, fragmentName: String) {
        Thread(Runnable {
            val connectedNodes: List<String> = Tasks
                .await(
                    Wearable
                        .getNodeClient(activity as MainActivity).connectedNodes)
                .map { it.id }
            connectedNodes.forEach {
                val messageClient: MessageClient = Wearable
                    .getMessageClient(activity as AppCompatActivity)
                messageClient.sendMessage(it, "/fragmentName", fragmentName.toByteArray())
            }
        }).start()
    }

}


object ImageUtils {
    fun getLastImageUri(context: Context): Uri? {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_TAKEN + " DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val imagePath = it.getString(columnIndex)
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Uri.parse("file://$imagePath")
                } else {
                    Uri.parse("file://$imagePath")
                }
            }
        }

        return null
    }
}