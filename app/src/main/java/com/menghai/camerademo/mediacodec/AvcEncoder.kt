package com.menghai.camerademo.mediacodec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Environment
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * @author      zhenhai.zhao
 * @date        2021/8/23 5:38 PM
 * @package     com.menghai.camerademo.mediacodec
 * @email       zhenhai.zhao@asiainnovations.com
 * @description
 **/
class AvcEncoder {
    private val TIMEOUT_USEC = 12000

    private var mediaCodec: MediaCodec? = null
    var mWidth = 0
    var mHeight = 0
    var mFramerate = 0

    var configByte: ByteArray? = null

    fun init(width: Int, height: Int, frameRate: Int, bitrate: Int){
        this.mWidth = width
        this.mHeight = height
        this.mFramerate = frameRate
        val mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //配置编码器参数
        mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        //启动编码器
        mediaCodec?.start()
        //创建保存编码后数据的文件
        createFile()
    }

    private var outputStream: BufferedOutputStream? = null
    private fun createFile() {
       var  path = Environment.getExternalStorageDirectory().absolutePath + "/test1.h264"
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        try {
            outputStream = BufferedOutputStream(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun StopEncoder() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    var isRuning = false
    fun stopThread() {
        isRuning = false
        try {
            StopEncoder()
            outputStream?.flush()
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun startEncoderThread() {
        val EncoderThread = Thread {
            isRuning = true
            var input: ByteArray? = null
            var pts: Long = 0
            var generateIndex: Long = 0
            while (isRuning) {
                //访问MainActivity用来缓冲待解码数据的队列
                if (MediaCodecActivity.YUVQueues.size > 0) {
                    //从缓冲队列中取出一帧
                    input = MediaCodecActivity.YUVQueues.poll()
                    val yuv420sp = ByteArray(mWidth * mHeight * 3 / 2)
                    //把待编码的视频帧转换为YUV420格式
                    NV21ToNV12(input, yuv420sp, mWidth, mHeight)
                    input = yuv420sp
                }
                if (input != null) {
                    try {
                        val startMs = System.currentTimeMillis()
                        //编码器输入缓冲区
                        val inputBuffers = mediaCodec!!.inputBuffers
                        //编码器输出缓冲区
                        val outputBuffers = mediaCodec!!.outputBuffers
                        val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(-1)
                        if (inputBufferIndex >= 0) {
                            pts = computePresentationTime(generateIndex)
                            val inputBuffer = inputBuffers[inputBufferIndex]
                            inputBuffer.clear()
                            //把转换后的YUV420格式的视频帧放到编码器输入缓冲区中
                            inputBuffer.put(input)
                            mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, input.size, pts, 0)
                            generateIndex += 1
                        }
                        val bufferInfo = MediaCodec.BufferInfo()
                        var outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC.toLong())
                        while (outputBufferIndex >= 0) {
                            //Log.i("AvcEncoder", "Get H264 Buffer Success! flag = "+bufferInfo.flags+",pts = "+bufferInfo.presentationTimeUs+"");
                            val outputBuffer = outputBuffers[outputBufferIndex]
                            val outData = ByteArray(bufferInfo.size)
                            outputBuffer[outData]
                            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                configByte = ByteArray(bufferInfo.size)
                                configByte = outData
                            } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                val keyframe = ByteArray(bufferInfo.size + configByte!!.size)
                                System.arraycopy(configByte, 0, keyframe, 0, configByte!!.size)
                                //把编码后的视频帧从编码器输出缓冲区中拷贝出来
                                System.arraycopy(outData, 0, keyframe, configByte!!.size, outData.size)
                                outputStream!!.write(keyframe, 0, keyframe.size)
                            } else {
                                //写到文件中
                                outputStream!!.write(outData, 0, outData.size)
                            }
                            mediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC.toLong())
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                } else {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        EncoderThread.start()
    }

    private fun NV21ToNV12(nv21: ByteArray?, nv12: ByteArray?, width: Int, height: Int) {
        if (nv21 == null || nv12 == null) return
        val framesize = width * height
        var i = 0
        var j = 0
        System.arraycopy(nv21, 0, nv12, 0, framesize)
        i = 0
        while (i < framesize) {
            nv12[i] = nv21[i]
            i++
        }
        j = 0
        while (j < framesize / 2) {
            nv12[framesize + j - 1] = nv21[j + framesize]
            j += 2
        }
        j = 0
        while (j < framesize / 2) {
            nv12[framesize + j] = nv21[j + framesize - 1]
            j += 2
        }
    }

    //生成第N帧的显示时间（以微秒为单位）
    private fun computePresentationTime(frameIndex: Long): Long {
        return 132 + frameIndex * 1000000 / mFramerate
    }

}