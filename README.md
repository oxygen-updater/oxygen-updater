# Oxygen Updater (Android App)

![][ci-badge]

This repository contains all the code of the Oxygen Updater Android application.

## Building and running
### Required tools
The application should be buildable with Android Studio v3.4.2 and higher. However, since we use Kotlin + Kotlin build scripts, you might benefit by using Android Studio v4.0.0 and higher.
The app targets Android 10/Q (API 29) and the minimum is Android Lollipop (API 21). Make sure you've got both platforms installed!

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
See [`CONTRIBUTING.md`][contributing].

## References to oxygenupdater.com/api (server side)
The app communicates with server code, of which sources are available at https://github.com/oxygen-updater/oxygen-updater-backend
If you want to run the app with this backend locally hosted (e.g. via Docker), please use the `localDebug` build variant.

## License
This repository has no license. Default copyright laws apply, as mentioned in GitHub's ["Licensing a repository" page][github-licensing-info]:
> [...] Without a license, the default copyright laws apply, meaning that you retain all rights to your source code and no one may reproduce, distribute, or create derivative works from your work. [...].

For legal purposes, the owner of this organization (Adhiraj Singh Chauhan) is to be considered as the owner of this project, and all its associated files and build outputs (APKs, AABs, etc.). Contributors of this project agree to transfer copyrights to the owner of this organization, with some exceptions:
- Code owners share ownership of that file, and are entitled to the same copyright laws as the owner of the organization.  
  Code owners are usually marked with the `@author` annotation on a class/method/variable.
- If any source file within this repository has license information as part of the file, that license overrides.

This means that you are not permitted to redistribute and/or modify both the source code of this project, and the relevant build outputs without our explicit permission.
You can contact us on [our official Discord server][discord], or over [email][support-email].

You can still view/fork this repository, submit PRs, and/or raise issues. This is in accordance of GitHub's Terms of Service:
> [...] If you publish your source code in a public repository on GitHub, according to the [Terms of Service][github-tos], other GitHub users have the right to view and fork your repository within the GitHub site. [...]

[ci-badge]: https://github.com/oxygen-updater/oxygen-updater/workflows/Android%20CI/badge.svg
[contributing]: ./CONTRIBUTING.md
[github-licensing-info]: https://help.github.com/en/github/creating-cloning-and-archiving-repositories/licensing-a-repository#choosing-the-right-license
[discord]: https://discord.gg/5TXdhKJ
[support-email]: mailto:support@oxygenupdater.com?subject=Legal%20request%20for%20Oxygen%20Updater
[github-tos]: https://help.github.com/en/articles/github-terms-of-service
