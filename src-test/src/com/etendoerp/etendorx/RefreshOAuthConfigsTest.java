package com.etendoerp.etendorx;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.util.ArrayList;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * Unit test class for testing the functionality of {@link RefreshOAuthConfigs}.
 * It verifies the behavior of the class under various scenarios using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class RefreshOAuthConfigsTest {

    @InjectMocks
    private RefreshOAuthConfigs refreshOAuthConfigs;

    @Mock
    private OBDal obDal;

    @Mock
    private OBCriteria<ETRXConfig> mockCriteria;

    /**
     * Sets up the mocked dependencies before running each test.
     *
     * @throws Exception if there is an issue during initialization
     */
    @Before
    public void setUp() throws Exception {
        when(obDal.createCriteria(ETRXConfig.class)).thenReturn(mockCriteria);
        when(mockCriteria.add(any())).thenReturn(mockCriteria);
    }

    /**
     * Tests the action method of {@link RefreshOAuthConfigs} when no configurations
     * are available to refresh. Expects an {@link OBException} to be thrown with
     * a specific message.
     */
    @Test
    public void testActionWithNoConfigs() {
        JSONObject params = new JSONObject();
        MutableBoolean isStopped = new MutableBoolean(false);
        when(mockCriteria.list()).thenReturn(new ArrayList<>());

        try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
             MockedStatic<OBMessageUtils> messageUtilsStatic = mockStatic(OBMessageUtils.class)) {

            obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
            messageUtilsStatic.when(() -> OBMessageUtils.getI18NMessage("ETRX_NoConfigToRefresh"))
                .thenReturn("No configuration to refresh");

            Exception exception = assertThrows(OBException.class, () ->
                refreshOAuthConfigs.action(params, isStopped)
            );
            assertEquals("No configuration to refresh", exception.getMessage());
        }
    }

    /**
     * Tests the getInputClass method of {@link RefreshOAuthConfigs} to ensure it
     * returns the correct class type {@link ETRXoAuthProvider}.
     */
    @Test
    public void testGetInputClass() {
        assertEquals(ETRXoAuthProvider.class, refreshOAuthConfigs.getInputClass());
    }

}