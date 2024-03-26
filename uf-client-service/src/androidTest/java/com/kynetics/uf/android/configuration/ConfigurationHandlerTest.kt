package com.kynetics.uf.android.configuration

import org.eclipse.hara.ddiclient.api.HaraClientData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ConfigurationHandlerTest {

    private val tenant = "tenant"
    private val controllerId = "controllerId"
    private val gatewayToken = "gatewayToken"

    @Test
    fun testAddingDdiSubdomainToBusinessTierServerUrl() {
        HaraClientData(
            tenant = tenant,
            controllerId = controllerId,
            serverUrl = "https://business.updatefactory.io",
            gatewayToken = gatewayToken
        ).also {
            val newClientData = ConfigurationHandler.testConfigurationUrlUpdate(it)
            assert(newClientData.serverUrl == "https://ddi.business.updatefactory.io")
        }
    }

    @Test
    fun testAddingDdiSubdomainToPersonalTierServerUrl() {
        HaraClientData(
            tenant = tenant,
            controllerId = controllerId,
            serverUrl = "https://personal.updatefactory.io",
            gatewayToken = gatewayToken
        ).also {
            val newClientData = ConfigurationHandler.testConfigurationUrlUpdate(it)
            assert(newClientData.serverUrl == "https://ddi.personal.updatefactory.io")
        }
    }

    @Test
    fun testAddingDdiSubdomainToStageTierServerUrl() {
        HaraClientData(
            tenant = tenant,
            controllerId = controllerId,
            serverUrl = "https://stage.updatefactory.io",
            gatewayToken = gatewayToken
        ).also {
            val newClientData = ConfigurationHandler.testConfigurationUrlUpdate(it)
            assert(newClientData.serverUrl == "https://ddi.stage.updatefactory.io")
        }
    }

    @Test
    fun testServerUrlShouldNotChangeInCaseOfUsingDdiSubdomain() {
        val serverUrl = "https://ddi.bussiness.updatefactory.io"
        HaraClientData(
            tenant = tenant,
            controllerId = controllerId,
            serverUrl = serverUrl,
            gatewayToken = gatewayToken
        ).also {
            val newClientData = ConfigurationHandler.testConfigurationUrlUpdate(it)
            assert(newClientData.serverUrl == serverUrl)
        }
    }

    @Test
    fun testServerUrlShouldNotChangeInCaseOfUsingCdnSubdomain() {
        val serverUrl = "https://cdn.bussiness.updatefactory.io"
        HaraClientData(
            tenant = tenant,
            controllerId = controllerId,
            serverUrl = serverUrl,
            gatewayToken = gatewayToken
        ).also {
            val newClientData = ConfigurationHandler.testConfigurationUrlUpdate(it)
            assert(newClientData.serverUrl == serverUrl)
        }
    }
}