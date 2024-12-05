package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * Test class for OAuthProviderConfigInjectorRegistry.
 * Tests the registration and retrieval of OAuth provider config injectors.
 */
@RunWith(MockitoJUnitRunner.class)
public class OAuthProviderConfigInjectorRegistryTest {

    @Mock
    private OAuthProviderConfigInjector mockInjector;


    /**
     * Resets the injectors list before each test by using reflection to clear the static list.
     */
    @Before
    public void setUp() throws Exception {
        java.lang.reflect.Field injectorsField = OAuthProviderConfigInjectorRegistry.class
            .getDeclaredField("injectors");
        injectorsField.setAccessible(true);
        ((List<?>) injectorsField.get(null)).clear();
    }

    /**
     * Tests manual registration of an injector.
     */
    @Test
    public void testRegisterInjector() {
        OAuthProviderConfigInjectorRegistry.registerInjector(mockInjector);
        List<OAuthProviderConfigInjector> injectors = OAuthProviderConfigInjectorRegistry.getInjectors();

        assertEquals("Should contain one injector", 1, injectors.size());
        assertTrue("Should contain the registered injector", injectors.contains(mockInjector));
    }

    /**
     * Tests that the list returned by getInjectors is unmodifiable.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testGetInjectorsReturnsUnmodifiableList() {
        OAuthProviderConfigInjectorRegistry.registerInjector(mockInjector);
        List<OAuthProviderConfigInjector> injectors = OAuthProviderConfigInjectorRegistry.getInjectors();

        injectors.add(mockInjector);
    }

    /**
     * Test implementation of OAuthProviderConfigInjector for testing purposes.
     */
    public static class TestInjector implements OAuthProviderConfigInjector {

        @Override
        public void injectConfig(JSONObject sourceJSON, ETRXoAuthProvider provider) throws JSONException {

        }
    }

    /**
     * Invalid test implementation without default constructor.
     */
    public static class InvalidTestInjector implements OAuthProviderConfigInjector {
        private InvalidTestInjector() {
            // Private constructor to force exception
        }

        @Override
        public void injectConfig(JSONObject sourceJSON, ETRXoAuthProvider provider) throws JSONException {

        }
    }
}