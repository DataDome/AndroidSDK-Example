package com.example.datadomesdkexample

import android.content.Context
import android.content.res.Resources
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.*


internal object Helper {
    private val TAG = "Helper"

    fun getConfigValue(context: Context, name: String): String? {
        val resources = context.resources
        var resource: InputStream? = null

        try {
            resource = resources.openRawResource(R.raw.config)
            val properties = Properties()
            properties.load(resource)
            return properties.getProperty(name)
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Unable to find the config file: " + e.message)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open config file.")
        } finally {
            if (resource != null) {
                try {
                    resource.close()
                } catch (e: IOException) {
                    Log.w(TAG, e.message)
                }

            }
        }

        return null
    }
}
