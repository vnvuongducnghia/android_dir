package com.example.androiddir

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initPermission()
        mPermissionHelper.requestPermission(PermissionHelper.PermissionType.GALLERY)

        findViewById<Button>(R.id.btnRun).setOnClickListener {
            if (mPermissionHelper.isAllowPermissions) {
                Log.d(TAG, "onCreate getEx DIRECTORY_DCIM: ${getExternalFilesDir(Environment.DIRECTORY_DCIM)}")
                Log.d(TAG, "onCreate getEx DIRECTORY_PICTURES: ${getExternalFilesDir(Environment.DIRECTORY_PICTURES)}")
                Log.d(TAG, "onCreate getEx DIRECTORY_MUSIC: ${getExternalFilesDir(Environment.DIRECTORY_MUSIC)}")
                Log.d(TAG, "onCreate getEx DIRECTORY_DOWNLOADS: ${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}")
                Log.d(TAG, "onCreate getExternalStorageDirectory: ${Environment.getExternalStorageDirectory().path}")


                // check a file
                val file = File(Environment.DIRECTORY_PICTURES + File.separator + "2021-04-20_081502.png")
                if (file.exists())
                    Log.d(TAG, "onCreate: ${file.path} exists")
                else
                    Log.d(TAG, "onCreate: ${file.path} not exists")

            }
        }
    }

    private fun time2Name(): String {
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.JAPAN)
        return formatter.format(Date()).toString()
    }

    private fun saveImage(bitmap: Bitmap, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/" + "YOUR_FOLDER")
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            val fos = resolver.openOutputStream(imageUri!!)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos!!.flush()
            fos.close()
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + "YOUR_FOLDER"
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, "$name.png")
            val fos = FileOutputStream(image)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPermissionHelper.checkResult(requestCode, permissions, grantResults)
    }

    lateinit var mPermissionHelper: PermissionHelper
    private fun initPermission() {
        mPermissionHelper = PermissionHelper(this)
        mPermissionHelper.setOnPermissionListener(object : PermissionHelper.PermissionListener {
            override fun onGranted(currentType: PermissionHelper.PermissionType?) {
                mPermissionHelper.isAllowPermissions = true
            }

            override fun onDenied() {
                mPermissionHelper.isAllowPermissions = false
            }

            override fun onCustomDialog() {

            }
        })
    }

}