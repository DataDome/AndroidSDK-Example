# IOSSDK-Example

[![Version Badge](https://api.bintray.com/packages/datadome-org/datadome/datadome-android-sdk/images/download.svg)](https://datadome.co/)
![MIT](https://img.shields.io/cocoapods/l/DataDomeSDK)

This sample show you 2 ways to use __DataDomeSDK__:
- DataDomeSDK network layer
- OKHTTP Interceptor

## Usage

It is possible test both integration mode thanks to the switch button. It switches between:
- `DataDomeSDK.get(endpoint)` way to make a request
- Creation of a basic OKHTTP builder and add `DataDomeInterceptor` to it.

##### User Agent
You can change UserAgent between:
- __*ALLOWUA*__ : A basic UserAgent. Request will generally pass Datadome.
- __*BLOCKINGUA*__ : Request will be blocked each time by Datadome and a captcha will be shown.

##### Cache
It is possible to clear cache for triggering captcha again if you want to.

##### Customization
You can customize endpoint in `config.properties` file.
You can customize UserAgents directly in the activity.
Finally, it is possible to change datadome sdk key in the `build.gradle` file of the app.

## Documentation

__Documentation__ can be found here:
*https://docs.datadome.co/docs/android-sdk*

__Changelog__ can be found here:
*https://docs.datadome.co/docs/android-sdk-changelog*
