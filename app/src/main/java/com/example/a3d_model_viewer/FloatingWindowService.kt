package com.example.a3d_model_viewer

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import com.google.android.filament.Skybox
import com.google.android.filament.utils.KtxLoader
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

class FloatingWindowService : Service() {

    companion object {
        init { Utils.init() }
    }

    private lateinit var floatView: ViewGroup
    private lateinit var floatWindowLayoutParams: WindowManager.LayoutParams
    private var LAYOUT_TYPE: Int? = null
    private lateinit var windowManager: WindowManager

    private lateinit var btnMax: ImageView
    private lateinit var btnClose: ImageView
    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer

    private var screenHeight:Int = 0
    private var screenWidth:Int = 0
    private var windowHeight:Int = 0
    private var windowWidth:Int = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val metrics = applicationContext.resources.displayMetrics
        screenHeight = metrics.heightPixels
        screenWidth = metrics.widthPixels

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = baseContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

        floatView = inflater.inflate(R.layout.floating_layout, null) as ViewGroup

        surfaceView = floatView.findViewById(R.id.surfaceView)
        btnMax = floatView.findViewById(R.id.maximizeButton)
        btnClose = floatView.findViewById(R.id.closeButton)

        choreographer = Choreographer.getInstance()
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)

        loadGlb("DamagedHelmet")
        loadEnvironment("venetian_crossroads_2k")
        modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_TOAST
        }

        windowHeight = (screenHeight * 0.55).toInt()
        windowWidth = (screenWidth * 0.55).toInt()

        floatWindowLayoutParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            LAYOUT_TYPE!!,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        floatWindowLayoutParams.gravity = Gravity.CENTER
        floatWindowLayoutParams.x = 0
        floatWindowLayoutParams.y = 0

        windowManager.addView(floatView, floatWindowLayoutParams)

        btnMax.setOnClickListener {
            stopSelf()
            windowManager.removeView(floatView)
            val back = Intent(this@FloatingWindowService, MainActivity::class.java)
            back.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(back)
        }

        btnClose.setOnClickListener {
            stopSelf()
            windowManager.removeView(floatView)
        }

        floatView.setOnTouchListener(object : View.OnTouchListener {
            val updatedFloatWindowLayoutParams = floatWindowLayoutParams
            var x = 0f
            var y = 0f
            var px = 0.0
            var py = 0.0
            var tempX:Int = 0
            var tempY:Int = 0
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                Log.i("kelta", "On touch")
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = updatedFloatWindowLayoutParams.x.toFloat()
                        y = updatedFloatWindowLayoutParams.y.toFloat()

                        px = event.rawX.toDouble()
                        py = event.rawY.toDouble()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        tempX = (x + event.rawX - px).toInt()
                        tempY = (y + event.rawY - py).toInt()

                        // Width check to avoid going beyond the screen
                        if(tempX - (windowWidth / 2) <= (-(screenWidth / 2))) tempX = -(screenWidth / 2)
                        if(tempX + (windowWidth / 2) > ((screenWidth / 2))) tempX = (screenWidth / 2)

                        // Height check to avoid going beyond the screen
                        if(tempY - (windowHeight / 2) <= (-(screenHeight / 2))) tempY = -(screenHeight / 2)
                        if(tempY + (windowHeight / 2) > (screenHeight / 2)) tempY = (screenHeight / 2)

                        updatedFloatWindowLayoutParams.x = tempX
                        updatedFloatWindowLayoutParams.y = tempY

                        windowManager.updateViewLayout(floatView, updatedFloatWindowLayoutParams)
                    }
                }
                return false
            }
        })

        // Start the frame callback
        choreographer.postFrameCallback(frameCallback)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(currentTime: Long) {
            choreographer.postFrameCallback(this)
            modelViewer.render(currentTime)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallback)
        stopSelf()
        windowManager.removeView(floatView)

    }

    private fun loadGlb(name: String) {
        val buffer = readAsset("models/$name.glb")
        modelViewer.loadModelGlb(buffer)
        modelViewer.transformToUnitCube()
    }

    private fun readAsset(assetName: String): ByteBuffer {
        return try {
            val input = assets.open(assetName)
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        } catch (e: Exception) {
            Log.e("Kelta", "Error: " + e.message.toString())
            ByteBuffer.allocate(0)
        }
    }

    private fun loadEnvironment(ibl: String) {
        // Create the indirect light source and add it to the scene.
        var buffer = readAsset("envs/$ibl/${ibl}_ibl.ktx")
        KtxLoader.createIndirectLight(modelViewer.engine, buffer).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }

        // Create the sky box and add it to the scene.
        buffer = readAsset("envs/$ibl/${ibl}_skybox.ktx")
        KtxLoader.createSkybox(modelViewer.engine, buffer).apply {
            modelViewer.scene.skybox = this
        }
    }
}
