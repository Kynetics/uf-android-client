/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.api.v1


import org.junit.Assert
import org.junit.Test

class UFServiceMessageV1Test {

    val messages:List<UFServiceMessageV1> = listOf(
        UFServiceMessageV1.Event.Polling,
        UFServiceMessageV1.Event.StartDownloadFile("app.apk"),
        UFServiceMessageV1.Event.FileDownloaded("app.apk"),
        UFServiceMessageV1.Event.DownloadProgress("app.apk", .9),
        UFServiceMessageV1.Event.AllFilesDownloaded,
        UFServiceMessageV1.Event.UpdateFinished(false, listOf("Info about the update")),
        UFServiceMessageV1.Event.UpdateFinished(true),
        UFServiceMessageV1.Event.Error(),
        UFServiceMessageV1.Event.Error(listOf("Error message")),
        UFServiceMessageV1.Event.UpdateProgress("Phase name", "description", 0.2),
        UFServiceMessageV1.Event.UpdateAvailable("7"),
        UFServiceMessageV1.State.Downloading(listOf()),
        UFServiceMessageV1.State.Downloading(listOf(UFServiceMessageV1.State.Downloading.Artifact("app.apk", 7, "12"))),
        UFServiceMessageV1.State.Updating,
        UFServiceMessageV1.State.CancellingUpdate,
        UFServiceMessageV1.State.WaitingDownloadAuthorization,
        UFServiceMessageV1.State.WaitingUpdateAuthorization,
        UFServiceMessageV1.State.Idle,
        UFServiceMessageV1.State.ConfigurationError(),
        UFServiceMessageV1.State.ConfigurationError(listOf("Error message"))
    )


    val jsons:List<String> = """
        {"name":"POLLING","description":"Client is contacting server to retrieve new action to execute"}
        {"name":"START_DOWNLOAD_FILE","description":"A file downloading is started","fileName":"app.apk"}
        {"name":"FILE_DOWNLOADED","description":"A file is downloaded","fileDownloaded":"app.apk"}
        {"name":"DOWNLOAD_PROGRESS","description":"Percent of file downloaded","fileName":"app.apk","percentage":0.9}
        {"name":"ALL_FILES_DOWNLOADED","description":"All file needed are downloaded"}
        {"name":"UPDATE_FINISHED","description":"The update is finished","successApply":false,"details":["Info about the update"]}
        {"name":"UPDATE_FINISHED","description":"The update is finished","successApply":true,"details":[]}
        {"name":"ERROR","description":"An error is occurred","details":[]}
        {"name":"ERROR","description":"An error is occurred","details":["Error message"]}
        {"name":"UPDATE_PROGRESS","description":"Phase of update","phaseName":"Phase name","phaseDescription":"description","percentage":0.2}
        {"name":"UPDATE_AVAILABLE","description":"An update is available on cloud","id":"7"}
        {"name":"DOWNLOADING","description":"Client is downloading artifacts from server","artifacts":[]}
        {"name":"DOWNLOADING","description":"Client is downloading artifacts from server","artifacts":[{"name":"app.apk","size":7,"md5":"12"}]}
        {"name":"UPDATING","description":"The update process is started. Any request to cancel an update will be rejected"}
        {"name":"CANCELLING_UPDATE","description":"Last update request is being cancelled"}
        {"name":"WAITING_DOWNLOAD_AUTHORIZATION","description":"Waiting authorization to start download"}
        {"name":"WAITING_UPDATE_AUTHORIZATION","description":"Waiting authorization to start update"}
        {"name":"IDLE","description":"Client is waiting for new requests from server"}
        {"name":"CONFIGURATION_ERROR","description":"Bad service configuration","details":[]}
        {"name":"CONFIGURATION_ERROR","description":"Bad service configuration","details":["Error message"]}
""".trimIndent().lines()

    @Test
    @Throws(Exception::class)
    fun serializationTest() {
        (messages zip jsons).forEach {(message, expectedJson) ->
            Assert.assertEquals(expectedJson, message.toJson())
        }
    }

    @Test
    @Throws(Exception::class)
    fun parsingTest() {
        (jsons zip messages).forEach {(json, expectedMessage) ->
            Assert.assertEquals(expectedMessage, UFServiceMessageV1.fromJson(json))
        }
    }
}