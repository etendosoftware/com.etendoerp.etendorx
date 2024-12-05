package com.etendoerp.etendorx.events;

import static org.junit.Assert.assertEquals;
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
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

/**
 * Test suite for the EntityFieldEventHandler class.
 * This test suite runs in a silent Mockito JUnit runner to suppress unnecessary console output.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class EntityFieldEventHandlerTest {

    private TestableEntityFieldEventHandler handler;
    private ETRXEntityField entityField;
    private ETRXProjectionEntity projectionEntity;

    @Before
    public void setUp() {
        handler = new TestableEntityFieldEventHandler();
        entityField = mock(ETRXEntityField.class);
        projectionEntity = mock(ETRXProjectionEntity.class);
    }

    /**
     * Tests the getObservedEntities method to ensure it returns the expected entities.
     */
    @Test
    public void testGetObservedEntities() {
        try (MockedStatic<ModelProvider> mockedProvider = mockStatic(ModelProvider.class)) {
            ModelProvider providerInstance = mock(ModelProvider.class);
            Entity mockEntity = mock(Entity.class);

            mockedProvider.when(ModelProvider::getInstance).thenReturn(providerInstance);
            when(providerInstance.getEntity(ETRXEntityField.class)).thenReturn(mockEntity);

            Entity[] observedEntities = handler.getObservedEntities();

            assertEquals(1, observedEntities.length);
        }
    }

    /**
     * Tests the onUpdate method when the event is not valid.
     */
    @Test
    public void testOnUpdateWhenNotValidEvent() {
        EntityUpdateEvent event = mock(EntityUpdateEvent.class);
        handler = spy(new TestableEntityFieldEventHandler());
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
        handler = spy(new TestableEntityFieldEventHandler());
        doReturn(false).when(handler).isValidEvent(any(EntityNewEvent.class));

        handler.onSave(event);

        verify(event, never()).getTargetInstance();
    }

    /**
     * Tests the validateEntityField method when the field mapping is "JM" and the property is blank.
     */
    @Test
    public void testValidateEntityFieldWithJMMapingAndBlankProperty() {
        when(entityField.getFieldMapping()).thenReturn("JM");
        when(entityField.getProperty()).thenReturn("");

        handler.validateEntityField(entityField);
    }

    /**
     * Tests the validateEntityField method when the mapping type is "W" and the property is valid.
     */
    @Test
    public void testValidateEntityFieldWithWMappingTypeAndValidProperty() {
        when(entityField.getFieldMapping()).thenReturn("Other");
        when(entityField.getProperty()).thenReturn("property.with.dots");
        when(entityField.getEtrxProjectionEntity()).thenReturn(projectionEntity);
        when(projectionEntity.getMappingType()).thenReturn("W");

        handler.validateEntityField(entityField);
    }

    /**
     * Tests the validateEntityField method when the mapping type is not "W".
     */
    @Test
    public void testValidateEntityFieldWithNonWMappingType() {
        when(entityField.getFieldMapping()).thenReturn("Other");
        when(entityField.getProperty()).thenReturn("property.with.many.dots");
        when(entityField.getEtrxProjectionEntity()).thenReturn(projectionEntity);
        when(projectionEntity.getMappingType()).thenReturn("Other");

        handler.validateEntityField(entityField);
    }

    /**
     * Tests the validateEntityField method when the mapping type is "W" and the property is invalid, which should throw an OBException.
     */
    @Test(expected = OBException.class)
    public void testValidateEntityFieldWithWMappingTypeAndInvalidProperty() {
        try (MockedStatic<OBMessageUtils> mockedUtils = mockStatic(OBMessageUtils.class)) {
            when(entityField.getFieldMapping()).thenReturn("Other");
            when(entityField.getProperty()).thenReturn("property.with.too.many.dots");
            when(entityField.getEtrxProjectionEntity()).thenReturn(projectionEntity);
            when(projectionEntity.getMappingType()).thenReturn("W");

            mockedUtils.when(() -> OBMessageUtils.getI18NMessage(
                    eq("EntityFieldEventHandler.invalidProperty")))
                .thenReturn("Invalid property");

            handler.validateEntityField(entityField);
        }
    }

    /**
     * Tests the onUpdate method with a valid entity field.
     */
    @Test
    public void testOnUpdateWithValidEntityField() {
        try (MockedStatic<ModelProvider> mockedProvider = mockStatic(ModelProvider.class)) {
            EntityUpdateEvent event = mock(EntityUpdateEvent.class);
            when(event.getTargetInstance()).thenReturn(entityField);
            when(entityField.getFieldMapping()).thenReturn("JM");
            when(entityField.getProperty()).thenReturn("");

            handler = spy(new TestableEntityFieldEventHandler());
            doReturn(true).when(handler).isValidEvent(any(EntityUpdateEvent.class));

            handler.onUpdate(event);
        }
    }

    /**
     * Tests the onSave method with a valid entity field.
     */
    @Test
    public void testOnSaveWithValidEntityField() {
        try (MockedStatic<ModelProvider> mockedProvider = mockStatic(ModelProvider.class)) {
            EntityNewEvent event = mock(EntityNewEvent.class);
            when(event.getTargetInstance()).thenReturn(entityField);
            when(entityField.getFieldMapping()).thenReturn("JM");
            when(entityField.getProperty()).thenReturn("");

            handler = spy(new TestableEntityFieldEventHandler());
            doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

            handler.onSave(event);
        }
    }

    /**
     * A testable implementation of the EntityFieldEventHandler class.
     * This class overrides the isValidEvent method to allow for testing
     * the behavior of the event handler.
     */
    private static class TestableEntityFieldEventHandler extends EntityFieldEventHandler {
        @Override
        public boolean isValidEvent(EntityPersistenceEvent event) {
            return super.isValidEvent(event);
        }
    }
}