package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBCriteria;
import com.etendoerp.etendorx.data.ETRXConfig;

/**
 * Unit tests for the AuthUtils class.
 * This test suite verifies the behavior of utility methods related to authentication configurations.
 */
@RunWith(MockitoJUnitRunner.class)
public class RXConfigTest {

    @Mock
    private OBCriteria<ETRXConfig> mockCriteria;

    private MockedStatic<OBDal> mockedOBDal;
    private AutoCloseable mocks;

    /**
     * Sets up the test environment by initializing mocks and static behaviors.
     */
    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        mockedOBDal = mockStatic(OBDal.class);
        OBDal mockOBDal = mock(OBDal.class);

        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
        when(mockOBDal.createCriteria(ETRXConfig.class)).thenReturn(mockCriteria);
    }

    /**
     * Cleans up resources and closes static mocks after each test.
     *
     * @throws Exception if an error occurs during cleanup
     */
    @After
    public void tearDown() throws Exception {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Tests the retrieval of an ETRXConfig entity using a service name.
     * Verifies that the correct configuration is returned.
     */
    @Test
    public void testGetRXConfig() {
        String serviceName = "myService";
        ETRXConfig expectedConfig = new ETRXConfig();
        expectedConfig.setServiceName(serviceName);

        when(OBDal.getInstance().createCriteria(ETRXConfig.class))
            .thenReturn(mockCriteria);
        when(mockCriteria.add(Mockito.any())).thenReturn(mockCriteria);
        when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
        when(mockCriteria.uniqueResult()).thenReturn(expectedConfig);

        ETRXConfig actualConfig = RXConfigUtils.getRXConfig(serviceName);

        assertEquals(expectedConfig, actualConfig);
    }

    /**
     * Tests the private constructor of AuthUtils to ensure it throws an exception
     * when accessed via reflection. The constructor should not be instantiated.
     */
    @Test
    public void testPrivateConstructorThrowsException() {
        try {
            Constructor<RXConfigUtils> constructor = RXConfigUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Expected IllegalStateException to be thrown");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalStateException);
            assertEquals("Utility class", cause.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
