# AndroidSDK-Example

[![Version Badge](https://api.bintray.com/packages/datadome-org/datadome/datadome-android-sdk/images/download.svg)](https://datadome.co/)
![MIT](https://img.shields.io/cocoapods/l/DataDomeSDK)

This sample show how to use __DataDomeSDK__ with the OKHTTP Interceptor and manual integration (:warning:).

## Usage

It is possible test the integration thanks to the multiple buttons.

##### User Agent
You can change UserAgent between:
- __*ALLOWUA*__ : A basic UserAgent. Request will generally pass Datadome.
- __*BLOCKINGUA*__ : Request will be blocked each time by Datadome and a captcha will be shown.

##### Multiple requests
If the switch is enabled, the request button will execute 5 requests and not only 1.

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
