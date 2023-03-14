package com.tf.practice

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.tf.practice.base.BaseActivity
import com.tf.practice.databinding.ActivityInspectBinding
import com.tf.practice.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class InspectActivity: BaseActivity<ActivityInspectBinding>(ActivityInspectBinding::inflate) {

    companion object {
        const val ARG_URI = "ARG_URI"
    }

    private val imageSize = 150
    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uri = intent.getParcelableExtra(ARG_URI)

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


            binding.resultImageView.apply {
                isVisible = true

                setImageResource(
                    if (confidences[0] == 1.0f) {
                        R.drawable.ic_pass
                    } else {
                        R.drawable.ic_fail
                    }
                )
            }

            // Releases model resources if no longer used.
            model.close()
        } catch (e: IOException) {
            // handle exception
        }
    }
}