package com.tf.practice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.tf.practice.base.BaseActivity
import com.tf.practice.databinding.ActivityInspectBinding
import com.tf.practice.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class InspectActivity: BaseActivity<ActivityInspectBinding>(ActivityInspectBinding::inflate) {

    companion object {
        const val ARG_URI = "ARG_URI"
    }

    private val imageSize = 150
    private var uri: Uri? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            binding.resultImageView.isVisible = false
            takeImage()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->

        if (isSuccess) {
            latestTmpUri?.let { uri ->
                binding.inspectImageView.setImageURI(uri)

                binding.bottomBtn.apply {
                    text = resources.getString(R.string.inspect)

                    setOnClickListener {
                        val bitmap = Bitmap.createScaledBitmap(
                            binding.inspectImageView.drawable.toBitmap(),
                            imageSize,
                            imageSize,
                            false
                        )
                        classifyImage(bitmap)
                    }
                }
            }
        }
    }

    private var latestTmpUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uri = intent.getParcelableExtra(ARG_URI)

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.closeBtn.setOnClickListener {
            finish()
        }

        binding.inspectImageView.setImageURI(uri)

        binding.bottomBtn.setOnClickListener {
            val bitmap = Bitmap.createScaledBitmap(binding.inspectImageView.drawable.toBitmap(), imageSize, imageSize, false)
            classifyImage(bitmap)
        }

    }

    private fun classifyImage(image: Bitmap?) {
        try {
            val model: Model = Model.newInstance(baseContext)

            // Creates inputs for reference.
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 150, 150, 3), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())
            val intValues = IntArray(imageSize * imageSize)
            image!!.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
            var pixel = 0
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val `val` = intValues[pixel++] // RGB
                    byteBuffer.putFloat((`val` shr 16 and 0xFF) * 1f)
                    byteBuffer.putFloat((`val` shr 8 and 0xFF) * 1f)
                    byteBuffer.putFloat((`val` and 0xFF) * 1f)
                }
            }
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs: Model.Outputs = model.process(inputFeature0)
            val outputFeature0: TensorBuffer = outputs.outputFeature0AsTensorBuffer
            val confidences = outputFeature0.floatArray
            // find the index of the class with the biggest confidence.

            val pass = confidences[0] == 1.0f

            binding.resultImageView.apply {
                isVisible = true

                setImageResource(
                    if (pass) {
                        R.drawable.ic_pass
                    } else {
                        R.drawable.ic_fail
                    }
                )
            }

            binding.backBtn.isVisible = false
            binding.closeBtn.isVisible = true

            binding.bottomBtn.apply {

                text = if (pass) {
                    resources.getString(R.string.next_image)
                } else {
                    resources.getString(R.string.inspect)
                }

                if (pass) {
                    setOnClickListener {
                        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            binding.resultImageView.isVisible = false
                            takeImage()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }

                isVisible = pass
            }

            binding.reportBtn.apply {
                isVisible = !pass
            }

            binding.retryBtn.apply {
                isVisible = !pass

                setOnClickListener {
                    finish()
                }
            }

            // Releases model resources if no longer used.
            model.close()
        } catch (e: IOException) {
            // handle exception
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