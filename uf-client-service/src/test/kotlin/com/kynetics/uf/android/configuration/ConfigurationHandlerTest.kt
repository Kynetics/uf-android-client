package com.kynetics.uf.android.configuration

import org.eclipse.hara.ddiclient.api.HaraClientData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ConfigurationHandlerTest {

    private val tenant = "tenant"
    private val controllerId = "controllerId"
    private val gatewayToken = "gatewayToken"

    private val clientData = HaraClientData(
        tenant = tenant,
        controllerId = controllerId,
        serverUrl = "https://stage.updatefactory.io",
        gatewayToken = gatewayToken
    )

    private val urlsThatShouldChangeMap = mapOf(
        "https://stage.updatefactory.io" to "https://ddi.stage.updatefactory.io",
        "https://personal.updatefactory.io" to "https://ddi.personal.updatefactory.io",
        "https://business.updatefactory.io" to "https://ddi.business.updatefactory.io",
        "https://stage.prod.updatefactory.io" to "https://ddi.stage.prod.updatefactory.io",
        "https://personal.prod.updatefactory.io" to "https://ddi.personal.prod.updatefactory.io",
        "https://business.prod.updatefactory.io" to "https://ddi.business.prod.updatefactory.io",
        "https://stage.test.updatefactory.io" to "https://ddi.stage.test.updatefactory.io",
        "https://personal.test.updatefactory.io" to "https://ddi.personal.test.updatefactory.io",
        "https://business.test.updatefactory.io" to "https://ddi.business.test.updatefactory.io",
    )

    private val urlsThatShouldNotChange = listOf(
        "https://corporate.debug.updatefactory.io",
        "https://ddi.business.prod.updatefactory.io",
        "https://ddi.business.updatefactory.io",
        "https://cdn.business.updatefactory.io",
        "https://hawkbit.eclipseprojects.io",
    )

    @Test
    fun testTheUrlsThatShouldChange() {
        urlsThatShouldChangeMap.forEach { (oldUrl, newUrl) ->
            clientData.copy(
                serverUrl = oldUrl
            ).apply {
                assertConfigurationUrlChangeTo(newUrl)
            }
        }
    }

    @Test
    fun testTheUrlsThatShouldStayTheSame() {
        urlsThatShouldNotChange.forEach { url ->
            assertConfigurationUrlStayTheSame(url)
        }
    }

    private fun HaraClientData.assertConfigurationUrlChangeTo(expectedUrl: String) {
        val newClientData = ConfigurationHandler.testConfigurationUrlUpdate(this)
        Assert.assertEquals(expectedUrl, newClientData.serverUrl)
    }

    private fun assertConfigurationUrlStayTheSame(serverUrl: String) {
        clientData.copy(
            serverUrl = serverUrl
        ).apply {
            assertConfigurationUrlChangeTo(serverUrl)
        }
    }
}