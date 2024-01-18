#!/bin/bash

#
# Copyright © 2017-2024  Kynetics  LLC
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

#Script used for generating public.jks keystore containing publicly available platform keys.

BASE_PWD=$PWD

for d in */ ; do
	KEY_NAME=$(basename $d)
	cd $d
	rm -f ./$KEY_NAME.p12
	openssl pkcs8 -inform DER -nocrypt -in "platform.pk8" | openssl pkcs12 -export -in "platform.x509.pem" -inkey /dev/stdin -name "platform_$KEY_NAME" -password pass:$KEY_NAME -out "$KEY_NAME.p12"
	keytool  -importkeystore  -deststorepass keystore -srckeystore "$KEY_NAME.p12" -srcstoretype PKCS12 -srcstorepass $KEY_NAME -destkeystore "$BASE_PWD/public.jks" -alias "platform_$KEY_NAME"
	cd $BASE_PWD
done
