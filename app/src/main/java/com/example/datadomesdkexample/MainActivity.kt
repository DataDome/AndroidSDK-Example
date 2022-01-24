package com.example.datadomesdkexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.datadome.sdk.base.SDKBase
import co.datadome.sdk.base.SDKBaseListener
import co.datadome.sdk.model.BackBehavior
import co.datadome.sdk.model.Request
import co.datadome.sdk.model.Response
import co.datadome.sdk.okhttp.interceptor.DataDomeOkHttp3Interceptor
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread
import kotlin.random.Random


class MainActivity: AppCompatActivity() {

    private lateinit var dataDomeSDK: SDKBase
    private val url = "https://datadome.co/wp-json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkCameraPermission()
        dataDomeSDK = SDKBase
            .Builder()
            .listener(captchaListener)
            .context(this)
            .backBehavior(BackBehavior.GO_BACK)
            .build()

        val manualButton: Button = findViewById(R.id.manualButton)
        manualButton.setOnClickListener {
            sendManualRequest()
        }

        val okHttpButton: Button = findViewById(R.id.okHttpButton)
        okHttpButton.setOnClickListener {
            sendOkHttpRequest()
        }

        val clearCache: Button = findViewById(R.id.clearCache)
        clearCache.setOnClickListener {
            clearCache()
        }

        // Back behavior selector
        val backBehaviours = arrayOf(
            BackBehavior.GO_BACK, BackBehavior.BLOCKED, BackBehavior.GO_BACKGROUND)
        val adapter: ArrayAdapter<BackBehavior> = ArrayAdapter<BackBehavior>(
            this,
            android.R.layout.simple_spinner_item,
            backBehaviours
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val backBehaviorSpinner = findViewById<Spinner>(R.id.backBehaviorSpinner)
        backBehaviorSpinner.adapter = adapter

        backBehaviorSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val choice = adapter.getItem(position) ?: BackBehavior.GO_BACK
                dataDomeSDK.backBehavior = choice
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }

    private fun sendManualRequest() {
        thread {
            val url = URL(url)
            with(url.openConnection() as HttpsURLConnection) {
                //set request method
                requestMethod = "GET"

                //set headers
                addRequestProperty("Accept", "application/json")
                addRequestProperty("User-Agent", "BLOCKUA")

                val cookie = dataDomeSDK.cookie(url.toString())
                if (!cookie.isNullOrBlank()) {
                    addRequestProperty("Cookie", cookie)
                }

                val requestHeaders = requestProperties


                val br: BufferedReader = if (responseCode == 200) {
                    BufferedReader(InputStreamReader(inputStream))
                } else {
                    BufferedReader(InputStreamReader(errorStream))
                }

                val sb = StringBuilder()
                var output: String?
                while (br.readLine().also { output = it } != null) {
                    sb.append(output)
                }

                dataDomeSDK.validateResponse(
                    response = Response(
                        responseCode,
                        sb.toString(),
                        headerFields
                    ),
                    request = Request(
                        url.toString(),
                        requestHeaders
                    ),
                    retryCallback = {
                        Log.i("[DataDome]", "Retrying failed request")
                        sendManualRequest()
                    },
                    successCallback = {
                        runOnUiThread {
                            Log.i("[DataDome]", "Request did succeed")
                            showStatus(responseCode)
                        }
                    }
                )
            }
        }
    }

    private fun sendOkHttpRequest() {
        thread {
            //Setup the interceptor
            val interceptor = DataDomeOkHttp3Interceptor(dataDomeSDK)

            //Setup the client
            val client = okhttp3.OkHttpClient()
                .newBuilder()
                .addInterceptor(interceptor)
                .build()

            //Create the request
            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("User-Agent", "BLOCKUA")
                .build()

            //Execute the request
            val response = client.newCall(request).execute()
            runOnUiThread {
                showStatus(response.code)
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    private fun clearCache() {
        this.getSharedPreferences("co.datadome.sdk_preferences", Context.MODE_PRIVATE)
            .edit().clear().commit()

        //clear the displayed response
        val responseTextView = findViewById<TextView>(R.id.responseTextView)
        responseTextView.text = ""
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        dataDomeSDK.logGesture(window.decorView.rootView)
        return super.onTouchEvent(event)
    }

    private val captchaListener = object : SDKBaseListener {
        override fun onCaptchaSucceed() {
            Toast.makeText(
                this@MainActivity,
                "captcha solved successfully!",
                Toast.LENGTH_SHORT
            )
                .show()
        }

        override fun onCaptchaDismissed() {
            Toast.makeText(
                this@MainActivity,
                "captcha has been dismissed",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onCaptchaError() {
            Toast.makeText(
                this@MainActivity,
                "an error has been detected when displaying captcha",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onCaptchaLoaded() {
            Toast.makeText(
                this@MainActivity,
                "Captcha did finish loading",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                1000
            )
        }
    }


    @SuppressLint("SetTextI18n")
    fun showStatus(code: Int) {
        val responseTextView = findViewById<TextView>(R.id.responseTextView)

        //update randomly the text color
        val color: Int = Color.argb(
            255,
            Random.nextInt(256),
            Random.nextInt(256),
            Random.nextInt(256)
        )
        responseTextView.setTextColor(color)

        if (code == 200) {
            responseTextView.visibility = View.VISIBLE
            responseTextView.text = "RESULT SUCCESS 🤩"
        } else {
            responseTextView.visibility = View.VISIBLE
            responseTextView.text = "RESULT FAILED 😡"
        }
    }
}

