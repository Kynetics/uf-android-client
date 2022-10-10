<h2 align="center">UFAndroidClient</h2>
<p align="center">
<a href="https://github.com/Kynetics/uf-android-client/actions/workflows/pipeline-build.yml"><img alt="Build Status" src="https://github.com/Kynetics/uf-android-client/actions/workflows/pipeline-build.yml/badge.svg"></a>
<a href="https://codeclimate.com/github/Kynetics/uf-android-client/maintainability"><img src="https://api.codeclimate.com/v1/badges/3dcb8f7ce1c2a6c9f9e2/maintainability" /></a>
<a href="http://www.eclipse.org/legal/epl-v10.html"><img alt="License" src="https://img.shields.io/badge/License-EPL%201.0-red.svg"></a>
<a href="https://jitpack.io/#kynetics/uf-android-client"><img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/kynetics/uf-android-client"></a>
</p>

The [Update Factory Android Client](https://docs.updatefactory.io/devices/android/android-client/) is an open-source project by [Kynetics](https://www.kynetics.com/) that provides an Android application that applies app (apk) and system (single copy or double copy OTA) software updates received from an [UpdateFactory](https://www.kynetics.com/iot-platform-update-factory) or [hawkBit](https://eclipse.org/hawkbit/) server.

Links to official documentation:
- [overview](https://docs.updatefactory.io/devices/android/android-client/)
- [installation](https://docs.updatefactory.io/devices/android/android-client-packages/)
- [configuration file](https://docs.updatefactory.io/devices/android/android-config-files/)
- [third-party integration](https://docs.updatefactory.io/devices/android/third-party-integration-v1_1/)
- [troubleshooting](https://docs.updatefactory.io/devices/android/android-troubleshooting/)
- [kdocs API](https://kynetics.github.io/uf-android-client/)

## uf-client-service
uf-client-service is an android service that run in background and manage the updates.

uf-client-service must be install as **SYSTEM** application.

### State diagrams
#### Main
![UF STM Main](https://drive.google.com/uc?export=view&id=1FXw8Au_NmpAaJhVkhzwV9ED1-nfdtMXG)
#### Update
![UF STM Update](https://drive.google.com/uc?export=view&id=1OMwLV1RwluYuMvEukEcDgLAbb9OjEVMa)

## Modules
- **uf-client-service**: service implementation that apply the updates received by the update factory server
- **uf-client-service-api**: service api used by the third-party apps to communicate with the *uf-client-service*
- **os-mock**: mock implementation of the android hidden api used by the *uf-client-service*
- **uf-ddiclient**: hara-ddiclient customization for Android and Update Factory
- **uf-client-ui-example**: an example of application that use the uf-client-service via *uf-client-service-api*

## Third-Party Libraries
* [hara-ddiclient](https://github.com/eclipse/hara-ddiclient) - [Eclipse Public License 2.0](https://github.com/eclipse/hara-ddiclient/blob/master/LICENSE)
* [slf4j-android-logger](https://github.com/PSDev/slf4j-android-logger) - [Apache License 2.0](https://github.com/PSDev/slf4j-android-logger/blob/master/LICENSE.txt)
* [gson](https://github.com/google/gson) - [Apache License 2.0](https://github.com/google/gson/blob/master/LICENSE)
* [Retrofit](https://github.com/square/retrofit) - [Apache License 2.0](https://github.com/square/retrofit/blob/master/LICENSE.txt)
* [OkHttp](https://github.com/square/okhttp) - [Apache License 2.0](https://github.com/square/okhttp/blob/master/LICENSE.txt)
* [cron-utils](https://github.com/jmrozanec/cron-utils) - [Apache License 2.0](https://github.com/jmrozanec/cron-utils/blob/master/LICENSE)

## Authors
* [Daniele Sergio](https://github.com/danielesergio) - *Initial work*
* [Andrea Zoleo](https://github.com/andrea-zoleo) 
* [Diego Rondini](https://github.com/diegorondini)
* [Alberto Battiston](https://github.com/albertob13)

See also the list of [contributors](https://github.com/Kynetics/UfAndroidClient/graphs/contributors) who participated in this project.

## License
Copyright © 2017-2022, [Kynetics LLC](https://www.kynetics.com).
Released under the [EPLv1 License](http://www.eclipse.org/legal/epl-v10.html).
