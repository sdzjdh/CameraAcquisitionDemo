package com.menghai.camerademo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.menghai.camerademo.mediacodec.MediaCodecActivity

/**
 * 这是
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
         findViewById<Button>(R.id.mediaCodecB).setOnClickListener {
             startActivity(Intent(this, MediaCodecActivity::class.java))
         }
    }
}