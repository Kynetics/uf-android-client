#!/bin/bash

#Script used to download aosp and nxp platform keys.

BASE_PWD=$PWD

#Create & setup aosp directory

mkdir aosp && cd aosp
curl -O https://raw.githubusercontent.com/aosp-mirror/platform_build/android-11.0.0_r48/target/product/security/platform.x509.pem
curl -O https://raw.githubusercontent.com/aosp-mirror/platform_build/android-11.0.0_r48/target/product/security/platform.pk8

cd $BASE_PWD

#Create & setup nxp directory

mkdir nxp && cd nxp
curl -o platform.x509.pem "https://source.codeaurora.org/external/imx/android-imx/device/fsl/plain/common/security/platform.x509.pem?h=android-11.0.0_2.6.0"
curl -o platform.pk8 "https://source.codeaurora.org/external/imx/android-imx/device/fsl/plain/common/security/platform.pk8?h=android-11.0.0_2.6.0_2"

cd $BASE_PWD
