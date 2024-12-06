package com.etendoerp.etendorx.events;

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
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.TestUtils;
import com.etendoerp.etendorx.data.EntityMapping;

/**
 * Test class for {@link CheckEntityInEntityMappingEventHandler}.
 * This class contains unit tests for the event handler methods to ensure correct behavior.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CheckEntityInEntityMappingEventHandlerTest {

  /**
   * A testable subclass of {@link CheckEntityInEntityMappingEventHandler} for overriding
   * methods during testing.
   */
  private static class TestableCheckEntityInEntityMappingEventHandler extends CheckEntityInEntityMappingEventHandler {
    @Override
    public boolean isValidEvent(EntityPersistenceEvent event) {
      return super.isValidEvent(event);
    }
  }

  private TestableCheckEntityInEntityMappingEventHandler handler;
  private EntityMapping entityMapping;
  private Entity mockEntity;

  /**
   * Sets up the test environment by initializing the required objects and mocks.
   */
  @Before
  public void setUp() {
    handler = new TestableCheckEntityInEntityMappingEventHandler();
    entityMapping = mock(EntityMapping.class);
    mockEntity = mock(Entity.class);
  }

  /**
   * Tests that onUpdate does not proceed if the event is not valid.
   */
  @Test
  public void testOnUpdateWhenNotValidEventShouldNotProceed() {
    EntityUpdateEvent event = mock(EntityUpdateEvent.class);
    handler = spy(new TestableCheckEntityInEntityMappingEventHandler());
    doReturn(false).when(handler).isValidEvent(any(EntityUpdateEvent.class));

    handler.onUpdate(event);

    verify(event, never()).getTargetInstance();
  }

  /**
   * Tests that onSave does not proceed if the event is not valid.
   */
  @Test
  public void testOnSaveWhenNotValidEventShouldNotProceed() {
    EntityNewEvent event = mock(EntityNewEvent.class);
    handler = spy(new TestableCheckEntityInEntityMappingEventHandler());
    doReturn(false).when(handler).isValidEvent(any(EntityNewEvent.class));

    handler.onSave(event);

    verify(event, never()).getTargetInstance();
  }

  /**
   * Tests that onUpdate with a valid entity does not throw any exception.
   */
  @Test
  public void testOnUpdateWithValidEntityShouldNotThrowException() {
    try (MockedStatic<ModelProvider> mockedProvider = mockStatic(ModelProvider.class)) {
      EntityUpdateEvent event = mock(EntityUpdateEvent.class);
      ModelProvider providerInstance = mock(ModelProvider.class);

      when(entityMapping.getMappingEntity()).thenReturn(TestUtils.VALID_ENTITY);
      when(event.getTargetInstance()).thenReturn(entityMapping);
      mockedProvider.when(ModelProvider::getInstance).thenReturn(providerInstance);
      when(providerInstance.getEntity(eq(TestUtils.VALID_ENTITY), eq(false))).thenReturn(mockEntity);

      handler = spy(new TestableCheckEntityInEntityMappingEventHandler());
      doReturn(true).when(handler).isValidEvent(any(EntityUpdateEvent.class));

      handler.onUpdate(event);
    }
  }

  /**
   * Tests that onSave with a valid entity does not throw any exception.
   */
  @Test
  public void testOnSaveWithValidEntityShouldNotThrowException() {
    try (MockedStatic<ModelProvider> mockedProvider = mockStatic(ModelProvider.class)) {
      EntityNewEvent event = mock(EntityNewEvent.class);
      ModelProvider providerInstance = mock(ModelProvider.class);

      when(entityMapping.getMappingEntity()).thenReturn(TestUtils.VALID_ENTITY);
      when(event.getTargetInstance()).thenReturn(entityMapping);
      mockedProvider.when(ModelProvider::getInstance).thenReturn(providerInstance);
      when(providerInstance.getEntity(eq(TestUtils.VALID_ENTITY), eq(false))).thenReturn(mockEntity);

      handler = spy(new TestableCheckEntityInEntityMappingEventHandler());
      doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

      handler.onSave(event);
    }
  }

  /**
   * Tests that onUpdate with an invalid entity throws an IllegalArgumentException.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testOnUpdateWithInvalidEntityShouldThrowException() {
    try (MockedStatic<ModelProvider> mockedProvider = mockStatic(ModelProvider.class);
         MockedStatic<OBMessageUtils> mockedUtils = mockStatic(OBMessageUtils.class)) {

      EntityUpdateEvent event = mock(EntityUpdateEvent.class);
      ModelProvider providerInstance = mock(ModelProvider.class);

      when(entityMapping.getMappingEntity()).thenReturn(TestUtils.INVALID_ENTITY);
      when(event.getTargetInstance()).thenReturn(entityMapping);
      mockedProvider.when(ModelProvider::getInstance).thenReturn(providerInstance);
      when(providerInstance.getEntity(eq(TestUtils.INVALID_ENTITY), eq(false))).thenReturn(null);

      mockedUtils.when(() -> OBMessageUtils.getI18NMessage(
              eq("ETRX_WrongEntityName"),
              any(String[].class)))
          .thenReturn("Entity not found");

      handler = spy(new TestableCheckEntityInEntityMappingEventHandler());
      doReturn(true).when(handler).isValidEvent(any(EntityUpdateEvent.class));

      handler.onUpdate(event);
    }
  }

  /**
   * Tests that onSave with an invalid entity throws an IllegalArgumentException.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testOnSaveWithInvalidEntityShouldThrowException() {
    try (MockedStatic<ModelProvider> mockedProvider = mockStatic(ModelProvider.class);
         MockedStatic<OBMessageUtils> mockedUtils = mockStatic(OBMessageUtils.class)) {

      EntityNewEvent event = mock(EntityNewEvent.class);
      ModelProvider providerInstance = mock(ModelProvider.class);

      when(entityMapping.getMappingEntity()).thenReturn(TestUtils.INVALID_ENTITY);
      when(event.getTargetInstance()).thenReturn(entityMapping);
      mockedProvider.when(ModelProvider::getInstance).thenReturn(providerInstance);
      when(providerInstance.getEntity(eq(TestUtils.INVALID_ENTITY), eq(false))).thenReturn(null);

      mockedUtils.when(() -> OBMessageUtils.getI18NMessage(
              eq("ETRX_WrongEntityName"),
              any(String[].class)))
          .thenReturn("Entity not found");

      handler = spy(new TestableCheckEntityInEntityMappingEventHandler());
      doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

      handler.onSave(event);
    }
  }
}
