# Oxygen Updater (Android App) #

This repository contains all the code of the Oxygen Updater Android application.

## References to oxygenupdater.com/api (server side)
The app communicates with server code, which is currently not open source.

## Version formatter library
The app contains a shared library to format version info (which is also used by private, closed-source components).
The version formatter is open sourced and available at https://bitbucket.org/arjan1995/oxygen-updater-update-data-version-formatter

## Private repository references
Currently, the app obtains the version formatter from a private repository. 
However, the version formatter can also be built manually and placed in the `lib` folder within the `app` folder.
If you do so, the references to the private repository may be omitted from the .gradle files.

## Using the private repository (only possible if having an account on it)
The following properties must be set in `~/.gradle/gradle.properties` or `C:\Users\<User>\.gradle\gradle.properties` to allow downloading from the private repository:

```
nexus_username=<repository username>
nexus_password=<repository password>
```

