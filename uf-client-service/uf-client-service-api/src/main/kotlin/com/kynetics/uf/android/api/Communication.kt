/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android.api

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
 import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Transform an instance of [Message] to [Communication.V1.Out]
 *
 * @receiver [Message] to be converted into a [Communication.V1.Out]
 * @throws IllegalArgumentException if the message can't be transformed to an
 *   [Communication.V1.Out] instance.
 */
@Suppress("DEPRECATION")
fun Message.toOutV1Message(): Communication.V1.Out {
    return runCatching {
        when (this.what) {
            Communication.V1.Out.ServiceNotification.ID ->
                Communication.V1.Out.ServiceNotification(UFServiceMessageV1.fromJson(data.getString(
                    Communication.V1.SERVICE_DATA_KEY)!!))

            Communication.V1.Out.CurrentServiceConfiguration.ID ->
                Communication.V1.Out.CurrentServiceConfiguration(data.getSerializable(Communication.V1.SERVICE_DATA_KEY
                ) as UFServiceConfiguration)

            Communication.V1.Out.AuthorizationRequest.ID ->
                Communication.V1.Out.AuthorizationRequest(data.getString(
                    Communication.V1.SERVICE_DATA_KEY
                )!!)

            Communication.V1.Out.CurrentServiceConfigurationV2.ID -> Communication.V1.Out.CurrentServiceConfigurationV2(
                UFServiceConfigurationV2.fromJson(data.getString(Communication.V1.SERVICE_DATA_KEY)!!)
            )

            else -> throw IllegalArgumentException("This message isn't sent by UF client (with api v1)")
        }
    }.onFailure {
        throw IllegalArgumentException("This message isn't sent by UF client (with api v1)", it)
    }.getOrThrow()
}

/**
 * Class that maps all messages that are exchanged with the [com.kynetics.uf.android.UpdateFactoryService]
 */
@Suppress("KDocUnresolvedReference")
sealed class Communication(val id: Int) {

    companion object {
        /**
         * [android.os.Bundle] key where the ApiCommunicationVersion of the message is stored
         *
         * @property id message code so that the recipient can identify what this message is about
         *  ([android.os.Message.what])
         */
        const val SERVICE_API_VERSION_KEY = "API_VERSION_KEY"
    }

    /**
     * Communication messages available in V1.x communication api
     */
    sealed class V1(id: Int) : Communication(id) {

        companion object {
            /**
             * [android.os.Bundle] key where the additional info are stored
             */
            const val SERVICE_DATA_KEY = "DATA_KEY"
        }

        /**
         * Class that maps all the messages that are sent to com.kynetics.uf.android.UpdateFactoryService
         *
         * @property id message code so that the recipient can identify what this message is about
         *  ([android.os.Message.what])
         */
        sealed class In(
            /**
             * message code so that the recipient can identify what this message is about
             */
            id: Int
        ) : V1(id) {

            /**
             * Convert the object to the corresponding [android.os.Message] instance.
             */
            open fun toMessage(): Message {
                val msg = Message.obtain(null, id)
                val bundleWithApiVersion = bundle()
                bundleWithApiVersion.putInt(SERVICE_API_VERSION_KEY, ApiCommunicationVersion.latest().versionCode)
                msg.data = bundleWithApiVersion
                return msg
            }

            /**
             * Class that maps all messages that are sent to [com.kynetics.uf.android.UpdateFactoryService]
             * that must receive a response.
             *
             * @property replyTo [Messenger] where replies to this message are sent
             * @property id message code so that the recipient can identify what this message is about
             *  ([android.os.Message.what])
             */
            abstract class WithReplyTo(
                /**
                 * [Messenger] where replies to this message are sent
                 */
                val replyTo: Messenger,
                /**
                 * message code so that the recipient can identify what this message is about
                 */
                id: Int
            ) : In(id) {
                override fun toMessage(): Message {
                    val msg = super.toMessage()
                    msg.replyTo = replyTo
                    return msg
                }
            }

            /**
             * @suppress
             */
            internal open fun bundle(): Bundle = Bundle()

            /**
             * Class to build a message to configure the service.
             * When the new configuration can be applied live, a [UFServiceMessageV1.Event.ConfigurationUpdated] is notified.
             * When the new configuration requires a restart of the service, a [UFServiceMessageV1.Event.Stopped] and a
             * [UFServiceMessageV1.Event.Started] are notified.
             *
             *  @property conf the service configuration
             *  @see UFServiceConfiguration
             */
            @Deprecated("As of release 1.3.0 replaced by com.kynetics.uf.android.api.Communication.V1.In.ConfigureServiceV2")
            @Suppress("DEPRECATION")
            class ConfigureService(
                /**
                 * the service configuration
                 */
                @Suppress("MemberVisibilityCanBePrivate") val conf: UFServiceConfiguration) : In(ID) {
                companion object {
                    const val ID = 1
                }

                /**
                 * @suppress
                 */
                override fun bundle(): Bundle {
                    return super.bundle().apply {
                        putSerializable(SERVICE_DATA_KEY, conf)
                    }
                }
            }

            /**
             * Class to build a message to configure the service.
             * When the new configuration can be applied live, a [UFServiceMessageV1.Event.ConfigurationUpdated] is notified.
             * When the new configuration requires a restart of the service, a [UFServiceMessageV1.Event.Stopped] and a
             * [UFServiceMessageV1.Event.Started] are notified.
             *
             *  @property conf the service configuration
             *  @see UFServiceConfigurationV2
             */
            class ConfigureServiceV2(
                @Suppress("MemberVisibilityCanBePrivate")
                /**
                 * the service configuration
                 */
                val conf: UFServiceConfigurationV2) : In(ID) {
                companion object {
                    const val ID = 10
                }

                /**
                 * @suppress
                 */
                override fun bundle(): Bundle {
                    return super.bundle().apply {
                        putSerializable(SERVICE_DATA_KEY, conf.toJson())
                    }
                }
            }

            /**
             * Class to build a message to subscribe a [Messenger] to the service notification
             * system.
             *
             * @property replyTo the [Messenger] to subscribe to the service notification system
             */
            class RegisterClient(
                /**
                 * [Messenger] where replies to this message are sent
                 */
                replyTo: Messenger
            ) : WithReplyTo(replyTo, ID) {
                companion object {
                    const val ID = 2
                }
            }

            /**
             * Class to build a message to unsubscribe a [Messenger] from the service notification
             * system.
             *
             * @property replyTo the [Messenger] to unsubscribe from the service notification system
             */
            class UnregisterClient(
                /**
                 * [Messenger] where replies to this message are sent
                 */
                replyTo: Messenger
            ) : WithReplyTo(replyTo, ID) {
                companion object {
                    const val ID = 3
                }
            }

            /**
             * Class to build a message to grant / deny an authorization
             *
             * @property granted, true to grant the authorization, false otherwise
             */
            class AuthorizationResponse(
                @Suppress("MemberVisibilityCanBePrivate")
                /**
                 * true to grant the authorization, false otherwise
                 */
                val granted: Boolean
            ) : In(ID) {
                companion object {
                    const val ID = 6
                }

                /**
                 * @suppress
                 */
                override fun bundle(): Bundle {
                    return super.bundle().apply {
                        putBoolean(SERVICE_DATA_KEY, granted)
                    }
                }
            }

            /**
             * Class to build a sync message.
             * When the service receives a sync message it replies with two messages,
             * the first message contains the service state and the second message contains the
             * service configuration
             *
             * @property replyTo the [Messenger] to unsubscribe from the service notification system
             * @see Communication.V1.Out.ServiceNotification
             * @see Communication.V1.Out.CurrentServiceConfiguration
             *
             */
            class Sync(
                /**
                 * [Messenger] to use for replies to this message
                 */
                replyTo: Messenger
            ) : WithReplyTo(replyTo, ID) {
                companion object {
                    const val ID = 8
                }
            }

            /**
             * Class to build ForcePing message.
             * When the service receives a force ping message it polls the server
             */
            object ForcePing : In(7)

            /**
             * Class to build a message to add target attributes
             *
             * @property targetAttributesWithPolicy, a Map of target attributes with an aggregation policy
             */
            class AddTargetAttributes(
                @Suppress("MemberVisibilityCanBePrivate")
                /**
                 * @property targetAttributesWithPolicy, the target attributes with the aggregation policy
                 */
                val targetAttributesWithPolicy: TargetAttributesWithPolicy
            ) : In(ID) {
                companion object {
                    const val ID = 12
                }

                /**
                 * @suppress
                 */
                override fun bundle(): Bundle {
                    return super.bundle().apply {
                        putSerializable(SERVICE_DATA_KEY,targetAttributesWithPolicy.toJson())
                    }
                }
            }

        }


        /**
         * Class that maps all messages that the [com.kynetics.uf.android.UpdateFactoryService]
         * sends to the clients
         *
         * @property id message code so that the recipient can identify what this message is about
         *  ([android.os.Message.what])
         */
        sealed class Out(
            /**
             * message code so that the recipient can identify what this message is about
             */
            id: Int
        ) : V1(id) {
            /**
             * This class represents a message that the [com.kynetics.uf.android.UpdateFactoryService]
             * sends to clients with the information about its state. This message is sent after each
             * polling request or as response of a [Communication.V1.In.Sync] message.
             *
             * @property content is the representation of the current service state
             * @see UFServiceMessageV1
             */
            class ServiceNotification(
                /**
                 * The notification content
                 */
                val content: UFServiceMessageV1) : Out(ID) {
                /**
                 * @suppress
                 */
                companion object {
                    const val ID = 4
                }
            }

            /**
             * This class represents a message that the [com.kynetics.uf.android.UpdateFactoryService]
             * sends to clients when it is waiting for an user authorization
             *
             * @property authName is the kind of authorization, it is one between *DOWNLOAD* and *UPDATE*
             */
            class AuthorizationRequest(
                /**
                 *  the kind of authorization, it is one between *DOWNLOAD* and *UPDATE*
                 */
                val authName: String) : Out(ID) {
                /**
                 * @suppress
                 */
                companion object {
                    const val ID = 5
                }
            }

            /**
             * This class represents a message that the [com.kynetics.uf.android.UpdateFactoryService]
             * sends to the client as response of a [Communication.V1.In.Sync] message.
             * @property conf is the service configuration
             * @see UFServiceConfiguration
             */
            @Deprecated("As of release 1.3.0 replaced by com.kynetics.uf.android.api.Communication.V1.Out.CurrentServiceConfigurationV2")
            @Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")
            class CurrentServiceConfiguration(
                /**
                 * the service configuration
                 */
                val conf: UFServiceConfiguration
            ) : Out(ID) {
                /**
                 * @suppress
                 */
                companion object {
                    const val ID = 9
                }
            }

            /**
             * This class represents a message that the [com.kynetics.uf.android.UpdateFactoryService]
             * sends to the client as response of a [Communication.V1.In.Sync] message.
             * @property conf is the service configuration
             * @see UFServiceConfigurationV2
             */
            class CurrentServiceConfigurationV2(
                @Suppress("MemberVisibilityCanBePrivate")
                /**
                 * the service configuration
                 */
                val conf: UFServiceConfigurationV2
            ) : Out(ID) {
                /**
                 * @suppress
                 */
                companion object {
                    const val ID = 11
                }
            }
        }

    }

}

@Serializable
/**
 * A Map of target attributes with an aggregation policy
 *
 * @property attributes, a map [String] to [String] of target attributes
 * @property policy, an aggregation policy
 */
data class TargetAttributesWithPolicy(
    /**
     * @property attributes, a map [String] to [String] of target attributes
     */
    val attributes: Map<String,String>,
    /**
     * @property policy, an aggregation policy
     */
    val policy: AggregationPolicy = AggregationPolicy.REPLACE
){

    companion object{
        private val json = Json { encodeDefaults = true }
        @JvmStatic
        fun parse(jsonData:String): TargetAttributesWithPolicy = json.decodeFromString(serializer(), jsonData)
    }

    fun toJson():String = json.encodeToString(serializer(), this)
}

/**
 *  The aggregation policy of targetAttributes
 */
@Serializable
enum class AggregationPolicy{
    /**
     * Merge previous target attributes added using the AddTargetAttributes message.
     * (Old target attributes with same key are updated with the new values, the others
     * are keep unchanged)
     */
    MERGE,

    /**
     * Replace previous target attributes added using the AddTargetAttributes message
     * (Old target attributes are removed)
     */
    REPLACE
}