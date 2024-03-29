package com.tf.practice

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class AppApplication: Application() {
  override fun onCreate() {
    super.onCreate()
    // Kakao SDK 초기화
    KakaoSdk.init(this, BuildConfig.KAKAO_API_KEY)
  }
}