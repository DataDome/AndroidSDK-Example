# AndroidSDK-Example

[![Version Badge](https://api.bintray.com/packages/datadome-org/datadome/datadome-android-sdk/images/download.svg)](https://datadome.co/)
![MIT](https://img.shields.io/cocoapods/l/DataDomeSDK)

This sample show how to use __DataDomeSDK__ with the OKHTTP Interceptor and manual integration (:warning:).

## Usage

It is possible test the integration thanks to the multiple buttons.

##### User Agent
*BLOCKINGUA*: Request will be blocked each time by Datadome and a captcha will be shown.
You can change add UserAgent to *BLOCKUA* by adding the header 
__*OKHTTP*__ : val request = okhttp3.Request.Builder()
                               .url(url)
                               .addHeader("User-Agent", "BLOCKUA")
                               .build()
__*Manual Integration*__ :  addRequestProperty("User-Agent", "BLOCKUA")
                               

##### Multiple requests
You can perform multiple requests and the SDK will check every single request and retry all of them after the captcha check, and we make sure that the captcha is displayed only for the first request failure.

##### Cache
It is possible to clear cache for triggering captcha again if you want to.

#### Back Button Behaviour
It is possible to set a custom behaviour for the back action on captcha page:
- __*GO_BACKGROUND*__ : Make your app go background. Its the default and recommended one.
- __*GO_BACK*__ : Go back and cancel the call.
- __*BLOCKED*__ : Back button make nothing.

##### Customization
You can customize endpoint in `config.properties` file.
You can customize UserAgents directly in the activity.
Finally, it is possible to change datadome sdk key in the `build.gradle` file of the app.

## Documentation

__Documentation__ can be found here:
*https://docs.datadome.co/docs/sdk-android*

__Changelog__ can be found here:
*https://docs.datadome.co/docs/android-sdk-changelog*
