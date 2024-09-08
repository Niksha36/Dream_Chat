package com.example.dreamchat.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.dreamchat.ui.CustomCropActivity

class ImagePickerUtil(
    private val fragment: Fragment,
    private val onImagePicked: (Uri) -> Unit,
    private val onError: (Exception) -> Unit
) {
    //ожидает результата интента выбора пользователя изображения и передаёт uri этого изображения
    //в ф-ю которая запускает crop activity и передаёт uri в putExtra
    private val pickImage: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                startCrop(it)
            }
        }

    //Контракт на получение фрагментом uri изображения и то что с ним надо сделать
    private val cropImage: ActivityResultLauncher<Intent> =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val croppedImageUri = result.data?.getParcelableExtra<Uri>("CROPPED_IMAGE_URI")
                croppedImageUri?.let { onImagePicked(it) }
            } else {
                val exception = result.data?.getSerializableExtra("CROP_ERROR") as? Exception
                exception?.let { onError(it) }
            }
        }

    //запускает все остальные ф-ии, открывает галлерею
    fun openGallery() {
        pickImage.launch("image/*")
    }

    //Запускает cropActivity и передаёт uri из интента открытии галлереи в putExtra для обрезки этого изображения
    private fun startCrop(uri: Uri) {
        val intent = Intent(fragment.requireContext(), CustomCropActivity::class.java)
        intent.putExtra("CROP_IMAGE_URI", uri)
        cropImage.launch(intent)
    }
}