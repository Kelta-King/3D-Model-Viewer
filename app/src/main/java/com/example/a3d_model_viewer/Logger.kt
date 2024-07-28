package com.example.a3d_model_viewer

import android.util.Log

class Logger {
    companion object {
        fun info(logString: String) {
            Log.i(AppUtils.LOG_TAG, logString)
        }

        fun error(logString: String) {
            Log.e(AppUtils.LOG_TAG, logString)
        }

        fun debug(logString: String) {
            Log.d(AppUtils.LOG_TAG, logString)
        }

        fun warning(logString: String) {
            Log.w(AppUtils.LOG_TAG, logString)
        }

    }
}