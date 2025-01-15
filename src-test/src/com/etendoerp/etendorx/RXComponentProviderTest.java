package com.etendoerp.etendorx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;

/**
 * Unit tests for the {@link RXComponentProvider} class.
 *
 * <p>This class verifies the behavior of the {@code RXComponentProvider} class
 * using JUnit and Mockito. The tests ensure the expected functionality of its methods.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class RXComponentProviderTest {

  @InjectMocks
  private RXComponentProvider rxComponentProvider;

  /**
   * Tests the {@code getGlobalComponentResources} method of {@link RXComponentProvider}.
   *
   * <p>This test validates that the method returns a non-null list with the expected
   * number of global component resources.</p>
   *
   * <p>Expected Result: The list of resources is not null and contains exactly one resource.</p>
   */
  @Test
  public void testGetGlobalComponentResources() {
    List<BaseComponentProvider.ComponentResource> resources = rxComponentProvider.getGlobalComponentResources();

    assertNotNull(resources);
    assertEquals(1, resources.size());
  }

  /**
   * Tests the {@code getComponent} method of {@link RXComponentProvider} when the component ID is not found.
   *
   * <p>This test ensures that the method returns {@code null} when it is unable to locate
   * a component for the provided ID and parameters.</p>
   *
   * @see RXComponentProvider#getComponent(String, Map)
   *
   * <p>Expected Result: The method returns {@code null}.</p>
   */
  @Test
  public void testGetComponentReturnsNull() {
    String componentId = "testComponentId";
    Map<String, Object> parameters = mock(Map.class);

    Component component = rxComponentProvider.getComponent(componentId, parameters);

    assertNull("The component should be null", component);
  }

}