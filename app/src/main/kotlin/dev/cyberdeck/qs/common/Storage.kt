package dev.cyberdeck.qs.common

import android.content.Context
import android.os.Environment

fun Context.prepStorageDir() = getExternalFilesDir(
    Environment.DIRECTORY_PICTURES
)!!.also {
    if (!it.mkdirs()) {
        debug("Directory not created")
    }
}