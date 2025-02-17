package com.etendoerp.etendorx.openapi;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * Unit tests for the EndpointConfig class.
 */
class EndpointConfigTest {

  public static final String TEST_TAG = "test-tag";
  public static final String PARAM_1 = "param1";
  public static final String PARAM_2 = "param2";

  /**
   * Tests the builder and getter methods of the EndpointConfig class.
   */
  @Test
  void testBuilderAndGetters() {
    // Mock schemas and parameters for testing
    Schema<String> mockResponseSchema = new Schema<>();
    mockResponseSchema.setTitle("ResponseSchema");
    Schema<String> mockRequestBodySchema = new Schema<>();
    mockRequestBodySchema.setTitle("RequestBodySchema");

    Parameter mockParameter = new Parameter().name(PARAM_1).description("test parameter");

    // Create instance using the builder
    EndpointConfig endpointConfig = new EndpointConfig.Builder()
        .tag(TEST_TAG)
        .actionValue("test-action")
        .summary("Test Summary")
        .description("This is a test description")
        .responseSchema(mockResponseSchema)
        .responseExample("{\"key\":\"value\"}")
        .parameters(List.of(mockParameter))
        .requestBodySchema(mockRequestBodySchema)
        .requestBodyExample("{\"requestKey\":\"requestValue\"}")
        .httpMethod("GET")
        .build();

    // Assert the properties
    Assertions.assertEquals(TEST_TAG, endpointConfig.getTag());
    Assertions.assertEquals("test-action", endpointConfig.getActionValue());
    Assertions.assertEquals("Test Summary", endpointConfig.getSummary());
    Assertions.assertEquals("This is a test description", endpointConfig.getDescription());
    Assertions.assertEquals(mockResponseSchema, endpointConfig.getResponseSchema());
    Assertions.assertEquals("{\"key\":\"value\"}", endpointConfig.getResponseExample());
    Assertions.assertEquals(1, endpointConfig.getParameters().size());
    Assertions.assertEquals(PARAM_1, endpointConfig.getParameters().get(0).getName());
    Assertions.assertEquals(mockRequestBodySchema, endpointConfig.getRequestBodySchema());
    Assertions.assertEquals("{\"requestKey\":\"requestValue\"}", endpointConfig.getRequestBodyExample());
    Assertions.assertEquals("GET", endpointConfig.getHttpMethod());
  }

  /**
   * Tests the builder with no properties set.
   */
  @Test
  void testEmptyBuilder() {
    // Create instance with no properties set
    EndpointConfig endpointConfig = new EndpointConfig.Builder().build();

    // Assert all properties are null or empty
    Assertions.assertNull(endpointConfig.getTag());
    Assertions.assertNull(endpointConfig.getActionValue());
    Assertions.assertNull(endpointConfig.getSummary());
    Assertions.assertNull(endpointConfig.getDescription());
    Assertions.assertNull(endpointConfig.getResponseSchema());
    Assertions.assertNull(endpointConfig.getResponseExample());
    Assertions.assertNull(endpointConfig.getParameters());
    Assertions.assertNull(endpointConfig.getRequestBodySchema());
    Assertions.assertNull(endpointConfig.getRequestBodyExample());
    Assertions.assertNull(endpointConfig.getHttpMethod());
  }

  /**
   * Tests the builder with some properties explicitly set to null.
   */
  @Test
  void testBuilderWithNullValues() {
    // Create instance setting some properties to null explicitly
    EndpointConfig endpointConfig = new EndpointConfig.Builder()
        .tag(null)
        .actionValue(null)
        .summary(null)
        .parameters(null)
        .build();

    // Assert explicitly set null values
    Assertions.assertNull(endpointConfig.getTag());
    Assertions.assertNull(endpointConfig.getActionValue());
    Assertions.assertNull(endpointConfig.getSummary());
    Assertions.assertNull(endpointConfig.getParameters());
  }

  /**
   * Tests the builder with multiple parameters.
   */
  @Test
  void testMultipleParameters() {
    // Create instance with multiple parameters
    Parameter param1 = new Parameter().name(PARAM_1).description("Parameter 1");
    Parameter param2 = new Parameter().name(PARAM_2).description("Parameter 2");

    EndpointConfig endpointConfig = new EndpointConfig.Builder()
        .parameters(List.of(param1, param2))
        .build();

    // Assert both parameters are set
    Assertions.assertEquals(2, endpointConfig.getParameters().size());
    Assertions.assertEquals(PARAM_1, endpointConfig.getParameters().get(0).getName());
    Assertions.assertEquals(PARAM_2, endpointConfig.getParameters().get(1).getName());
  }

  /**
   * Tests the equals and hashCode methods.
   */
  @Test
  void testEqualsAndHashCode() {
    // Create two identical instances
    EndpointConfig endpointConfig1 = new EndpointConfig.Builder()
        .tag(TEST_TAG)
        .httpMethod("GET")
        .build();

    EndpointConfig endpointConfig2 = new EndpointConfig.Builder()
        .tag(TEST_TAG)
        .httpMethod("GET")
        .build();

    // Assert they are equal
    Assertions.assertEquals(endpointConfig1.getHttpMethod(), endpointConfig2.getHttpMethod());
    Assertions.assertEquals(endpointConfig1.getTag(), endpointConfig2.getTag());
  }

  /**
   * Tests the toString method.
   */
  @Test
  void testToString() {
    // Create instance
    EndpointConfig endpointConfig = new EndpointConfig.Builder()
        .tag(TEST_TAG)
        .httpMethod("POST")
        .build();

    // Assert toString contains key information
    Assertions.assertTrue(StringUtils.contains(endpointConfig.getTag(), TEST_TAG));
    Assertions.assertTrue(StringUtils.contains(endpointConfig.getHttpMethod(), "POST"));
  }
}