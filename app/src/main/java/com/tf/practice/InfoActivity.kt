package com.tf.practice

import android.os.Bundle
import com.tf.practice.base.BaseActivity
import com.tf.practice.databinding.ActivityInfoBinding

class InfoActivity: BaseActivity<ActivityInfoBinding>(ActivityInfoBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.closeImageView.setOnClickListener {
            finish()
        }
    }

}