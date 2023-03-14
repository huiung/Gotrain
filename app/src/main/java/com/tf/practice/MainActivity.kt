package com.tf.practice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.tf.practice.base.BaseActivity
import com.tf.practice.databinding.ActivityMainBinding
import java.io.File

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    companion object {
        const val ARG_URI = "ARG_URI"
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takeImage()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->

        if (isSuccess) {
            latestTmpUri?.let { uri ->
                val intent = Intent(baseContext, InspectActivity::class.java)
                intent.putExtra(ARG_URI, uri)
                startActivity(intent)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

        uri ?: return@registerForActivityResult

        val intent = Intent(baseContext, InspectActivity::class.java)
        intent.putExtra(ARG_URI, uri)
        startActivity(intent)
    }

    private var latestTmpUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.infoTextView.setOnClickListener {
            val intent = Intent(baseContext, InfoActivity::class.java)
            startActivity(intent)
        }

        binding.cameraButton.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takeImage()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        binding.galleryButton.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }
}