# Oxygen Updater (Android App) ![][badge-ci] [![latest release][badge-latest-release]][github-ou-releases]

Skip staged rollout queues and update your OnePlus device ASAP!

## Installation
These are the two places where we publish new versions; all others should not be trusted:
- [Google Play Store][play-store] (Android App Bundles)
- [GitHub releases page][github-ou-releases] (APKs)

**We highly recommend downloading & installing the app from Google Play Store**, as it has several benefits: much smaller download sizes, auto-updates (including in-app updates), Play Protect, etc.

<a href="https://play.google.com/store/apps/details?id=com.arjanvlek.oxygenupdater&utm_source=github&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1"><img alt="Get it on Google Play" src="https://oxygenupdater.com/img/google-play-store-badge-en.svg?v=1" width="200"/></a>

Note: installing via GitHub still supports auto-updates via the Play Store. Both releases are completely identical (except for format differences: AAB vs APK), since they're built on the exact same codebase & commit. Usually, we release on the Play Store first (100% rollout target), and a GitHub release follows shortly after.

## Development
| **Recommended**                    | **Alternatives**                                                                                    | Note for alternatives                                                                                                                          |
|------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| **Android Studio (latest stable)** | Any other IDE (IntelliJ IDEA, Eclipse, etc.) or even a text editor (Visual Studio Code, Atom, etc.) | Additional tools not included with your chosen IDE/editor might need to be installed separately (ADB, SDK Manager, platforms/sources, etc.)    |
| **Android device**                 | Android emulator                                                                                    | Emulators generally perform worse than a real device, even with accelerators. Also, you won't be able to test the app's root-related features. |

The app targets API 31 (Android 12/S), with a minimum API level of 21 (Android 5/Lollipop). Make sure both SDK platforms (and optionally sources) are installed via SDK Manager.

### Setup
This app uses Google & Firebase APIs, so `app/google-services.json` needs to exist. For development purposes, you'll need to create [your own Firebase project][firebase-config].

### KeyStore
Running non-`debug` variants of the app requires two things: generating a Java KeyStore and referencing it in `keystore.properties`.
Java KeyStores can be created in Android Studio itself: <kbd>Build</kbd> → <kbd>Generate Signed Bundle / APK</kbd> → <kbd>Next</kbd> → <kbd>Create new…</kbd> (under key store path). If you're not using Android Studio, check the instructions for your specific IDE. Otherwise you'll need to create one [via CLI][java8-keytool].

Store the JKS anywhere you want, though we'd recommend keeping it in the `app` directory. This git project has been configured to ignore all JKS files, so there's no danger of accidentally committing secrets.

Once that's done, create a `keystore.properties` file in this project's root directory and fill in the fields correctly:
```properties
storePassword=<store-password>
keyPassword=<key-password>
keyAlias=<alias>
storeFile=<jks-location>
```
Note: if you saved the JKS file in the `app` directory, `storeFile` doesn't need an absolute path (it's relative to `app`)

## License
This repository has no license. Default copyright laws apply, as mentioned in GitHub's ["Licensing a repository" page][github-licensing-info]:
> [...] without a license, the default copyright laws apply, meaning that you retain all rights to your source code and no one may reproduce, distribute, or create derivative works from your work. [...].

For legal purposes, the owner of this organization (Adhiraj Singh Chauhan) is to be considered as the owner of this project, and all its associated files and build outputs (APKs, AABs, etc.). Contributors of this project agree to transfer copyrights to the owner of this organization, with some exceptions:
- Code owners share ownership of that file, and are entitled to the same copyright laws as the owner of the organization.
  Code owners are usually marked with the `@author` annotation on a class/method/variable.
- If any source file within this repository has license information as part of the file, that license overrides.

This means that you are not permitted to redistribute and/or modify both the source code of this project, and the relevant build outputs without our explicit permission.
You can contact us on [our official Discord server][discord], or over [email][support-email].

You can still view/fork this repository, submit PRs, and/or raise issues. This is in accordance with GitHub's Terms of Service:
> [...] By setting your repositories to be viewed publicly, you agree to allow others to view and "fork" your repositories [...]

## Contributing
See [`CONTRIBUTING.md`][contributing].

[badge-ci]: https://github.com/oxygen-updater/oxygen-updater/workflows/Android%20CI/badge.svg
[badge-latest-release]: https://img.shields.io/badge/release-v5.9.1-%23f50514?logo=github

[contributing]: ./CONTRIBUTING.md

[android-studio]: https://developer.android.com/studio
[java8-keytool]: https://docs.oracle.com/javase/8/docs/technotes/tools/windows/keytool.html

[firebase-config]: https://support.google.com/firebase/answer/7015592#android

[github-ou-releases]: https://github.com/oxygen-updater/oxygen-updater/releases
[github-licensing-info]: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository#choosing-the-right-license
[github-tos]: https://docs.github.com/en/site-policy/github-terms/github-terms-of-service

[play-store]: https://play.google.com/store/apps/details?id=com.arjanvlek.oxygenupdater&utm_source=github
[discord]: https://discord.gg/5TXdhKJ
[support-email]: mailto:support@oxygenupdater.com
