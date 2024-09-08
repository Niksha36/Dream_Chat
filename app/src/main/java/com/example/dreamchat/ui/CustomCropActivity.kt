package com.example.dreamchat.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dreamchat.databinding.ActivityCustomCropBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomCropActivity : AppCompatActivity() {
    lateinit var binding: ActivityCustomCropBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCustomCropBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //making crop area square
        binding.cropImageView.setAspectRatio(1, 1)
        binding.cropImageView.setFixedAspectRatio(true)

        val uri = intent.getParcelableExtra<Uri>("CROP_IMAGE_URI")
        binding.cropImageView.setImageUriAsync(uri)

        binding.btnCrop.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            val cropped = binding.cropImageView.croppedImage
            cropped?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val file = saveBitmapToFile(it)
                    withContext(Dispatchers.Main) {
                        val resultIntent = Intent().apply {
                            putExtra("CROPPED_IMAGE_URI", Uri.fromFile(file))
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                }

            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "cropped_image_$timeStamp.png"
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 30, out)
        }
        return file
    }
}