package dev.cyberdeck.qs.common

import android.os.Debug
import android.util.Log

fun debug(str: String) {
    if (Debug.isDebuggerConnected()) {
        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.size >= 4) {
            val callingElement = stackTrace[3]
            val callingClassName = callingElement.className
            val callingMethodName = callingElement.methodName
            Log.d(callingClassName, "$callingMethodName :: $str")
        } else {
            Log.d("Debug", str)
        }
    }
}