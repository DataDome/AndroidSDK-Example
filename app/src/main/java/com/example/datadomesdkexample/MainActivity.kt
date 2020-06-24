package com.example.datadomesdkexample

import android.Manifest
import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import android.view.MotionEvent
import co.datadome.sdk.*
import okio.Buffer
import okio.GzipSource
import java.nio.charset.Charset
import java.util.HashMap


class MainActivity: AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val BLOCKUA = "BLOCKUA"
        private val ALLOWUA =
            "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
    }

    private lateinit var userAgent: String
    private var dataDomeSdk: DataDomeSDK.Builder? = null

    private var dataDomeSDKManualIntegrationListener: DataDomeSDKManualIntegrationListener = object: DataDomeSDKManualIntegrationListener() {
        private val MANUALTAG = "MANUAL " + MainActivity::class.java.simpleName

        override fun onRequestInProgress(requestId: Int?) {
            super.onRequestInProgress(requestId)
            Log.d(MANUALTAG, "onRequestInProgress " + requestId)
        }

        override fun onComplete(requestId: Int?) {
            super.onComplete(requestId)
            Log.d(MANUALTAG, "onComplete")
        }

        override fun onError(requestId: Int?, errorMessage: String?) {
            super.onError(requestId, errorMessage)
            Log.d(MANUALTAG, "onError")
        }
    }

    private var dataDomeSDKListener: DataDomeSDKListener = object: DataDomeSDKListener() {

        override fun onDataDomeResponse(code: Int, response: String?) {
            Log.d(TAG, "onDataDomeResponse")
            super.onDataDomeResponse(code, response)
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
            .manualListener(dataDomeSDKManualIntegrationListener)
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

    fun clickOnBackBehaviour(v: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Back button behaviour")
        builder.setMessage("Choose Back button behaviour for Captcha page")

        builder.setPositiveButton("Background") { dialog, which ->
            dataDomeSdk = dataDomeSdk?.backBehaviour(DataDomeSDK.BackBehaviour.GO_BACKGROUND)
            Toast.makeText(applicationContext,
                "Back button will go in background", Toast.LENGTH_LONG).show()
        }

        builder.setNegativeButton("Close and back") { dialog, which ->
            dataDomeSdk = dataDomeSdk?.backBehaviour(DataDomeSDK.BackBehaviour.GO_BACK)
            Toast.makeText(applicationContext,
                "Back button will close captcha page", Toast.LENGTH_LONG).show()
        }

        builder.setNeutralButton("Nothing") { dialog, which ->
            dataDomeSdk = dataDomeSdk?.backBehaviour(DataDomeSDK.BackBehaviour.BLOCKED)
            Toast.makeText(applicationContext,
                "Back button will make nothing", Toast.LENGTH_LONG).show()
        }
        builder.show()
    }

    fun makeRequest(v: View) {
        val endpoint = Helper.getConfigValue(this, "datadome.endpoint")
        makeOkHttpRequest(endpoint)
    }

    private fun makeOkHttpRequest(endPoint: String? = "") {
        val dataDomeInterceptor = DataDomeInterceptor(
            application,
            dataDomeSdk
        )
        val numberOfRequest = if (switchMultipleRequest.isChecked) 5 else 1
        for (i in 1..numberOfRequest) {
            var task = OkHttpRequestTask(dataDomeInterceptor, "$i", this)
            task.execute(endPoint, userAgent)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        dataDomeSdk?.handleTouchEvent(event);
        return super.onTouchEvent(event)
    }

    internal class OkHttpRequestTask(dataDomeInterceptor: DataDomeInterceptor, customId: String = "", contextForToast: Context? = null) : AsyncTask<String, Void, Void>() {
        var dataDomeInterceptorRef: WeakReference<DataDomeInterceptor>
        var customTextRef: Reference<String>
        var contextForToastRef: Reference<Context?>

        init {
            dataDomeInterceptorRef = WeakReference(dataDomeInterceptor)
            customTextRef = WeakReference(customId)
            contextForToastRef = WeakReference(contextForToast)
        }

        override fun doInBackground(vararg args: String): Void? {
            val dataDomeInterceptor = dataDomeInterceptorRef.get()
            val customText = customTextRef.get()
            val contextForToast = contextForToastRef.get()

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
                    var callback: Callback = object: Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.d(TAG,"ERROR")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            Log.d(TAG,"Task $customText -> ${response.code()}")
                            if (contextForToast != null)
                                (contextForToast as? MainActivity)?.runOnUiThread {
                                    Toast.makeText(contextForToast, "Task $customText response : ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }

                    client.newCall(request).enqueue(callback)
                } catch (e: IOException) {
                    Log.d(TAG, e.message)
                }

            }
            return null
        }
    }

    // Manual integration

    private val UTF8 = Charset.forName("UTF-8")
    private fun handleBlockedResponse(response: Response, idToRedo: Int?) {
        val responseBody = response.body() ?: return
        val request = response.request()
        val userAgent = request.header("User-Agent")
        val headers = HashMap<String, String>()
        val responseHeaders = response.headers()
        val headersNames = responseHeaders.names()
        for (name in headersNames) {
            val value = responseHeaders.get(name)
            if (value != null) {
                headers[name] = value
            }
        }
        val source = responseBody.source()
        source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
        var buffer = source.buffer().clone()

        if ("gzip".equals(headers["Content-Encoding"] ?: "", ignoreCase = true)) {
            GzipSource(buffer.clone()).use { gzippedResponseBody ->
                Buffer().use { buf ->
                    buf.writeAll(gzippedResponseBody)
                    buffer = buf.clone()
                }
            }
        }

        var charset: Charset? = UTF8
        val contentType = responseBody.contentType()
        if (contentType != null) {
            charset = contentType.charset(UTF8)
        }
        if (charset == null) {
            charset = UTF8
        }
        if (charset != null) {
            val content = buffer.readString(charset)
            buffer.close()
            dataDomeSdk?.handleResponse(idToRedo, headers, response.code(), content)
            return
        } else {
            buffer.close()
        }
        dataDomeSdk?.handleResponse(idToRedo, headers, response.code(), "")
    }

    fun manualRequest(idToRedo: Int?) {
        val endpoint = Helper.getConfigValue(this, "datadome.endpoint")
        val builder = OkHttpClient.Builder()
        var headers = HashMap<String, String>()

        headers["User-Agent"] = userAgent
        headers["Accept"] = "application/json"

        //addDataDomeHeaders
        headers = dataDomeSdk?.addDataDomeHeaders(headers) as? HashMap<String, String> ?: HashMap<String, String>()

        var req = Request.Builder()
            .url(endpoint)
        for (h in headers) {
            req.addHeader(h.key, h.value)
        }

        val request = req.build()

        val client = OkHttpClient()

        var callback: Callback = object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG,"ERROR")
            }

            override fun onResponse(call: Call, response: Response) {
                val headers = HashMap<String, String>()
                val responseHeaders = response.headers()
                val headersNames = responseHeaders.names()
                for (name in headersNames) {
                    val value = responseHeaders.get(name)
                    if (value != null) {
                        headers[name] = value
                    }
                }

                val url = call.request().url().toString()
                if (dataDomeSdk?.verifyResponse(url, headers, response.code(), applicationContext) != false) {
                    Log.d("Manual response", "Response handle")
                    handleBlockedResponse(response, idToRedo)
                } else {
                    Log.d("Manual response", "No response handle")
                    runOnUiThread { Toast.makeText(this@MainActivity, "No response handled by DD", Toast.LENGTH_SHORT).show() }
                }
            }
        }
        client.newCall(request).enqueue(callback)
    }

    fun clickOnManualCallButton(v: View) {
        val numberOfRequest = if (switchMultipleRequest.isChecked) 5 else 1
        for (i in 1..numberOfRequest) {
            manualRequest(i)
        }
    }
}

