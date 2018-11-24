# Oxygen Updater (Android App)

This repository contains all the code of the Oxygen Updater Android application.

## Building and running
The application should be buildable with Android Studio 3.2.1 and later.
The app targets Android Pie (api 29) and the minimum is Android Lollipop (API 21). 
The app can be run within the emulator but is limited to no-root features only.
To test automatic update installations and other root features, you'll need to run it on a rooted device.

## Contributing
See `CONTRIBUTING.md`.

## References to oxygenupdater.com/api (server side)
The app communicates with server code, of which sources are available at https://github.com/arjanvlek/oxygen-updater-backend
If you want to run the app with this backend locally hosted (e.g. via Docker), please use the `localDebug` build variant.
