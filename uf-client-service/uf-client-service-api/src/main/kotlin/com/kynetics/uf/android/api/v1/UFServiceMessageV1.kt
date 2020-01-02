/*
 * Copyright © 2017-2020  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.api.v1

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
@Suppress("MaxLineLength")
sealed class UFServiceMessageV1 {
    enum class MessageName {
        DOWNLOADING,
        ERROR,
        UPDATING,
        CANCELLING_UPDATE,
        WAITING_DOWNLOAD_AUTHORIZATION,
        WAITING_UPDATE_AUTHORIZATION,
        IDLE,
        START_DOWNLOAD_FILE,
        DOWNLOAD_PROGRESS,
        UPDATE_PROGRESS,
        FILE_DOWNLOADED,
        UPDATE_FINISHED,
        POLLING,
        ALL_FILES_DOWNLOADED,
        UPDATE_AVAILABLE,
        CONFIGURATION_ERROR
    }

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    abstract val description: String
    abstract val name: MessageName
    @Serializable
    sealed class State(override val name: MessageName, override val description: String) : UFServiceMessageV1() {
        @Serializable
        data class Downloading(val artifacts: List<Artifact>) : State(MessageName.DOWNLOADING, "Client is downloading artifacts from server") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }

            @Serializable
            data class Artifact(val name: String, val size: Long, val md5: String)
        }
        object Updating : State(MessageName.UPDATING, "The update process is started. Any request to cancel an update will be rejected")
        object CancellingUpdate : State(MessageName.CANCELLING_UPDATE, "Last update request is being cancelled")
        object WaitingDownloadAuthorization : State(MessageName.WAITING_DOWNLOAD_AUTHORIZATION, "Waiting authorization to start download")
        object WaitingUpdateAuthorization : State(MessageName.WAITING_UPDATE_AUTHORIZATION, "Waiting authorization to start update")
        object Idle : State(MessageName.IDLE, "Client is waiting for new requests from server")
        @Serializable
        data class ConfigurationError(val details: List<String> = emptyList()) : State(MessageName.CONFIGURATION_ERROR, "Bad service configuration") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        override fun toJson(): String {
            return json.stringify(serializer(), this)
        }
    }

    @Serializable
    sealed class Event(override val name: MessageName, override val description: String) : UFServiceMessageV1() {
        object Polling : Event(MessageName.POLLING, "Client is contacting server to retrieve new action to execute")
        @Serializable
        data class StartDownloadFile(val fileName: String) : Event(MessageName.START_DOWNLOAD_FILE, "A file downloading is started") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }
        }
        @Serializable
        data class FileDownloaded(val fileDownloaded: String) : Event(MessageName.FILE_DOWNLOADED, "A file is downloaded") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }
        }
        @Serializable
        data class DownloadProgress(val fileName: String, val percentage: Double = 0.0) : Event(MessageName.DOWNLOAD_PROGRESS, "Percent of file downloaded") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }
        }

        object AllFilesDownloaded : Event(MessageName.ALL_FILES_DOWNLOADED, "All file needed are downloaded")
        @Serializable
        data class UpdateFinished(val successApply: Boolean, val details: List<String> = emptyList()) : Event(MessageName.UPDATE_FINISHED, "The update is finished") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }
        }

        @Serializable
        data class Error(val details: List<String> = emptyList()) : Event(MessageName.ERROR, "An error is occurred") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }
        }

        @Serializable
        data class UpdateProgress(val phaseName: String, val phaseDescription: String = "", val percentage: Double = 0.0) : Event(MessageName.UPDATE_PROGRESS, "Phase of update") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }
        }

        @Serializable
        data class UpdateAvailable(val id: String) : Event(MessageName.UPDATE_AVAILABLE, "An update is available on cloud") {
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson(): String {
                return json.stringify(serializer(), this)
            }
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        override fun toJson(): String {
            println(json.stringify(serializer(), this))
            return json.stringify(serializer(), this)
        }
    }

    abstract fun toJson(): String

    companion object {

        private val json = Json(JsonConfiguration.Stable.copy(strictMode = false))

        @UseExperimental(ImplicitReflectionSerializer::class)
        @Suppress("ComplexMethod")
        fun fromJson(jsonContent: String): UFServiceMessageV1 {
            val jsonElement = json.parseJson(jsonContent)
            return when (jsonElement.jsonObject["name"]?.primitive?.content) {

                MessageName.DOWNLOADING.name -> json.fromJson<State.Downloading>(jsonElement)
                MessageName.UPDATING.name -> State.Updating
                MessageName.CANCELLING_UPDATE.name -> State.CancellingUpdate
                MessageName.WAITING_DOWNLOAD_AUTHORIZATION.name -> State.WaitingDownloadAuthorization
                MessageName.WAITING_UPDATE_AUTHORIZATION.name -> State.WaitingUpdateAuthorization
                MessageName.IDLE.name -> State.Idle

                MessageName.ERROR.name -> json.fromJson<Event.Error>(jsonElement)
                MessageName.START_DOWNLOAD_FILE.name -> json.fromJson<Event.StartDownloadFile>(jsonElement)
                MessageName.UPDATE_PROGRESS.name -> json.fromJson<Event.UpdateProgress>(jsonElement)/**/
                MessageName.DOWNLOAD_PROGRESS.name -> json.fromJson<Event.DownloadProgress>(jsonElement)
                MessageName.FILE_DOWNLOADED.name -> json.fromJson<Event.FileDownloaded>(jsonElement)
                MessageName.UPDATE_FINISHED.name -> json.fromJson<Event.UpdateFinished>(jsonElement)
                MessageName.POLLING.name -> Event.Polling
                MessageName.ALL_FILES_DOWNLOADED.name -> Event.AllFilesDownloaded
                MessageName.UPDATE_AVAILABLE.name -> json.fromJson<Event.UpdateAvailable>(jsonElement)
                MessageName.CONFIGURATION_ERROR.name -> json.fromJson<State.ConfigurationError>(jsonElement)

                else -> throw IllegalArgumentException("$jsonContent is not obtained by toJson method of ${UFServiceMessageV1::class.java.simpleName}")
            }
        }
    }
}
