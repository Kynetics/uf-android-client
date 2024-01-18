/*
 * Copyright © 2017-2024  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.android

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kynetics.uf.android.apicomptibility.ApiVersion
import com.kynetics.uf.android.client.RestartableClientService
import com.kynetics.uf.android.communication.CommunicationApi
import com.kynetics.uf.android.communication.CommunicationApiStrategy
import com.kynetics.uf.android.communication.messenger.MessageHandler
import com.kynetics.uf.android.communication.messenger.MessengerHandler
import com.kynetics.uf.android.configuration.AndroidDeploymentPermitProvider
import com.kynetics.uf.android.configuration.AndroidMessageListener
import com.kynetics.uf.android.configuration.ConfigurationHandler
import com.kynetics.uf.android.content.SharedPreferencesWithObject
import com.kynetics.uf.android.content.SharedPreferencesFactory
import com.kynetics.uf.android.ui.MainActivity
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.update.system.SystemUpdateType
import de.psdev.slf4j.android.logger.AndroidLoggerAdapter
import de.psdev.slf4j.android.logger.LogLevel
import org.eclipse.hara.ddiclient.api.MessageListener
import java.lang.ref.WeakReference

/*
 * @author Daniele Sergio
 */
class UpdateFactoryService : Service(), UpdateFactoryServiceCommand {

    override fun authorizationGranted() {
        softDeploymentPermitProvider?.allow(true)
    }

    override fun authorizationDenied() {
        softDeploymentPermitProvider?.allow(false)
    }

    private val mMessenger = Messenger(IncomingHandler(this))

    private var mNotificationManager: NotificationManager? = null
    private var systemUpdateType: SystemUpdateType = SystemUpdateType.SINGLE_COPY

    var softDeploymentPermitProvider: AndroidDeploymentPermitProvider? = null
    private var messageListener: MessageListener? = null

    private var sharedPreferencesFile: String? = null
    private var configurationHandler: ConfigurationHandler? = null
    private var ufService: RestartableClientService? = null

    private val api: CommunicationApi by lazy {
        CommunicationApiStrategy.newInstance(
            configurationHandler!!,
            ufService!!,
            softDeploymentPermitProvider!!
        )
    }
    override fun configureService() {
        if(ufService == null){
            ufService = RestartableClientService.newInstance(softDeploymentPermitProvider!!, listOf(messageListener!!))
        }
        when {
            configurationHandler!=null -> {
                ufService?.restartService(configurationHandler!!)
            }
            else -> {
                Log.w(TAG, "Service cant be configured because configuration handler is null")
            }
        }
    }

    private lateinit var forcePingPendingIntent: PendingIntent
    lateinit var currentUpdateState: CurrentUpdateState

    override fun onCreate() {
        super.onCreate()
        AndroidLoggerAdapter.setLogTag("Update Factory Client")
        if(BuildConfig.DEBUG){
            AndroidLoggerAdapter.setLogLevel(LogLevel.TRACE)
        }
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPreferencesFile = getString(R.string.shared_preferences_file)
        configurationHandler = ConfigurationHandler( this, getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE))
        systemUpdateType = SystemUpdateType.getSystemUpdateType()
        ufServiceCommand = this
        softDeploymentPermitProvider = AndroidDeploymentPermitProvider.build(configurationHandler!!, mNotificationManager!!, this)
        messageListener = AndroidMessageListener(this)
        currentUpdateState = CurrentUpdateState(this)

        val forcePingIntent = Intent(FORCE_PING_ACTION)

        forcePingPendingIntent = PendingIntent.getBroadcast(this, 1, forcePingIntent, 0)

        // add actions here !
        val intentFilter = IntentFilter()
        intentFilter.addAction(FORCE_PING_ACTION)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == FORCE_PING_ACTION) {
                    ufService?.forcePing()
                    MessengerHandler.onAction(MessageHandler.Action.FORCE_PING)
                    val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                    sendBroadcast(closeIntent)
                }
            }
        }

        registerReceiver(receiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, String.format("service's starting with version %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        startForeground()
        isRunning = true
        var serviceConfiguration = configurationHandler?.getConfigurationFromFile()
        if (serviceConfiguration == null && intent != null) {
            serviceConfiguration = configurationHandler?.getServiceConfigurationFromIntent(intent)
        } else if (serviceConfiguration != null) {
            Log.i(TAG, "Loaded new configuration from file")
        }
        if (configurationHandler?.getCurrentConfiguration() != serviceConfiguration) {
            configurationHandler?.saveServiceConfigurationToSharedPreferences(serviceConfiguration)
        }
        configureService()
        return START_STICKY
    }

    // todo add api to configure targetAttibutes (separete  d from serviceConfiguration)
    private class IncomingHandler(service: UpdateFactoryService) : Handler(Looper.myLooper()!!) {
        private val updateFactoryServiceRef = WeakReference(service)

        private fun <T> WeakReference<T>.execute(action: T.() -> Unit){
            get()?.let { ref ->
                ref.action()
            }
        }
        override fun handleMessage(msg: Message) {
            updateFactoryServiceRef.execute { api.onMessage(msg) }
        }

    }

    override fun onBind(intent: Intent): IBinder? {
        startService(this)
        return mMessenger.binder
    }

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferencesWithObject {
        return SharedPreferencesFactory.get(applicationContext, name, mode)
    }

    private fun startForeground() {
        if(mNotificationManager != null) {
            ApiVersion.fromVersionCode().configureChannel(CHANNEL_ID, getString(R.string.app_name), mNotificationManager!!)
            startForeground(NOTIFICATION_ID, getNotification(""))
        }
    }

    fun getNotification(notificationContent: String, forcePingAction: Boolean = false): Notification {

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.uf_logo)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
                .setContentTitle(getString(R.string.update_factory_notification_title))
                .setContentText(notificationContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (forcePingAction) {
            notificationBuilder.addAction(android.R.drawable.ic_popup_sync, "Grant Authorization", forcePingPendingIntent)
        }
        return notificationBuilder.build()
    }

    companion object {
        enum class AuthorizationType(
            val toActionOnGranted: MessageHandler.Action,
            val toActionOnDenied: MessageHandler.Action
        ) {
            DOWNLOAD(
                    MessageHandler.Action.AUTH_DOWNLOAD_GRANTED,
                    MessageHandler.Action.AUTH_DOWNLOAD_DENIED
            ) {
                override val extra = MainActivity.INTENT_TYPE_EXTRA_VALUE_DOWNLOAD
                override val event = "WaitingDownloadAuthorization"
            },

            UPDATE(
                MessageHandler.Action.AUTH_UPDATE_GRANTED,
                    MessageHandler.Action.AUTH_UPDATE_DENIED) {
                override val extra: Int = MainActivity.INTENT_TYPE_EXTRA_VALUE_REBOOT
                override val event = "WaitingUpdateAuthorization"
            };

            abstract val extra: Int
            abstract val event: String
        }

        @JvmStatic
        fun startService(context: Context) {
            if (!isRunning) {
                val myIntent = Intent(context, UpdateFactoryService::class.java)
                ApiVersion.fromVersionCode().startService(context, myIntent)
            }
        }

        var isRunning = false

        @JvmStatic
        var ufServiceCommand: UpdateFactoryServiceCommand? = null
        private const val FORCE_PING_ACTION = "ForcePing"
        private const val CHANNEL_ID = "UPDATE_FACTORY_NOTIFICATION_CHANNEL_ID"
        const val NOTIFICATION_ID = 1
        private val TAG = UpdateFactoryService::class.java.simpleName
    }
}
