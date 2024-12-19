package com.etendoerp.etendorx.events;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.TestUtils;
import com.etendoerp.etendorx.data.ETRXJavaMapping;

/**
 * Test class for the JavaMappingsEventHandler.
 * This test class verifies the behavior of the JavaMappingsEventHandler
 * using Mockito's silent runner, which suppresses unnecessary logging
 * during test execution.
 * The test class includes:
 * <ul>
 *   <li>Constants for test data and validation messages</li>
 *   <li>Mocked objects for testing event handling scenarios</li>
 *   <li>Setup method to prepare test environment</li>
 * </ul>
 *
 * Uses {@link MockitoJUnitRunner.Silent} to run tests with reduced noise
 * and improved readability.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class JavaMappingsEventHandlerTest {


    private TestableJavaMappingsEventHandler handler;
    private ETRXJavaMapping javaMapping;

    /**
     * Prepares the test environment before each test method.
     * Initializes:
     * <ul>
     *   <li>A new testable Java mappings event handler</li>
     *   <li>A mocked ETRXJavaMapping entity</li>
     * </ul>
     */
    @Before
    public void setUp() {
        handler = new TestableJavaMappingsEventHandler();
        javaMapping = mock(ETRXJavaMapping.class);
    }

    /**
     * Tests the validateJavaMappingsQualifier method with a valid qualifier.
     */
    @Test
    public void testValidateJavaMappingsQualifierWithValidQualifier() {
        assertTrue(handler.validateJavaMappingsQualifier(TestUtils.VALID_QUALIFIER));
    }

    /**
     * Tests the validateJavaMappingsQualifier method with a null qualifier.
     */
    @Test
    public void testValidateJavaMappingsQualifierWithNullQualifier() {
        assertFalse(handler.validateJavaMappingsQualifier(null));
    }

    /**
     * Tests the validateJavaMappingsQualifier method with a too-short qualifier.
     */
    @Test
    public void testValidateJavaMappingsQualifierWithTooShortQualifier() {
        assertFalse(handler.validateJavaMappingsQualifier("ab"));
    }

    /**
     * Tests the validateJavaMappingsQualifier method with a too-long qualifier.
     */
    @Test
    public void testValidateJavaMappingsQualifierWithTooLongQualifier() {
        assertFalse(handler.validateJavaMappingsQualifier("ThisQualifierIsWayTooLongAndShouldNotBeAccepted"));
    }

    /**
     * Tests the validateJavaMappingsQualifier method with a qualifier containing special characters.
     */
    @Test
    public void testValidateJavaMappingsQualifierWithSpecialCharacters() {
        assertFalse(handler.validateJavaMappingsQualifier("Invalid123"));
    }

    /**
     * Tests the onUpdate method when the event is not valid.
     */
    @Test
    public void testOnUpdateWhenNotValidEvent() {
        EntityUpdateEvent event = mock(EntityUpdateEvent.class);
        handler = spy(new TestableJavaMappingsEventHandler());
        doReturn(false).when(handler).isValidEvent(any(EntityUpdateEvent.class));

        handler.onUpdate(event);

        verify(event, never()).getTargetInstance();
    }

    /**
     * Tests the onSave method when the event is not valid.
     */
    @Test
    public void testOnSaveWhenNotValidEvent() {
        EntityNewEvent event = mock(EntityNewEvent.class);
        handler = spy(new TestableJavaMappingsEventHandler());
        doReturn(false).when(handler).isValidEvent(any(EntityNewEvent.class));

        handler.onSave(event);

        verify(event, never()).getTargetInstance();
    }

    /**
     * Tests the onUpdate method with a valid qualifier.
     */
    @Test
    public void testOnUpdateWithValidQualifier() {
        EntityUpdateEvent event = mock(EntityUpdateEvent.class);
        when(javaMapping.getQualifier()).thenReturn(TestUtils.VALID_QUALIFIER);
        when(javaMapping.getName()).thenReturn(TestUtils.TEST_MAPPING);
        when(event.getTargetInstance()).thenReturn(javaMapping);

        handler = spy(new TestableJavaMappingsEventHandler());
        doReturn(true).when(handler).isValidEvent(any(EntityUpdateEvent.class));

        handler.onUpdate(event);
    }

    /**
     * Tests the onSave method with a valid qualifier.
     */
    @Test
    public void testOnSaveWithValidQualifier() {
        EntityNewEvent event = mock(EntityNewEvent.class);
        when(javaMapping.getQualifier()).thenReturn(TestUtils.VALID_QUALIFIER);
        when(javaMapping.getName()).thenReturn(TestUtils.TEST_MAPPING);
        when(event.getTargetInstance()).thenReturn(javaMapping);

        handler = spy(new TestableJavaMappingsEventHandler());
        doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

        handler.onSave(event);
    }

    /**
     * Tests the onUpdate method with an invalid qualifier, which should throw an OBException.
     */
    @Test(expected = OBException.class)
    public void testOnUpdateWithInvalidQualifier() {
        try (MockedStatic<OBMessageUtils> mockedUtils = mockStatic(OBMessageUtils.class)) {
            EntityUpdateEvent event = mock(EntityUpdateEvent.class);
            when(javaMapping.getQualifier()).thenReturn("123");
            when(javaMapping.getName()).thenReturn(TestUtils.TEST_MAPPING);
            when(event.getTargetInstance()).thenReturn(javaMapping);

            mockedUtils.when(() -> OBMessageUtils.getI18NMessage(
                    eq(TestUtils.INVALID_PROJECTION_MESSAGE),
                    any(String[].class)))
                .thenReturn("Invalid qualifier");

            handler = spy(new TestableJavaMappingsEventHandler());
            doReturn(true).when(handler).isValidEvent(any(EntityUpdateEvent.class));

            handler.onUpdate(event);
        }
    }

    /**
     * Tests the onSave method with an invalid qualifier, which should throw an OBException.
     */
    @Test(expected = OBException.class)
    public void testOnSaveWithInvalidQualifier() {
        try (MockedStatic<OBMessageUtils> mockedUtils = mockStatic(OBMessageUtils.class)) {
            EntityNewEvent event = mock(EntityNewEvent.class);
            when(javaMapping.getQualifier()).thenReturn("a1");
            when(javaMapping.getName()).thenReturn(TestUtils.TEST_MAPPING);
            when(event.getTargetInstance()).thenReturn(javaMapping);

            mockedUtils.when(() -> OBMessageUtils.getI18NMessage(
                    eq(TestUtils.INVALID_PROJECTION_MESSAGE),
                    any(String[].class)))
                .thenReturn("Invalid qualifier");

            handler = spy(new TestableJavaMappingsEventHandler());
            doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

            handler.onSave(event);
        }
    }

    /**
     * A testable implementation of the JavaMappingsEventHandler class.
     * This class overrides the isValidEvent method to allow for testing
     * the behavior of the event handler.
     */
    private static class TestableJavaMappingsEventHandler extends JavaMappingsEventHandler {
        @Override
        public boolean isValidEvent(EntityPersistenceEvent event) {
            return super.isValidEvent(event);
        }
    }
}