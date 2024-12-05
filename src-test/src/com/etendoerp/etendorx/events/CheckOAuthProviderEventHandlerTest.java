package com.etendoerp.etendorx.events;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * Test class for the {@link CheckOAuthProviderEventHandler} class. Verifies the handler's behavior during entity
 * persistence events, including:
 * - When the event is not valid, the onSave method does not throw an exception.
 * - When the event is valid and the client ID is valid, the onSave method does not throw an exception.
 * - When the event is valid and the client ID is null, the onSave method throws an OBException.
 * - The getObservedEntities method returns the expected entity.
 * - When the event is not valid, the onUpdate method does not throw an exception.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CheckOAuthProviderEventHandlerTest {

  /**
   * A testable extension of the CheckOAuthProviderEventHandler class that exposes the isValidEvent method for testing purposes.
   */
  private static class TestableCheckOAuthProviderEventHandler extends CheckOAuthProviderEventHandler {
    @Override
    public boolean isValidEvent(EntityPersistenceEvent event) {
      return super.isValidEvent(event);
    }
  }

  /**
   * Tests that the onSave method does not throw an exception when the event is not valid.
   */
  @Test
  public void testOnSaveWhenNotValidEvent() {
    ETRXoAuthProvider provider = mock(ETRXoAuthProvider.class);
    EntityNewEvent event = mock(EntityNewEvent.class);

    TestableCheckOAuthProviderEventHandler handler = spy(new TestableCheckOAuthProviderEventHandler());
    doReturn(false).when(handler).isValidEvent(any(EntityNewEvent.class));

    handler.onSave(event);

    verify(provider, never()).getIDForClient();
  }

  /**
   * Tests that the onSave method does not throw an exception when the event is valid and the client ID is valid.
   */
  @Test
  public void testOnSaveWhenValidEventAndValidClientId() {
    ETRXoAuthProvider provider = mock(ETRXoAuthProvider.class);
    when(provider.getIDForClient()).thenReturn("validClientId");

    EntityNewEvent event = mock(EntityNewEvent.class);
    when(event.getTargetInstance()).thenReturn(provider);

    TestableCheckOAuthProviderEventHandler handler = spy(new TestableCheckOAuthProviderEventHandler());
    doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

    handler.onSave(event);
  }

  /**
   * Tests that the onSave method throws an exception when the event is valid and the client ID is null.
   */
  @Test(expected = OBException.class)
  public void testOnSaveWhenValidEventAndNullClientId() {
    try (MockedStatic<OBMessageUtils> mockedUtils = mockStatic(OBMessageUtils.class)) {
      ETRXoAuthProvider provider = mock(ETRXoAuthProvider.class);
      when(provider.getIDForClient()).thenReturn(null);

      EntityNewEvent event = mock(EntityNewEvent.class);
      when(event.getTargetInstance()).thenReturn(provider);

      TestableCheckOAuthProviderEventHandler handler = spy(new TestableCheckOAuthProviderEventHandler());
      doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

      mockedUtils.when(() -> OBMessageUtils.getI18NMessage("ETRX_IDForClientNotSet"))
          .thenReturn("Client ID not set");

      handler.onSave(event);
    }
  }

  /**
   * Tests that the getObservedEntities method returns the expected entity.
   */
  @Test
  public void testGetObservedEntities() {
    Entity mockEntity = mock(Entity.class);
    when(mockEntity.getName()).thenReturn(ETRXoAuthProvider.ENTITY_NAME);

    try (MockedStatic<ModelProvider> mockedProvider = mockStatic(ModelProvider.class)) {
      ModelProvider providerInstance = mock(ModelProvider.class);
      mockedProvider.when(ModelProvider::getInstance).thenReturn(providerInstance);
      when(providerInstance.getEntity(ETRXoAuthProvider.ENTITY_NAME)).thenReturn(mockEntity);

      CheckOAuthProviderEventHandler handler = new CheckOAuthProviderEventHandler();
      Entity[] observedEntities = handler.getObservedEntities();

      assertEquals(1, observedEntities.length);
      assertEquals(ETRXoAuthProvider.ENTITY_NAME, observedEntities[0].getName());
    }
  }

  /**
   * Tests that the onUpdate method does not throw an exception when the event is not valid.
   */
  @Test
  public void testOnUpdateWhenNotValidEvent() {
    ETRXoAuthProvider provider = mock(ETRXoAuthProvider.class);
    EntityUpdateEvent event = mock(EntityUpdateEvent.class);

    TestableCheckOAuthProviderEventHandler handler = spy(new TestableCheckOAuthProviderEventHandler());
    doReturn(false).when(handler).isValidEvent(any(EntityUpdateEvent.class));

    handler.onUpdate(event);

    verify(provider, never()).getIDForClient();
  }
}