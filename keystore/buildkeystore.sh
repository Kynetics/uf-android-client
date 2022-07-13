#!/bin/bash

#Script used for generating public.jks keystore containing aosp and nxp platform keys.

BASE_PWD=$PWD

for d in */ ; do
	KEY_NAME=$(basename $d)
	cd $d
	rm -f ./$KEY_NAME.p12
	openssl pkcs8 -inform DER -nocrypt -in "platform.pk8" | openssl pkcs12 -export -in "platform.x509.pem" -inkey /dev/stdin -name "platform_$KEY_NAME" -password pass:$KEY_NAME -out "$KEY_NAME.p12"
	keytool  -importkeystore  -deststorepass keystore -srckeystore "$KEY_NAME.p12" -srcstoretype PKCS12 -srcstorepass $KEY_NAME -destkeystore "$BASE_PWD/public.jks" -alias "platform_$KEY_NAME"
	cd $BASE_PWD
done
