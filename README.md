<h2 align="center">UFAndroidClient</h2>
<p align="center">
<a href="https://github.com/Kynetics/uf-android-client/actions/workflows/pipeline-build.yml"><img alt="Build Status" src="https://github.com/Kynetics/uf-android-client/actions/workflows/pipeline-build.yml/badge.svg"></a>
<a href="https://sonarcloud.io/summary/new_code?id=Kynetics_uf-android-client"><img alt="Quality Gate Status" src="https://sonarcloud.io/api/project_badges/measure?project=Kynetics_uf-android-client&metric=alert_status" /></a>
<a href="https://www.eclipse.org/legal/epl-2.0"><img alt="License" src="https://img.shields.io/badge/License-EPL%202.0-red.svg"></a>
<a href="https://jitpack.io/#kynetics/uf-android-client"><img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/kynetics/uf-android-client"></a>
</p>

The [Update Factory Android Client](https://docs.updatefactory.io/devices/android/android-client/) is an open-source project by [Kynetics](https://www.kynetics.com/) that provides an Android application that applies app (apk) and system (single copy or double copy OTA) software updates received from an [UpdateFactory](https://www.kynetics.com/iot-platform-update-factory) or [hawkBit](https://eclipse.org/hawkbit/) server.

Links to official documentation:
- [Overview](https://docs.updatefactory.io/devices/android/android-client/)
- [Installation](https://docs.updatefactory.io/devices/android/android-client-packages/)
- [Configuration file](https://docs.updatefactory.io/devices/android/android-config-files/)
- [Third-party integration](https://docs.updatefactory.io/devices/android/third-party-integration-latest/)
- [Troubleshooting](https://docs.updatefactory.io/devices/android/android-troubleshooting/)
- [API documentation](https://kynetics.github.io/uf-android-client/)

## uf-client-service
uf-client-service is an Android service that runs in the background and manages the updates.

uf-client-service must be install as **SYSTEM** application.

### State diagrams
#### Main
![UF STM Main](https://drive.google.com/uc?export=view&id=1FXw8Au_NmpAaJhVkhzwV9ED1-nfdtMXG)
#### Update
![UF STM Update](https://drive.google.com/uc?export=view&id=1OMwLV1RwluYuMvEukEcDgLAbb9OjEVMa)

## Modules
- **uf-client-service**: service implementation that applies the updates received by the Update Factory server
- **uf-client-service-api**: service API used by the third-party applications to communicate with the *uf-client-service*
- **os-mock**: mock implementation of the Android hidden api used by the *uf-client-service*
- **uf-ddiclient**: customization of hara-ddiclient library for Android and Update Factory

## Third-Party Libraries
* [hara-ddiclient](https://github.com/eclipse-hara/hara-ddiclient) - [Eclipse Public License 2.0](https://github.com/eclipse-hara/hara-ddiclient/blob/master/LICENSE)
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
* [Saeed Rezaee](https://github.com/SaeedRe)

See also the list of [contributors](https://github.com/Kynetics/UfAndroidClient/graphs/contributors) who participated in this project.

## License
Copyright © 2017-2024, [Kynetics LLC](https://www.kynetics.com).

Released under the [EPLv2 License](https://www.eclipse.org/legal/epl-2.0).
