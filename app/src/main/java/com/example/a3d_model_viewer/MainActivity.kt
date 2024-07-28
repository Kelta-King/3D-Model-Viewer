package com.example.a3d_model_viewer

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import android.view.Choreographer
import android.view.SurfaceView
import android.widget.Button
import android.widget.Toast

class MainActivity : ComponentActivity() {

    companion object {
        // Initialize the Model Loader
        init { ModelLoader.init() }
    }

    // Lateinit properties for essential components
    private lateinit var dialog: AlertDialog
    private lateinit var btnMin: Button
    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelLoader: ModelLoader

    // Lifecycle method called when the activity is created
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize views
        setContentView(R.layout.activity_main)
        surfaceView = findViewById(R.id.surfaceView)
        btnMin = findViewById(R.id.minimizeButton)

        // Set the click listener for the minimize button
        btnMin.setOnClickListener {
            // Check if the overlay permission is granted
            if(checkOverlayPermission()) {
                // Start the floating window service
                startService(Intent(this@MainActivity, FloatingWindowService::class.java))
                finish()
            }
            else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                // Request the overlay permission
                requestFloatingWindowPermission()
            }
        }

        // Initialize choreographer and model loader started
        choreographer = Choreographer.getInstance()
        modelLoader = ModelLoader()
        modelLoader.onCreate(surfaceView, choreographer, assets, "scene", "venetian_crossroads_2k", ModelLoader.Formats.GLTF)
    }

    // Lifecycle method called when the activity is resumed
    override fun onResume() {
        super.onResume()
        modelLoader.onResume()
    }

    // Lifecycle method called when the activity is paused
    override fun onPause() {
        super.onPause()
        modelLoader.onPause()
    }

    // Lifecycle method called when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        modelLoader.onDestroy()
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if(FloatingWindowService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    // Method to request the floating window permission
    private fun requestFloatingWindowPermission() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle("Screen overlay permission")
        builder.setMessage("Enable display over other apps")
        builder.setPositiveButton("Open Settings", DialogInterface.OnClickListener{ _, _ ->
            // Open the settings to grant overlay permission
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, RESULT_OK)
        })
        dialog = builder.create()
        dialog.show()
    }

    // Method to check if the overlay permission is granted
    private fun checkOverlayPermission(): Boolean {
        return if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        }
        else true
    }
}
