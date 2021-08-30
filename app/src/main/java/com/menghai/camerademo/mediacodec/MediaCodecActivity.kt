package com.menghai.camerademo.mediacodec

import android.Manifest
import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.lifecycle.Lifecycle
import com.menghai.camerademo.Audio.AudioRecordUtils
import com.menghai.camerademo.R
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

/**
 * @author      zhenhai.zhao
 * @date        2021/8/20 11:18 AM
 * @package     com.menghai.camerademo.mediacodec
 * @email       zhenhai.zhao@asiainnovations.com
 * @description MediaCodec视频拍摄类
 **/
class MediaCodecActivity : Activity(), SurfaceHolder.Callback, PreviewCallback {

    private var surfaceview: SurfaceView? = null

    private var surfaceHolder: SurfaceHolder? = null

    private var camera: Camera? = null

    private var parameters: Camera.Parameters? = null

    var width: Int = 1280

    var height = 720

    var framerate = 30

    var biterate = 8500 * 1000


    private var avcCodec: AvcEncoder? = null

    companion object {
        const val yuvqueuesize = 10

        //待解码视频缓冲队列，静态成员！ 有界阻塞队列
        val YUVQueues = ArrayBlockingQueue<ByteArray>(yuvqueuesize)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec)

        surfaceview = findViewById(R.id.surfaceview)
        surfaceHolder = surfaceview?.holder
        surfaceHolder?.addCallback(this)
    }

    /**
     * Surface创建时触发，一般在这个函数开启绘图线程（新的线程，不要再这个线程中绘制Surface）
     * @param holder
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        camera = getBackCamera()
        startCamera(camera)
        //创建AvEncoder对象
        avcCodec = AvcEncoder()
        avcCodec?.init(width, height, framerate, biterate)
        //启动编码线程
        avcCodec?.startEncoderThread()

        AudioRecordUtils.start(this)
    }

    /**
     * surface大小或格式发生变化时触发，在surfaceCreated调用后该函数至少会被调用一次
     * @param holder
     * @param format
     * @param width
     * @param height
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    /**
     *  销毁时触发，一般不可见时就会销毁
     * @param holder
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (camera != null) {
            camera?.setPreviewCallback(null)
            camera?.stopPreview()
            camera?.release()
            camera = null
            avcCodec?.stopThread()
        }

        AudioRecordUtils.stop()
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        //将当前帧图像保存在队列中
        if (data != null) {
            putYUVData(data)
        }
    }

    private fun putYUVData(buffer: ByteArray) {
        if (YUVQueues.size >= 10) {
            YUVQueues.poll()//取出排在首位的对象
        }
        YUVQueues.add(buffer)
    }


    private fun startCamera(mCamera: Camera?) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(this)
                mCamera.setDisplayOrientation(90)
                if (parameters == null) {
                    parameters = mCamera.parameters
                }
                //获取默认的camera配置
                //parameters = mCamera.parameters
                //设置预览格式
                parameters?.previewFormat = ImageFormat.NV21
                //设置预览图像分辨率
                parameters?.setPreviewSize(width, height)
                //配置camera参数
                mCamera.parameters = parameters
                //将完全初始化的SurfaceHolder传入到setPreviewDisplay(SurfaceHolder)中
                //没有surface的话，相机不会开启preview预览
                mCamera.setPreviewDisplay(surfaceHolder)
                //调用startPreview()用以更新preview的surface，必须要在拍照之前start Preview
                mCamera.startPreview()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    fun getBackCamera(): Camera? {
        var c: Camera? = null
        try {
            //获取Camera的实例
            c = Camera.open(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //获取Camera的实例失败时返回null
        return c
    }

}