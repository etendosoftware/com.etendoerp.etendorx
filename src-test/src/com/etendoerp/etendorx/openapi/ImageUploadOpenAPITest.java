package com.etendoerp.etendorx.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;

/**
 * Unit tests for the ImageUploadOpenAPI class.
 */
class ImageUploadOpenAPITest {

  public static final String APPLICATION_JSON = "application/json";
  public static final String BASE_64_IMAGE = "base64Image";
  public static final String FILENAME = "filename";

  /**
   * Tests the getClasses method to ensure it returns the expected classes.
   */
  @Test
  void testGetClasses() {
    ImageUploadOpenAPI imageUploadOpenAPI = new ImageUploadOpenAPI();
    Class<?>[] classes = imageUploadOpenAPI.getClasses();

    assertNotNull(classes);
    assertEquals(1, classes.length);
    assertEquals("com.etendoerp.etendorx.services.ImageUploadServlet", classes[0].getName());
  }

  /**
   * Tests the getEndpointPath method to ensure it returns the correct endpoint path.
   */
  @Test
  void testGetEndpointPath() {
    ImageUploadOpenAPI imageUploadOpenAPI = new ImageUploadOpenAPI();
    String endpointPath = imageUploadOpenAPI.getEndpointPath();

    assertNotNull(endpointPath);
    assertEquals("/sws/com.etendoerp.etendorx.imageUpload/", endpointPath);
  }

  /**
   * Tests the getPOSTEndpoint method to ensure it returns the correct POST endpoint configuration.
   */
  @Test
  void testGetPOSTEndpoint() {
    ImageUploadOpenAPI imageUploadOpenAPI = new ImageUploadOpenAPI();
    Operation postEndpoint = imageUploadOpenAPI.getPOSTEndpoint();

    // Check Operation metadata
    assertNotNull(postEndpoint);
    assertEquals("Upload an image to EtendoERP", postEndpoint.getSummary());
    assertTrue(postEndpoint.getDescription().contains("Upload an image"));

    // Check RequestBody
    RequestBody requestBody = postEndpoint.getRequestBody();
    assertNotNull(requestBody);

    Content content = requestBody.getContent();
    assertNotNull(content);
    assertTrue(content.containsKey(APPLICATION_JSON));

    Schema<?> schema = content.get(APPLICATION_JSON).getSchema();
    assertNotNull(schema);

    // Verify required properties
    assertNotNull(schema.getProperties());
    Map<String, Schema> properties = schema.getProperties();
    assertTrue(properties.containsKey(FILENAME));
    assertTrue(properties.containsKey("columnId"));
    assertTrue(properties.containsKey(BASE_64_IMAGE));

    // Check specific properties
    Schema<?> filenameSchema = properties.get(FILENAME);
    assertEquals("The name of the file", filenameSchema.getDescription());
    assertEquals("image.jpg", filenameSchema.getExample());

    Schema<?> columnIdSchema = properties.get("columnId");
    assertEquals("The column ID where the size and resize configuration is stored", columnIdSchema.getDescription());
    assertEquals(ImageUploadOpenAPI.ETENDO_ID_PATTERN, columnIdSchema.getPattern());

    Schema<?> base64ImageSchema = properties.get(BASE_64_IMAGE);
    assertEquals("The base64 encoded image", base64ImageSchema.getDescription());

    // Check required fields
    assertNotNull(schema.getRequired());
    assertTrue(schema.getRequired().contains(FILENAME));
    assertTrue(schema.getRequired().contains(BASE_64_IMAGE));
  }

  /**
   * Tests that the base64Image property in the POST endpoint request body schema has the correct description.
   */
  @Test
  void postEndpointRequestBodySchemaBase64ImagePropertyHasCorrectDescription() {
    ImageUploadOpenAPI imageUploadOpenAPI = new ImageUploadOpenAPI();
    Operation postEndpoint = imageUploadOpenAPI.getPOSTEndpoint();

    RequestBody requestBody = postEndpoint.getRequestBody();
    Content content = requestBody.getContent();
    Schema<?> schema = content.get(APPLICATION_JSON).getSchema();
    assertNotNull(schema);

    Schema<?> base64ImageSchema = schema.getProperties().get(BASE_64_IMAGE);
    assertEquals("The base64 encoded image", base64ImageSchema.getDescription());
  }

}