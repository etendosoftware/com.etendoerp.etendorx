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
import com.etendoerp.etendorx.data.ETRXProjection;

/**
 * Unit tests for the {@link ProjectionEventHandler} class.
 * These tests validate the functionality of event handling and projection name validation logic.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ProjectionEventHandlerTest {


  /**
   * Testable subclass of {@link ProjectionEventHandler} for unit testing purposes.
   */
  private static class TestableProjectionEventHandler extends ProjectionEventHandler {
    @Override
    public boolean isValidEvent(EntityPersistenceEvent event) {
      return super.isValidEvent(event);
    }
  }

  private TestableProjectionEventHandler handler;
  private ETRXProjection projection;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    handler = new TestableProjectionEventHandler();
    projection = mock(ETRXProjection.class);
  }

  /**
   * Tests {@link ProjectionEventHandler#validProjectionName(String)} with a valid name.
   */
  @Test
  public void testValidProjectionNameWithValidName() {
    String validName = TestUtils.VALID_PROJECTION_NAME;
    assertTrue(handler.validProjectionName(validName));
  }

  /**
   * Tests {@link ProjectionEventHandler#validProjectionName(String)} with a name of minimum length.
   */
  @Test
  public void testValidProjectionNameWithMinLengthName() {
    String minLengthName = "abc";
    assertTrue(handler.validProjectionName(minLengthName));
  }

  /**
   * Tests {@link ProjectionEventHandler#validProjectionName(String)} with a name of maximum length.
   */
  @Test
  public void testValidProjectionNameWithMaxLengthName() {
    String maxLengthName = "abcdefghij";
    assertTrue(handler.validProjectionName(maxLengthName));
  }

  /**
   * Tests {@link ProjectionEventHandler#validProjectionName(String)} with a null name.
   */
  @Test
  public void testValidProjectionNameWithNullName() {
    assertFalse(handler.validProjectionName(null));
  }

  /**
   * Tests {@link ProjectionEventHandler#validProjectionName(String)} with a name that is too short.
   */
  @Test
  public void testValidProjectionNameWithTooShortName() {
    String tooShortName = TestUtils.TOO_SHORT_NAME;
    assertFalse(handler.validProjectionName(tooShortName));
  }

  /**
   * Tests {@link ProjectionEventHandler#validProjectionName(String)} with a name that is too long.
   */
  @Test
  public void testValidProjectionNameWithTooLongName() {
    String tooLongName = "abcdefghijk";
    assertFalse(handler.validProjectionName(tooLongName));
  }

  /**
   * Tests {@link ProjectionEventHandler#validProjectionName(String)} with a name containing special characters.
   */
  @Test
  public void testValidProjectionNameWithSpecialCharacters() {
    String nameWithSpecialChars = "Test123";
    assertFalse(handler.validProjectionName(nameWithSpecialChars));
  }

  /**
   * Verifies that {@link ProjectionEventHandler#onUpdate(EntityUpdateEvent)} does not proceed
   * when the event is invalid.
   */
  @Test
  public void testOnUpdateWhenNotValidEventShouldNotProceed() {
    EntityUpdateEvent event = mock(EntityUpdateEvent.class);
    handler = spy(new TestableProjectionEventHandler());
    doReturn(false).when(handler).isValidEvent(any(EntityUpdateEvent.class));

    handler.onUpdate(event);

    verify(event, never()).getTargetInstance();
  }

  /**
   * Verifies that {@link ProjectionEventHandler#onSave(EntityNewEvent)} does not proceed
   * when the event is invalid.
   */
  @Test
  public void testOnSaveWhenNotValidEventShouldNotProceed() {
    EntityNewEvent event = mock(EntityNewEvent.class);
    handler = spy(new TestableProjectionEventHandler());
    doReturn(false).when(handler).isValidEvent(any(EntityNewEvent.class));

    handler.onSave(event);

    verify(event, never()).getTargetInstance();
  }

  /**
   * Verifies that {@link ProjectionEventHandler#onUpdate(EntityUpdateEvent)} does not throw an exception
   * when the projection name is valid.
   */
  @Test
  public void testOnUpdateWithValidNameShouldNotThrowException() {
    EntityUpdateEvent event = mock(EntityUpdateEvent.class);
    when(projection.getName()).thenReturn(TestUtils.VALID_PROJECTION_NAME);
    when(event.getTargetInstance()).thenReturn(projection);

    handler = spy(new TestableProjectionEventHandler());
    doReturn(true).when(handler).isValidEvent(any(EntityUpdateEvent.class));

    handler.onUpdate(event);
  }

  /**
   * Verifies that {@link ProjectionEventHandler#onSave(EntityNewEvent)} does not throw an exception
   * when the projection name is valid.
   */
  @Test
  public void testOnSaveWithValidNameShouldNotThrowException() {
    EntityNewEvent event = mock(EntityNewEvent.class);
    when(projection.getName()).thenReturn(TestUtils.VALID_PROJECTION_NAME);
    when(event.getTargetInstance()).thenReturn(projection);

    handler = spy(new TestableProjectionEventHandler());
    doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

    handler.onSave(event);
  }

  /**
   * Verifies that {@link ProjectionEventHandler#onUpdate(EntityUpdateEvent)} throws an {@link OBException}
   * when the projection name is invalid.
   */
  @Test(expected = OBException.class)
  public void testOnUpdateWithInvalidNameShouldThrowException() {
    try (MockedStatic<OBMessageUtils> mockedUtils = mockStatic(OBMessageUtils.class)) {
      EntityUpdateEvent event = mock(EntityUpdateEvent.class);
      when(projection.getName()).thenReturn(TestUtils.TOO_SHORT_NAME);
      when(event.getTargetInstance()).thenReturn(projection);

      mockedUtils.when(() -> OBMessageUtils.getI18NMessage(
              eq(ProjectionEventHandler.INVALID_PROJECTION_MESSAGE),
              any(String[].class)))
          .thenReturn("Invalid projection name");

      handler = spy(new TestableProjectionEventHandler());
      doReturn(true).when(handler).isValidEvent(any(EntityUpdateEvent.class));

      handler.onUpdate(event);
    }
  }

  /**
   * Verifies that {@link ProjectionEventHandler#onSave(EntityNewEvent)} throws an {@link OBException}
   * when the projection name is invalid.
   */
  @Test(expected = OBException.class)
  public void testOnSaveWithInvalidNameShouldThrowException() {
    try (MockedStatic<OBMessageUtils> mockedUtils = mockStatic(OBMessageUtils.class)) {
      EntityNewEvent event = mock(EntityNewEvent.class);
      when(projection.getName()).thenReturn("123");
      when(event.getTargetInstance()).thenReturn(projection);

      mockedUtils.when(() -> OBMessageUtils.getI18NMessage(
              eq(ProjectionEventHandler.INVALID_PROJECTION_MESSAGE),
              any(String[].class)))
          .thenReturn("Invalid projection name");

      handler = spy(new TestableProjectionEventHandler());
      doReturn(true).when(handler).isValidEvent(any(EntityNewEvent.class));

      handler.onSave(event);
    }
  }
}