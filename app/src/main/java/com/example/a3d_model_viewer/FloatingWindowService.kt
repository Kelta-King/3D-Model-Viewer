package com.example.a3d_model_viewer

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView

// Floating window service extending Service
class FloatingWindowService : Service() {

    companion object {
        // Initialize the Model Loader
        init {
            ModelLoader.init()
        }
    }

    // Lateint properties for important components
    private lateinit var floatView: ViewGroup
    private lateinit var floatWindowLayoutParams: WindowManager.LayoutParams
    private var LAYOUT_TYPE: Int? = null
    private lateinit var windowManager: WindowManager

    private lateinit var btnMax: ImageView
    private lateinit var btnClose: ImageView

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelLoader: ModelLoader

    private var screenHeight:Int = 0
    private var screenWidth:Int = 0
    private var windowHeight:Int = 0
    private var windowWidth:Int = 0

    // Not a binder service
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Creating the service
    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        // Calculation of metrices
        val metrics = applicationContext.resources.displayMetrics
        screenHeight = metrics.heightPixels
        screenWidth = metrics.widthPixels

        // Window manager object creation
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Using inflater to inflate floating_layout in floatView as viewGroup
        val inflater = baseContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatView = inflater.inflate(R.layout.floating_layout, null) as ViewGroup


        // Layout's buttons interaction
        btnMax = floatView.findViewById(R.id.maximizeButton)
        btnClose = floatView.findViewById(R.id.closeButton)

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

        // ModelLoader interaction for 3D viewer
        surfaceView = floatView.findViewById(R.id.surfaceView)
        choreographer = Choreographer.getInstance()
        modelLoader = ModelLoader()
        modelLoader.onCreate(surfaceView, choreographer, assets, "construction_worker", "venetian_crossroads_2k", ModelLoader.Formats.GLB)

        // Defining the layout type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_TOAST
        }

        // Defining height and width of floating window
        windowHeight = (screenHeight * 0.55).toInt()
        windowWidth = (screenWidth * 0.55).toInt()

        // Floating window parameters definition
        floatWindowLayoutParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            LAYOUT_TYPE!!,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Defining the gravity of float view
        floatWindowLayoutParams.gravity = Gravity.CENTER
        floatWindowLayoutParams.x = 0
        floatWindowLayoutParams.y = 0

        // On touch listeners of float view
        floatView.setOnTouchListener(object : View.OnTouchListener {
            val updatedFloatWindowLayoutParams = floatWindowLayoutParams
            var x = 0f
            var y = 0f
            var px = 0.0
            var py = 0.0
            var tempX:Int = 0
            var tempY:Int = 0
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
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

        // Adding floatView to the window using window manager with parameters
        windowManager.addView(floatView, floatWindowLayoutParams)

        // Starting the modelLoader
        modelLoader.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        modelLoader.onDestroy()
        stopSelf()
        windowManager.removeView(floatView)
    }
}
