package com.etendoerp.etendorx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.etendoerp.etendorx.utils.RXConfigUtils;

/**
 * Test class for the GetTokenURL functionality.
 * Verifies the behavior of the `execute` method under different scenarios.
 */
public class GetTokenURLTest {


    private GetTokenURL getTokenURL;

    @Mock
    private OBContext obContext;

    @Mock
    private ETRXoAuthProvider oauthProvider;

    @Mock
    private ETRXConfig rxConfig;

    @Mock
    private OBDal obDal;

    @Mock
    private User user;

    /**
     * Sets up the test environment.
     * Mocks the necessary static methods and dependencies before each test.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        getTokenURL = new GetTokenURL();

        try (var mockedOBContext = mockStatic(OBContext.class);
             var mockedOBDal = mockStatic(OBDal.class)
             ) {

            mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
            mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
        }
    }

    /**
     * Test case for successful token URL generation.
     * Verifies that the `execute` method correctly generates a valid URL
     * when all necessary configurations are present.
     *
     * @throws Exception if an error occurs during test execution
     */
    @Test
    public void testExecuteSuccessfulTokenURLGeneration() throws Exception {
        // Arrange
        try (var mockedOBContext = mockStatic(OBContext.class);
             var mockedOBDal = mockStatic(OBDal.class);
             var mockedAuthUtils = mockStatic(RXConfigUtils.class)) {

            String oAuthProviderId = "testId";
            String userId = "testUserId";
            String publicURL = "http://test.com/";
            String authEndpoint = "auth/";
            String value = "token";

            mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
            mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
            mockedAuthUtils.when(() -> RXConfigUtils.getRXConfig(TestUtils.AUTH)).thenReturn(rxConfig);

            when(obContext.getUser()).thenReturn(user);
            when(user.getId()).thenReturn(userId);
            when(obDal.get(ETRXoAuthProvider.class, oAuthProviderId)).thenReturn(oauthProvider);
            when(oauthProvider.getAuthorizationEndpoint()).thenReturn(authEndpoint);
            when(oauthProvider.getValue()).thenReturn(value);
            when(rxConfig.getPublicURL()).thenReturn(publicURL);

            JSONObject content = new JSONObject();
            content.put("id", oAuthProviderId);

            JSONObject result = getTokenURL.execute(new HashMap<>(), content.toString());

            assertNotNull(result);
            assertTrue(result.has("auth_url"));

          String urlBuilder = publicURL +
              authEndpoint +
              value +
              "?userId=" +
              userId +
              "&etrxOauthProviderId=" +
              oAuthProviderId;

            assertEquals(urlBuilder, result.getString("auth_url"));
        }
    }

    /**
     * Test case for missing configuration.
     * Verifies that the `execute` method returns an appropriate error message
     * when the required authorization configuration is missing.
     *
     * @throws Exception if an error occurs during test execution
     */
    @Test
    public void testExecuteMissingConfig() throws Exception {
        try (var mockedOBContext = mockStatic(OBContext.class);
             var mockedOBDal = mockStatic(OBDal.class);
             var mockedAuthUtils = mockStatic(RXConfigUtils.class);
             var mockedOBMessageUtils = mockStatic(OBMessageUtils.class)) {

            String oAuthProviderId = "testId";
            String errorMessage = "No auth config found";

            mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
            mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
            mockedAuthUtils.when(() -> RXConfigUtils.getRXConfig(TestUtils.AUTH)).thenReturn(null);
            mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage("ETRX_NoConfigAuthFound"))
                .thenReturn(errorMessage);

            when(obDal.get(ETRXoAuthProvider.class, oAuthProviderId)).thenReturn(oauthProvider);

            JSONObject content = new JSONObject();
            content.put("id", oAuthProviderId);

            JSONObject result = getTokenURL.execute(new HashMap<>(), content.toString());

            assertNotNull(result);
            assertTrue(result.has(TestUtils.JSON_KEY_MESSAGE));
            JSONObject message = result.getJSONObject(TestUtils.JSON_KEY_MESSAGE);
            assertEquals(TestUtils.ERROR_SEVERITY, message.getString(TestUtils.JSON_KEY_SEVERITY));
            assertEquals(TestUtils.ERROR_TITLE, message.getString(TestUtils.JSON_KEY_TITLE));
            assertEquals(errorMessage, message.getString("text"));
        }
    }

    /**
     * Test case for invalid input content.
     * Verifies that the `execute` method handles invalid JSON content gracefully
     * and returns an error message.
     *
     * @throws Exception if an error occurs during test execution
     */
    @Test
    public void testExecuteInvalidContent() throws Exception {
        try (var mockedOBContext = mockStatic(OBContext.class)) {
            mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);

            String invalidContent = "{invalid json}";

            JSONObject result = getTokenURL.execute(new HashMap<>(), invalidContent);

            assertNotNull(result);
            assertTrue(result.has(TestUtils.JSON_KEY_MESSAGE));
            JSONObject message = result.getJSONObject(TestUtils.JSON_KEY_MESSAGE);
            assertEquals(TestUtils.ERROR_SEVERITY, message.getString(TestUtils.JSON_KEY_SEVERITY));
            assertEquals(TestUtils.ERROR_TITLE, message.getString(TestUtils.JSON_KEY_TITLE));
        }
    }
}