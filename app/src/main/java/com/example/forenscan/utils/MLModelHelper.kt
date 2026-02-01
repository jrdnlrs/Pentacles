package com.example.forenscan.utils

import android.content.Context
import android.util.Log
// Change 1: Update imports to use the LiteRT namespace
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.Accelerator
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MLModelHelper - Wrapper for LiteRT model
 * Handles model loading and inference for Evil Twin detection
 */
class MLModelHelper(context: Context) {
    // Change 2: Replace Interpreter with CompiledModel
    private var compiledModel: CompiledModel? = null

    private val NUM_FEATURES = 3
    private val TAG = "MLModelHelper"

    init {
        try {
            // Change 3: Initialize CompiledModel directly from the assets path
            // LiteRT handles the loading internally; ensure "RFC_EvilTwin.tflite" is in src/main/assets
            val options = CompiledModel.Options(Accelerator.CPU) // Or Accelerator.GPU for 16KB-aligned acceleration

            // Note: Use the relative path within assets
            compiledModel = CompiledModel.create(context.assets, "RFC_EvilTwin.tflite", options)
            Log.d(TAG, "LiteRT CompiledModel initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LiteRT: ${e.message}")
            e.printStackTrace()
        }
    }

//    /**
//     * Predict if a network is an Evil Twin
//     * @param features Array of [frameProtection, signalStrength, dataRate]
//     * @return Probability of being Evil Twin (0.0 to 1.0)
//     */
//
//    fun predict(features: FloatArray): Float {
//        val model = compiledModel ?: return -1f
//
//        if (features.size != NUM_FEATURES) {
//            throw IllegalArgumentException("Expected $NUM_FEATURES features, got ${features.size}")
//        }
//
//        // 1. Prepare buffers
//        val inputBuffers = model.createInputBuffers()
//        val outputBuffers = model.createOutputBuffers()
//
//        // 2. Load data manually
//        // We access the raw ByteBuffer directly.
//        val inputBuffer = inputBuffers[0] as ByteBuffer
//        inputBuffer.clear()
//        inputBuffer.order(ByteOrder.nativeOrder()) // Essential for ML models
//
//        val floatBuffer = inputBuffer.asFloatBuffer()
//        floatBuffer.put(features)
//
//        return try {
//            // 3. Run inference
//            model.run(inputBuffers, outputBuffers)
//
//            // 4. Read result manually
//            val outputBuffer = outputBuffers[0] as ByteBuffer
//            outputBuffer.rewind() // Reset position before reading
//
//            val result = outputBuffer.asFloatBuffer().get(0)
//            result
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Inference error: ${e.message}")
//            -1f                   // Make it the last line
//        }
//    }

    /**
     * Predict if a network is an Evil Twin
     * @param features Array of [frameProtection, signalStrength, dataRate]
     * @return Probability of being Evil Twin (0.0 to 1.0)
     */
    fun predict(features: FloatArray): Float {
        val model = compiledModel ?: return -1f

        if (features.size != NUM_FEATURES) {
            throw IllegalArgumentException("Expected $NUM_FEATURES features, got ${features.size}")
        }

        try {
            // 1. Prepare buffers
            val inputBuffers = model.createInputBuffers()
            val outputBuffers = model.createOutputBuffers()

            // 2. Load data manually
            // FIX: The array contains ByteBuffers directly. No 'TensorBuffer' cast needed.
            val inputBuffer = inputBuffers[0] as ByteBuffer
            inputBuffer.clear()
            inputBuffer.order(ByteOrder.nativeOrder()) // Essential for ML models

            // Write the float array into the buffer
            val floatBuffer = inputBuffer.asFloatBuffer()
            floatBuffer.put(features)

            // 3. Run inference
            model.run(inputBuffers, outputBuffers)

            // 4. Read result manually
            // FIX: Access the output ByteBuffer directly
            val outputBuffer = outputBuffers[0] as ByteBuffer
            outputBuffer.order(ByteOrder.nativeOrder()) // Ensure byte order matches
            outputBuffer.rewind() // Reset position before reading

            // The model returns a single float probability
            // FIX: Explicitly type 'result' to help the compiler
            val result: Float = outputBuffer.asFloatBuffer().get(0)
            return result

        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            e.printStackTrace()
            return -1f
        }
    }

    fun close() {
        // Change 6: Clean up resources
        compiledModel?.close()
        compiledModel = null
    }
}

/**
 * Data class for ML prediction result remains unchanged
 */
data class MLPrediction(
    val probability: Float,
    val isEvilTwin: Boolean,
    val confidence: String
) {
    companion object {
        const val THRESHOLD = 0.7f

        fun from(probability: Float): MLPrediction {
            val isEvil = probability >= THRESHOLD
            val confidence = when {
                probability >= 0.9f -> "HIGH"
                probability >= 0.7f -> "MEDIUM"
                else -> "LOW"
            }
            return MLPrediction(probability, isEvil, confidence)
        }
    }
}

//package com.example.forenscan.utils
//
//import android.content.Context
//import android.util.Log
//import com.google.ai.edge.litert.CompiledModel
//import com.google.ai.edge.litert.Accelerator
//import com.google.ai.edge.litert.TensorBuffer // Import added
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//
///**
// * MLModelHelper - Wrapper for LiteRT model
// * Handles model loading and inference for Evil Twin detection
// */
//class MLModelHelper(context: Context) {
//
//    private var compiledModel: CompiledModel? = null
//    private val NUM_FEATURES = 3
//    private val TAG = "MLModelHelper"
//
//    init {
//        try {
//            // Initialize CompiledModel directly from the assets path
//            // Note: Use Accelerator.CPU for compatibility, or GPU if supported
//            val options = CompiledModel.Options(Accelerator.CPU)
//
//            // Ensure "RFC_EvilTwin.tflite" is in src/main/assets
//            compiledModel = CompiledModel.create(context.assets, "RFC_EvilTwin.tflite", options)
//            Log.d(TAG, "LiteRT CompiledModel initialized successfully")
//        } catch (e: Exception) {
//            Log.e(TAG, "Error initializing LiteRT: ${e.message}")
//            e.printStackTrace()
//        }
//    }
//
//    /**
//     * Predict if a network is an Evil Twin
//     * @param features Array of [frameProtection, signalStrength, dataRate]
//     * @return Probability of being Evil Twin (0.0 to 1.0)
//     */
//    fun predict(features: FloatArray): Float {
//        val model = compiledModel ?: return -1f
//
//        if (features.size != NUM_FEATURES) {
//            throw IllegalArgumentException("Expected $NUM_FEATURES features, got ${features.size}")
//        }
//
//        try {
//            // 1. Prepare buffers
//            val inputBuffers = model.createInputBuffers()
//            val outputBuffers = model.createOutputBuffers()
//
//            // 2. Load data manually
//            // FIX: Cast to TensorBuffer first, then access the internal ByteBuffer
//            val inputTensor = inputBuffers[0] as TensorBuffer
//            val inputBuffer = inputTensor.buffer
//
//            inputBuffer.order(ByteOrder.nativeOrder()) // Essential for ML models
//
//            // Write the float array into the buffer
//            val floatBuffer = inputBuffer.asFloatBuffer()
//            floatBuffer.put(features)
//
//            // 3. Run inference
//            model.run(inputBuffers, outputBuffers)
//
//            // 4. Read result manually
//            // FIX: Cast to TensorBuffer first, then access the internal ByteBuffer
//            val outputTensor = outputBuffers[0] as TensorBuffer
//            val outputBuffer = outputTensor.buffer
//
//            outputBuffer.rewind() // Reset position before reading
//
//            // The model returns a single float probability
//            val result = outputBuffer.asFloatBuffer().get(0)
//            return result
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Inference error: ${e.message}")
//            e.printStackTrace()
//            return -1f
//        }
//    }
//
//    fun close() {
//        try {
//            compiledModel?.close()
//            compiledModel = null
//        } catch (e: Exception) {
//            Log.e(TAG, "Error closing model: ${e.message}")
//        }
//    }
//}
//
///**
// * Data class for ML prediction result
// */
//data class MLPrediction(
//    val probability: Float,
//    val isEvilTwin: Boolean,
//    val confidence: String
//) {
//    companion object {
//        const val THRESHOLD = 0.7f
//
//        fun from(probability: Float): MLPrediction {
//            val isEvil = probability >= THRESHOLD
//            val confidence = when {
//                probability >= 0.9f -> "HIGH"
//                probability >= 0.7f -> "MEDIUM"
//                else -> "LOW"
//            }
//            return MLPrediction(probability, isEvil, confidence)
//        }
//    }
//}