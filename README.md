# Oxygen Updater (Android App)

This repository contains all the code of the Oxygen Updater Android application.

## Building and running
### Required tools
The application should be buildable with Android Studio 3.2.1 and later.
The app targets Android Pie (api 29) and the minimum is Android Lollipop (API 21). Make sure you've got both platforms installed!

### Key store / signing
Before you can run the non-debug variants of the app, you'll need to create a `key store` to sign the app.
Also, you need to specify its location, alias and credentials in `keystore.properties`.
You can create a key store through Android Studio: `Build -> Generate Signed Bundle / APK -> Next -> Create new (under key store path)`
Then, add the alias, key and store passwords and the file path to `keystore.properties`. 

Note: The file path of the key store is relative to the `app` folder of the project!
It is recommended to save the key store as `app/keyStore.jks`, as this file is ignored from Git. 
In the keyStore properties, name it just `keyStore.jks`.

Example:

```properties
storePassword=passw0rd
keyPassword=passw0rd!
keyAlias=my_app_key_name
storeFile=keyStore.jks  
```



The app can be run within the emulator but is limited to no-root features only.
To test automatic update installations and other root features, you'll need to run it on a rooted device.

## Contributing
See [`CONTRIBUTING.md`](./CONTRIBUTING.md).

## References to oxygenupdater.com/api (server side)
The app communicates with server code, of which sources are available at https://github.com/arjanvlek/oxygen-updater-backend
If you want to run the app with this backend locally hosted (e.g. via Docker), please use the `localDebug` build variant.
