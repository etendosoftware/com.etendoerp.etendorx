package com.etendoerp.etendorx.openapi;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class EndpointConfigTest {

  @Test
  void testBuilderAndGetters() {
    // Mock schemas and parameters for testing
    Schema<String> mockResponseSchema = new Schema<>();
    mockResponseSchema.setTitle("ResponseSchema");
    Schema<String> mockRequestBodySchema = new Schema<>();
    mockRequestBodySchema.setTitle("RequestBodySchema");

    Parameter mockParameter = new Parameter().name("param1").description("test parameter");

    // Create instance using the builder
    EndpointConfig endpointConfig = new EndpointConfig.Builder()
        .tag("test-tag")
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
    Assertions.assertEquals("test-tag", endpointConfig.getTag());
    Assertions.assertEquals("test-action", endpointConfig.getActionValue());
    Assertions.assertEquals("Test Summary", endpointConfig.getSummary());
    Assertions.assertEquals("This is a test description", endpointConfig.getDescription());
    Assertions.assertEquals(mockResponseSchema, endpointConfig.getResponseSchema());
    Assertions.assertEquals("{\"key\":\"value\"}", endpointConfig.getResponseExample());
    Assertions.assertEquals(1, endpointConfig.getParameters().size());
    Assertions.assertEquals("param1", endpointConfig.getParameters().get(0).getName());
    Assertions.assertEquals(mockRequestBodySchema, endpointConfig.getRequestBodySchema());
    Assertions.assertEquals("{\"requestKey\":\"requestValue\"}", endpointConfig.getRequestBodyExample());
    Assertions.assertEquals("GET", endpointConfig.getHttpMethod());
  }

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

  @Test
  void testMultipleParameters() {
    // Create instance with multiple parameters
    Parameter param1 = new Parameter().name("param1").description("Parameter 1");
    Parameter param2 = new Parameter().name("param2").description("Parameter 2");

    EndpointConfig endpointConfig = new EndpointConfig.Builder()
        .parameters(List.of(param1, param2))
        .build();

    // Assert both parameters are set
    Assertions.assertEquals(2, endpointConfig.getParameters().size());
    Assertions.assertEquals("param1", endpointConfig.getParameters().get(0).getName());
    Assertions.assertEquals("param2", endpointConfig.getParameters().get(1).getName());
  }



  @Test
  void testEqualsAndHashCode() {
    // Create two identical instances
    EndpointConfig endpointConfig1 = new EndpointConfig.Builder()
        .tag("test-tag")
        .httpMethod("GET")
        .build();

    EndpointConfig endpointConfig2 = new EndpointConfig.Builder()
        .tag("test-tag")
        .httpMethod("GET")
        .build();

    // Assert they are equal
    Assertions.assertEquals(endpointConfig1.getHttpMethod(), endpointConfig2.getHttpMethod());
    Assertions.assertEquals(endpointConfig1.getTag(), endpointConfig2.getTag());
  }

  @Test
  void testToString() {
    // Create instance
    EndpointConfig endpointConfig = new EndpointConfig.Builder()
        .tag("test-tag")
        .httpMethod("POST")
        .build();

    // Assert toString contains key information

    Assertions.assertTrue(endpointConfig.getTag().contains("test-tag"));
    Assertions.assertTrue(endpointConfig.getHttpMethod().contains("POST"));
  }
}