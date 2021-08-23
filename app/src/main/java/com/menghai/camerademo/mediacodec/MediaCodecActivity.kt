package com.menghai.camerademo.mediacodec

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.menghai.camerademo.R

/**
 * @author      zhenhai.zhao
 * @date        2021/8/20 11:18 AM
 * @package     com.menghai.camerademo.mediacodec
 * @email       zhenhai.zhao@asiainnovations.com
 * @description MediaCodec视频拍摄类
 **/
class MediaCodecActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec)
       // MediaCodec 编码接口
        val mediaCodec = MediaCodec.createEncoderByType("video")

        val videoFormat = MediaFormat.createVideoFormat("video/avc", 640, 480)
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 600) //比特率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30) //帧率
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10) //指定关键帧时间间隔

        mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

    }
}