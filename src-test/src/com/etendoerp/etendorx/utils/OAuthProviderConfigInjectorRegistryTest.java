package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * Test class for OAuthProviderConfigInjectorRegistry.
 * Tests the registration and retrieval of OAuth provider config injectors.
 */
@RunWith(MockitoJUnitRunner.class)
public class OAuthProviderConfigInjectorRegistryTest {

    /**
     * Resets the injectors list before each test by using reflection to clear the static list.
     * @throws Exception if an error occurs while accessing or modifying the injectors field
     *         through reflection, such as:
     *         <ul>
     *           <li>{@link NoSuchFieldException} if the field cannot be found</li>
     *           <li>{@link IllegalAccessException} if the field cannot be accessed</li>
     *         </ul>
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
        TestInjector mockInjector = new TestInjector();
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
        TestInjector mockInjector = new TestInjector();
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

            // This method intentionally left empty as no configuration injection is needed for this implementation

        }
    }

    /**
     * Invalid test implementation without default constructor.
     */
    public static class InvalidTestInjector implements OAuthProviderConfigInjector {
        public InvalidTestInjector() {
            // Private constructor to force exception
        }

        @Override
        public void injectConfig(JSONObject sourceJSON, ETRXoAuthProvider provider) throws JSONException {
            // This method is intentionally empty since no configuration injection is required for this implementation

        }
    }
}