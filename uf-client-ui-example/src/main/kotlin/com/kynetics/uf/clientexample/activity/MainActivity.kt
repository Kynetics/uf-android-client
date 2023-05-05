/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.clientexample.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.UFServiceInfo
import com.kynetics.uf.android.api.toOutV1Message
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.clientexample.BuildConfig
import com.kynetics.uf.clientexample.R
import com.kynetics.uf.clientexample.data.MessageHistory
import com.kynetics.uf.clientexample.fragment.ListStateFragment
import com.kynetics.uf.clientexample.fragment.MyAlertDialogFragment
import com.kynetics.uf.clientexample.fragment.UFServiceInteractionFragment
import kotlinx.android.synthetic.main.state_list.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.timer
import kotlin.properties.Delegates

/**
 * @author Daniele Sergio
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var twoPane: Boolean = false

    private var timer: Timer? = null

    /** Messenger for communicating with service.  */
    internal var mService: Messenger? = null
    /** Flag indicating whether we have called bind on the service.  */
    internal var mIsBound: Boolean by Delegates.observable(false) { _, old, new ->
        when {
            new == old -> {}

            new -> {
                mSnackbarServiceDisconnect?.dismiss()
                timer?.purge()
                timer?.cancel()
            }

            !new -> {
                mSnackbarServiceDisconnect?.show()
                timer = timer(
                    name = "Service Reconnection",
                    initialDelay = 5_000,
                    period = 30_000.toLong()
                ) {
                    Log.i(TAG, "Try reconnection")
                    doBindService()
                }
                mService = null
            }
        }
    }

    private var mSnackbarServiceDisconnect: Snackbar? = null
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    internal val mMessenger:Messenger by lazy {
        Messenger(IncomingHandler(this))
    }
    private var mServiceExist = false

    /**
     * Class for interacting with the main interface of the service.
     */
    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            mService = Messenger(service)

            Toast.makeText(this@MainActivity, R.string.ui_connected,
                Toast.LENGTH_SHORT).show()

            handleRemoteException {
                mService?.send(Communication.V1.In.RegisterClient(mMessenger).toMessage())
                mService?.send(Communication.V1.In.Sync(mMessenger).toMessage())
            }
            mIsBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.i(TAG, "Service is disconnected")
            doUnbindService()
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.i(TAG, "Service binding is died")
            mIsBound = false
        }
    }

    private var mResumeUpdateFab: FloatingActionButton? = null
    private var mNavigationView: NavigationView? = null

    private fun handleRemoteException(body: () -> Unit) {
        try {
            body.invoke()
        } catch (e: RemoteException) {
            Toast.makeText(this@MainActivity, "service communication error",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun NavigationView.configure(listener: NavigationView.OnNavigationItemSelectedListener) {
        mNavigationView!!.setNavigationItemSelectedListener(listener)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val textViewUiVersion = findViewById<TextView>(R.id.ui_version)
        val textViewServiceVersion = findViewById<TextView>(R.id.service_version)
        textViewUiVersion.text = String.format(getString(R.string.ui_version), BuildConfig.VERSION_NAME)
        try {
            val info = packageManager.getPackageInfo(UFServiceInfo.SERVICE_PACKAGE_NAME, 0)
            textViewServiceVersion.text = String.format(getString(R.string.service_version), info.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            textViewServiceVersion.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mResumeUpdateFab = findViewById(R.id.fab_resume_update)
        mResumeUpdateFab!!.setOnClickListener { _ ->
            handleRemoteException {
                mService?.send(Communication.V1.In.ForcePing.toMessage())
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val mToolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(mToolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        val navigationViewWrapper: NavigationView = findViewById(R.id.nav_view_wrapper)
        mNavigationView = navigationViewWrapper.findViewById(R.id.nav_view)

        mSnackbarServiceDisconnect = Snackbar.make(findViewById<View>(R.id.coordinatorLayout),
            R.string.service_disconnected, Snackbar.LENGTH_INDEFINITE)

        mSnackbarServiceDisconnect?.show()

        navigationViewWrapper.configure(this)
        initAccordingScreenSize()
    }

    override fun onStart() {
        super.onStart()
        doBindService()
    }

    override fun onStop() {
        super.onStop()
        doUnbindService()
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        when {
            drawer.isDrawerOpen(GravityCompat.START) -> drawer.closeDrawer(GravityCompat.START)
            !twoPane -> onBackPressedWithOnePane()
            else -> super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {

            R.id.menu_settings -> {
                val settingsIntent = Intent(UFServiceInfo.ACTION_SETTINGS)
                startActivity(settingsIntent)
            }

            R.id.force_ping -> {
                Log.d(TAG, "Force Ping Request")
                handleRemoteException {
                    mService?.send(Communication.V1.In.ForcePing.toMessage())
                }
            }

            R.id.menu_back -> onBackPressedWithOnePane()
        }

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    fun sendPermissionResponse(response: Boolean) {
        handleRemoteException {
            mService?.send(Communication.V1.In.AuthorizationResponse(response).toMessage())
        }
    }

    /**
     * Handler of incoming messages from service.
     */
    internal class IncomingHandler(mainActivity: MainActivity) : Handler(Looper.getMainLooper()) {
        private val activityRef = WeakReference(mainActivity)

        private fun <T>WeakReference<T>.execute(action: T.() -> Unit){
            get()?.let { ref ->
                ref.action()
            }
        }

        override fun handleMessage(msg: Message) {
            runCatching {
                @Suppress("DEPRECATION")
                when (val v1Msg = msg.toOutV1Message()) {

                    is Communication.V1.Out.CurrentServiceConfiguration -> handleServiceConfigurationMsg(v1Msg)

                    is Communication.V1.Out.AuthorizationRequest -> handleAuthorizationRequestMsg(v1Msg)

                    is Communication.V1.Out.ServiceNotification -> handleServiceNotificationMsg(v1Msg)

                    is Communication.V1.Out.CurrentServiceConfigurationV2 -> handleServiceConfigurationV2Msg(v1Msg)
                }
            }.onFailure {
                Log.w(TAG, "Cannot handle the $msg message", it)
            }
        }

        private fun handleServiceConfigurationMsg(
            @Suppress("DEPRECATION")
            currentServiceConfiguration: Communication.V1.Out.CurrentServiceConfiguration
        ) {
           Log.i(TAG, currentServiceConfiguration.conf.toString())
        }

        private fun handleServiceConfigurationV2Msg(
            currentServiceConfiguration: Communication.V1.Out.CurrentServiceConfigurationV2
        ) {
            Log.i(TAG, currentServiceConfiguration.conf.toString())
        }



        private fun handleAuthorizationRequestMsg(authRequest: Communication.V1.Out.AuthorizationRequest) =
            activityRef.execute {
                try{
                    val newFragment = MyAlertDialogFragment.newInstance(authRequest.authName)
                    newFragment.show(supportFragmentManager, null)
                } catch (e:IllegalStateException){
                    Log.w(TAG, "Error on show alert dialog", e)
                }
            }

        private fun handleServiceNotificationMsg(serviceNotification: Communication.V1.Out.ServiceNotification) =
            activityRef.execute {
                val content = serviceNotification.content
                when (content) {
                    is UFServiceMessageV1.Event -> {
                        post {
                            if (!MessageHistory.appendEvent(content)) {
                                Toast.makeText(
                                    applicationContext,
                                    content.name.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }
                    }

                    is UFServiceMessageV1.State -> {
                        MessageHistory.addState(MessageHistory.StateEntry(state = content))
                    }
                }

                supportFragmentManager.fragments
                    .filterIsInstance<UFServiceInteractionFragment>()
                    .forEach { fragment -> fragment.onMessageReceived(content) }

                when (content) {
                    is UFServiceMessageV1.State.WaitingDownloadAuthorization,
                    UFServiceMessageV1.State.WaitingUpdateAuthorization -> {
                        mResumeUpdateFab!!.setImageResource(iconByMessageName.getValue(content.name))
                        mResumeUpdateFab!!.show()
                    }

                    is UFServiceMessageV1.State -> mResumeUpdateFab!!.hide()

                    else -> { }
                }
            }

        private val iconByMessageName = mapOf(
            UFServiceMessageV1.MessageName.WAITING_DOWNLOAD_AUTHORIZATION to R.drawable.ic_get_app_black_48dp,
            UFServiceMessageV1.MessageName.WAITING_UPDATE_AUTHORIZATION to R.drawable.ic_loop_black_48dp
        )
    }

    fun changePage(fragment: Fragment, addToBackStack: Boolean = true) {
        val tx = supportFragmentManager.beginTransaction()
            .replace(R.id.main_content, fragment)

        if (addToBackStack) {
            tx.addToBackStack(null)
        }

        tx.commit()
    }

    private fun initAccordingScreenSize() {
        twoPane = state_detail_container != null
        val listStateFragment = ListStateFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ListStateFragment.ARG_TWO_PANE, this@MainActivity.twoPane)
            }
        }
        if (twoPane) {
            this.supportFragmentManager
                .beginTransaction()
                .replace(R.id.state_list_container, listStateFragment)
                .commit()
        } else {
            changePage(listStateFragment, false)
        }
    }

    private fun onBackPressedWithOnePane() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    private fun doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        val intent = Intent(UFServiceInfo.SERVICE_ACTION)
        intent.setPackage(UFServiceInfo.SERVICE_PACKAGE_NAME)
        intent.flags = FLAG_INCLUDE_STOPPED_PACKAGES
        val serviceExist = bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        if (!serviceExist && !mServiceExist) {
            Toast.makeText(applicationContext, "UpdateFactoryService not found", Toast.LENGTH_LONG).show()
            unbindService(mConnection)
            this.finish()
        } else {
            mServiceExist = true
        }
    }

    private fun doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            try {
                mService?.send(Communication.V1.In.UnregisterClient(mMessenger).toMessage())
            } catch (e: RemoteException) {
                // There is nothing special we need to do if the service
                // has crashed.
            }

            // Detach our existing connection.
            unbindService(mConnection)
            mIsBound = false
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
