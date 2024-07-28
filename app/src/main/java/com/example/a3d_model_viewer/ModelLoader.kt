package com.example.a3d_model_viewer

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.view.Choreographer
import android.view.SurfaceView
import com.google.android.filament.Skybox
import com.google.android.filament.utils.KtxLoader
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

class ModelLoader {

    companion object {
        // Initialize the Filament utility functions
        fun init() {
            Utils.init()
        }
    }

    // Enum to specify model formats
    public enum class Formats {
        GLB,
        GLTF
    }

    // Lateinit properties for essential components
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mChoreographer: Choreographer
    private lateinit var mModelViewer: ModelViewer
    private lateinit var mAssets: AssetManager

    // Method to set up the 3D model viewer
    @SuppressLint("ClickableViewAccessibility")
    fun onCreate(surfaceView: SurfaceView, choreographer: Choreographer, assets: AssetManager,
                 modelName: String, environmentName: String, format: Formats) {
        // Initialize the components
        mSurfaceView = surfaceView
        mChoreographer = choreographer
        mModelViewer = ModelViewer(mSurfaceView)
        mAssets = assets

        // Set touch listener for interaction
        mSurfaceView.setOnTouchListener(mModelViewer)

        // Load the model and environment
        loadModel(modelName, format)
        loadEnvironment(environmentName)

        // Set the skybox for the scene
        mModelViewer.scene.skybox = Skybox.Builder().build(mModelViewer.engine)
    }

    // Frame callback to continuously render the scene
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(currentTime: Long) {
            mChoreographer.postFrameCallback(this)
            mModelViewer.render(currentTime)
        }
    }

    // Method to load the model based on the format
    private fun loadModel(modelName: String, format: Formats) {
        Logger.info("Start loading $modelName with ${Formats.GLB}")
        when (format) {
            Formats.GLB -> loadGlb(modelName)
            Formats.GLTF -> loadGltf(modelName)
            else -> Logger.error("Invalid format provided")
        }
    }

    // Method to resume rendering
    fun onResume() {
        mChoreographer.postFrameCallback(frameCallback)
    }

    // Method to pause rendering
    fun onPause() {
        mChoreographer.removeFrameCallback(frameCallback)
    }

    // Method to stop rendering and clean up resources
    fun onDestroy() {
        mChoreographer.removeFrameCallback(frameCallback)
        mModelViewer.destroyModel()
        // Destroy the Filament engine and other related resources
        // mModelViewer.engine.destroy()
    }

    // Method to load a GLB model
    private fun loadGlb(name: String) {
        val buffer = readAsset("models/$name.glb")
        mModelViewer.loadModelGlb(buffer)
        mModelViewer.transformToUnitCube()
    }

    // Method to load a GLTF model
    private fun loadGltf(name: String) {
        val buffer = readAsset("models/${name}.gltf")
        mModelViewer.loadModelGltf(buffer) { uri -> readAsset("models/$uri") }
        mModelViewer.transformToUnitCube()
    }

    // Method to read an asset file into a ByteBuffer
    private fun readAsset(assetName: String): ByteBuffer {
        return try {
            val input = mAssets.open(assetName)
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        } catch (e: Exception) {
            Logger.error("Error: " + e.message.toString())
            ByteBuffer.allocate(0)
        }
    }

    // Method to load the environment for the scene
    private fun loadEnvironment(ibl: String) {
        // Load the indirect light source
        var buffer = readAsset("envs/$ibl/${ibl}_ibl.ktx")
        KtxLoader.createIndirectLight(mModelViewer.engine, buffer).apply {
            intensity = 50_000f
            mModelViewer.scene.indirectLight = this
        }

        // Load the skybox
        buffer = readAsset("envs/$ibl/${ibl}_skybox.ktx")
        KtxLoader.createSkybox(mModelViewer.engine, buffer).apply {
            mModelViewer.scene.skybox = this
        }
    }
}
