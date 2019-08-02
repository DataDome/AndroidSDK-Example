package com.example.datadomesdkexample

import android.Manifest
import android.app.Activity
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import co.datadome.sdk.DataDomeInterceptor
import co.datadome.sdk.DataDomeSDK
import co.datadome.sdk.internal.DataDomeActivity
import co.datadome.sdk.internal.DataDomeEvent
import co.datadome.sdk.internal.DataDomeSDKListener
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference

class MainActivity: AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val BLOCKUA = "BLOCKUA"
        private val ALLOWUA =
            "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
    }

    private lateinit var userAgent: String
    private var dataDomeSdk: DataDomeSDK.Builder? = null

    private var dataDomeSDKListener: DataDomeSDKListener = object: DataDomeSDKListener() {
        override fun onDataDomeResponse(code: Int, response: String?) {
            Log.d(TAG, "onDataDomeResponse")
            runOnUiThread {
                if (response != null)
                    Toast.makeText(this@MainActivity, "Response code: $code", Toast.LENGTH_LONG).show()
            }
        }

        override fun onError(errno: Int, error: String?) {
            Log.d(TAG, "onError")
            runOnUiThread { Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_LONG).show() }
        }

        override fun onHangOnRequest(code: Int) {
            Log.d(TAG, "onHangOnRequest")
            super.onHangOnRequest(code)
            runOnUiThread { Toast.makeText(this@MainActivity, "HangOn Request - code: $code", Toast.LENGTH_SHORT).show() }
        }

        override fun onCaptchaSuccess() {
            Log.d(TAG, "onCaptchaSuccess")
            super.onCaptchaSuccess()
            runOnUiThread { Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show() }
        }

        override fun onCaptchaCancelled() {
            Log.d(TAG, "onCaptchaCancelled")
            super.onCaptchaCancelled()
            runOnUiThread { Toast.makeText(this@MainActivity, "User cancelled captcha", Toast.LENGTH_SHORT).show() }
        }

        override fun onCaptchaLoaded() {
            Log.d(TAG, "onCaptchaLoaded")
            super.onCaptchaLoaded()
        }

        override fun onCaptchaDismissed() {
            Log.d(TAG, "onCaptchaDismissed")
            super.onCaptchaDismissed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userAgent = BLOCKUA
        currentua.text = userAgent

        dataDomeSdk = DataDomeSDK
            .with(application, BuildConfig.DATADOME_SDK_KEY, BuildConfig.VERSION_NAME)
            .listener(dataDomeSDKListener)
            .agent(userAgent)

        ActivityCompat.requestPermissions(this, arrayOf<String>(Manifest.permission.CAMERA), 101)

        _clearCache()
    }

    fun switchUserAgent(v: View) {
        userAgent = if (BLOCKUA == userAgent) ALLOWUA else BLOCKUA
        currentua.text = userAgent
        dataDomeSdk?.userAgent = this.userAgent
    }

    private fun _clearCache() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply()
        runOnUiThread { Toast.makeText(this@MainActivity, "Cache cleared", Toast.LENGTH_SHORT).show() }
        dataDomeSdk?.logEvent(DataDomeEvent(101, "Cache cleared", MainActivity::class.java.simpleName))
    }

    fun clearCache(v: View) {
        _clearCache()
    }

    fun makeRequest(v: View) {
        val endpoint = Helper.getConfigValue(this, "datadome.endpoint")
        if (use_custom_network_layer_switch.isChecked) {
            makeDatadomeHTTPClientRequest(endpoint)
        } else {
            makeOkHttpRequest(endpoint)
        }
    }

    private fun makeDatadomeHTTPClientRequest(endPoint: String? = "") {
        dataDomeSdk?.get(endPoint)?.apply()
    }

    private fun makeOkHttpRequest(endPoint: String? = "") {
        val dataDomeInterceptor = DataDomeInterceptor(
            application,
            dataDomeSDKListener,
            BuildConfig.DATADOME_SDK_KEY,
            BuildConfig.VERSION_NAME
        )
        var task = OkHttpRequestTask(dataDomeInterceptor)
        task.execute(endPoint, userAgent)
    }

    internal class OkHttpRequestTask(dataDomeInterceptor: DataDomeInterceptor) : AsyncTask<String, Void, Void>() {
        var dataDomeInterceptorRef: WeakReference<DataDomeInterceptor>

        init {
            dataDomeInterceptorRef = WeakReference<DataDomeInterceptor>(dataDomeInterceptor)
        }

        override fun doInBackground(vararg args: String): Void? {
            val dataDomeInterceptor = dataDomeInterceptorRef.get()
            if (dataDomeInterceptor != null) {
                val builder = OkHttpClient.Builder()

                builder.addInterceptor(dataDomeInterceptor)

                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC)
                loggingInterceptor.redactHeader("Authorization")
                builder.addInterceptor(loggingInterceptor)
                val client = builder.build()

                val request = Request.Builder()
                    .header("User-Agent", args[1])
                    .url(args[0])
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    // do what you want with response
                    Log.d(TAG, response.header("Date"))
                    if (response.body() != null) {
                        response.body()?.close()
                    }
                } catch (e: IOException) {
                    Log.d(TAG, e.message)
                }

            }
            return null
        }
    }

//    internal class OkHttpRequestTask(activity: Activity) : AsyncTask<String, Void, Void>() {
//        var activityWeakReference: WeakReference<Activity>
//
//        init {
//            activityWeakReference = WeakReference<Activity>(activity)
//        }
//
//        override fun doInBackground(vararg args: String): Void? {
//            val activity = activityWeakReference.get()
//            if (activity != null) {
//                val builder = OkHttpClient.Builder()
//                builder.addInterceptor(DataDomeInterceptor(
//                    activity.application,
//                    dataDomeSDKListener,
//                    "Client_Side_Key",
//                    BuildConfig.VERSION_NAME)
//                )
//                val loggingInterceptor = HttpLoggingInterceptor()
//                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC)
//                loggingInterceptor.redactHeader("Authorization")
//                builder.addInterceptor(loggingInterceptor)
//                val client = builder.build()
//
//                val postdata = JSONObject()
//                try {
//                    postdata.put("username", "aneh")
//                    postdata.put("password", "12345")
//                } catch (e: JSONException) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace()
//                }
//
//                val MEDIA_TYPE = MediaType.parse("application/json")
//                val body = RequestBody.create(MEDIA_TYPE, postdata.toString())
//
//                val request = Request.Builder()
//                    .header("User-Agent", args[1])
//                    .url(args[0])
//                    .post(body)
//                    .build()
//
//                try {
//                    val response = client.newCall(request).execute()
//                    // do what you want with response
//                    Log.d(TAG, response.header("Date"))
//                    if (response.body() != null) {
//                        response.body()?.close()
//                    }
//                } catch (e: IOException) {
//                    Log.d(TAG, e.message)
//                }
//
//            }
//            return null
//        }
//    }
}
