/*
 *
 *  Copyright © 2017-2019  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.uf.android.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.kynetics.updatefactory.ddiclient.core.api.Updater;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static android.content.pm.PackageInstaller.EXTRA_PACKAGE_NAME;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PackageInstallerBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = PackageInstallerBroadcastReceiver.class.getSimpleName();

    private static final int SESSION_ID_NOT_FOUND = -1;
    private final int sessionId;
    private final CountDownLatch countDownLatch;
    private final CurrentUpdateState currentUpdateState;
    private final Updater.SwModuleWithPath.Artifact artifact;
    private final Updater.Messenger messenger;
    private final Long packageVersion;
    private final String packageName;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!InstallerSession.ACTION_INSTALL_COMPLETE.equals(intent.getAction())) {
            return;
        }

        final int sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, SESSION_ID_NOT_FOUND);
        if(sessionId != this.sessionId){
            return;
        }

        final int result = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, SESSION_ID_NOT_FOUND);

        final String currentPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME);

        switch (result){
            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_ABORTED:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                final String errorMessage = String.format("Installation of %s (%s) fails with error code %s", artifact.getFilename(), packageName, result);
                currentUpdateState.addErrorToRepor(errorMessage);
                messenger.sendMessageToServer(errorMessage);
                currentUpdateState.packageInstallationTerminated(packageName, packageVersion);
                countDownLatch.countDown();
                context.unregisterReceiver(this);
                break;

            case PackageInstaller.STATUS_SUCCESS:
                final String message = String.format("%s (%s) installed", artifact.getFilename(), packageName);
                currentUpdateState.addSuccessMessageToRepor(message);
                messenger.sendMessageToServer(message);
                currentUpdateState.packageInstallationTerminated(packageName, packageVersion);
                countDownLatch.countDown();
                context.unregisterReceiver(this);
                break;
            default:
                Log.w(TAG, String.format("Status (%s) of package installation (%s) not handle",
                        result,
                        packageName));
                break;
        }

        Log.d(TAG, String.format("Result code of %s installation: %s", packageName,result));
    }


    PackageInstallerBroadcastReceiver(int sessionId,
                                      CountDownLatch countDownLatch,
                                      Updater.SwModuleWithPath.Artifact artifact,
                                      CurrentUpdateState currentUpdateState,
                                      Updater.Messenger messenger,
                                      String packageName,
                                      Long packageVersion) {
        this.sessionId = sessionId;
        this.countDownLatch = countDownLatch;
        this.currentUpdateState = currentUpdateState;
        this.artifact = artifact;
        this.messenger = messenger;
        this.packageVersion = packageVersion;
        this.packageName = packageName;
    }
}
