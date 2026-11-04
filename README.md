<!--
SPDX-FileCopyrightText: The LineageOS Project
SPDX-License-Identifier: Apache-2.0
-->

Updater
=======
ExpressiveOS's system update app, based on LineageOS's Updater. It checks the
ExpressiveOS OTA feed for new builds, downloads them and installs them over the
air.


Where it looks for updates
--------------------------
The app sends a `GET` request to the URL in the `updater_server_url` resource,
which a device tree can overlay. By default that is the ExpressiveOS OTA feed:

```
https://raw.githubusercontent.com/denson9874/expressiveos_ota/main/builds/{device}.json
```

`{device}` is replaced at runtime with the device codename, read from
`ro.updater.next_device` or, when that is empty, `ro.evolution.device`.

To point a build at another feed, set the `evolution.updater.uri` system
property. When it is set, it is used instead of `updater_server_url`, and
`{device}` is replaced in it the same way. The URL must use `https://`, and
redirects are not followed.

The notes shown for an update are the plain text file named by the
`menu_changelog_url` resource, by default `changelogs/<device>.txt` in the same
repository as the feed. When it can't be fetched, the app falls back to a
`Changelog.txt` in `/system/etc`, `/product/etc` or `/system_ext/etc`.


Feed format
-----------
The server must answer with a JSON with the following structure:
```json
{
  "response": [
    {
      "buildtype": "user",
      "download": "https://example.com/expressiveos/cheetah/ota/ExpressiveOS-17.0-20261203-cheetah-1.1-Official.zip",
      "filename": "ExpressiveOS-17.0-20261203-cheetah-1.1-Official.zip",
      "firmware": "https://developers.google.com/android/images#cheetah",
      "forum": "https://example.com/forum/cheetah",
      "maintainer": "Name",
      "os_patch_level": "2026-11-01",
      "paypal": "https://example.com/donate",
      "sha256": "fd2e25df5a8f49a87e82ca052f99ffac5187b145c1867ad0c02f500b575eedaf",
      "size": 3436884325,
      "timestamp": 1796263200,
      "version": "1.1"
    }
  ]
}
```

`{"response": []}` means that no build is published, and the app reports that
there are no updates.

ExpressiveOS packages are named
`ExpressiveOS-<Android version>-<build date>-<device>-<ExpressiveOS version>-<release type>.zip`,
for example `ExpressiveOS-17.0-20261203-cheetah-1.1-Official.zip`: Android 17.0,
built on 2026-12-03, for cheetah (Pixel 7 Pro), ExpressiveOS 1.1, an Official
release. The app doesn't parse the name; it saves the download under it.

The `base_timestamp` optional attribute makes the OTA update an incremental one: it is offered only to the build whose `timestamp` it names.  
The `buildtype` optional attribute is the build type of the OTA update.  
The `download` attribute is the URL of the file to be downloaded.  
The `filename` attribute is the name of the file to be downloaded.  
The `firmware` optional attribute is a link to the firmware for the device.  
The `forum` optional attribute is a link to the support thread for the device.  
The `maintainer` optional attribute is the name of the device maintainer.  
The `os_patch_level` optional attribute is the security patch level of the OTA update.  
The `os_sdk_level` optional attribute is the SDK level of the OTA update. It defaults to the running one.  
The `ota_property_files` optional attribute is the `ota-property-files` value from `META-INF/com/android/metadata` of the OTA update.  
The `paypal` optional attribute is a donation link for the device maintainer.  
The `sha256` attribute is a sha256 of the OTA update.  
The `size` attribute is the size of the update expressed in bytes.  
The `timestamp` attribute is the build date expressed as UNIX timestamp.  
The `version` attribute is the ExpressiveOS version of the OTA update, such as `1.1`.  

Additional attributes are ignored, such as the `device`, `oem`, `md5`,
`currently_maintained` and `github` fields that the ExpressiveOS feed also
carries.

An update is offered only when its `timestamp` is newer than the running
build's `ro.build.date.utc` and its SDK level isn't lower than the running one.
Setting `evolution.updater.allow_downgrading` to `true` lifts the first check,
and an update for a newer Android SDK level can only be installed when
`evolution.updater.allow_major_upgrades` is `true`.


Testing a local package
-----------------------
`push-update.sh` copies an OTA package to a connected device over adb and adds
it to the app's list of updates, so it can be installed without publishing it:

```
./push-update.sh ExpressiveOS-17.0-20261203-cheetah-1.1-Official.zip [UNVERIFIED] [SERIAL]
```

It reads the ExpressiveOS version and the release type from the file name, so
the package must keep the name the build gave it. The device must allow
`adb root`.


Build with Android Studio
-------------------------
Updater needs access to the system API, therefore it can't be built only using
the public SDK. You first need to generate the libraries with all the needed
classes. The application also needs elevated privileges, so you need to sign
it with the right key to update the one in the system partition. To do this:

 - Place this directory anywhere in the Android source tree
 - Generate a keystore and keystore.properties using `gen-keystore.sh`
 - Build the platform artifacts that provide the non-public classes used by
   the app. At minimum, the jars copied by `pull-system-libs.sh` must exist
   under `out/soong/.intermediates/`
 - Run `./pull-system-libs.sh` from this directory. By default it reads from
   `../../../out` relative to this repository path and will populate
   `system_libs/` with the jars Gradle expects:
   - `framework.jar`
   - `SettingsLib.jar`
   - `SpaLib.jar`
 - If your build output lives somewhere else, pass it explicitly:
   `./pull-system-libs.sh /path/to/out`

You need to do the above once, unless Android Studio can't find some symbol.
In that case, rebuild the relevant platform targets and rerun
`./pull-system-libs.sh`.
