package com.menghai.camerademo.Audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

private const val SAMPLE_RATE = 16000
private const val TAG = "AudioRecordUtils"

object AudioRecordUtils {

    private var isRecording = false

    fun start(context: Context) {
        thread {
            if (isRecording) {
                log("已经在录音")
                return@thread
            }
            val bufferSizeInBytes = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes * 2
            )
            audioRecord.startRecording()
            val pcmFilePath = context.getExternalFilesDir("")?.absolutePath + File.separator + "vcRecord.pcm"
            val file = File(pcmFilePath)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            log("文件路径 ${file.absolutePath}")
            val outputStream = FileOutputStream(pcmFilePath)
            isRecording = true
            val tempBytes = ByteArray(1024)
            while (isRecording) {
                val read = audioRecord.read(tempBytes, 0, tempBytes.size)
                outputStream.write(tempBytes)
            }
            outputStream.flush()
            outputStream.close()
            audioRecord.stop()
            audioRecord.release()
            log("结束录音")
        }
    }

    fun stop() {
        isRecording = false
    }

    fun log(str: String) {
        Log.d(TAG, str)
    }

}