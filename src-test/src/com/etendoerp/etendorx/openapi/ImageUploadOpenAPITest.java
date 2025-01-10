package com.etendoerp.etendorx.openapi;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;

import org.junit.jupiter.api.Test;

import java.util.Map;

class ImageUploadOpenAPITest {

  @Test
  void testGetClasses() {
    ImageUploadOpenAPI imageUploadOpenAPI = new ImageUploadOpenAPI();
    Class<?>[] classes = imageUploadOpenAPI.getClasses();

    assertNotNull(classes);
    assertEquals(1, classes.length);
    assertEquals("com.etendoerp.etendorx.services.ImageUploadServlet", classes[0].getName());
  }

  @Test
  void testGetEndpointPath() {
    ImageUploadOpenAPI imageUploadOpenAPI = new ImageUploadOpenAPI();
    String endpointPath = imageUploadOpenAPI.getEndpointPath();

    assertNotNull(endpointPath);
    assertEquals("/sws/com.etendoerp.etendorx.imageUpload/", endpointPath);
  }

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
    assertTrue(content.containsKey("application/json"));

    Schema<?> schema = content.get("application/json").getSchema();
    assertNotNull(schema);

    // Verify required properties
    assertNotNull(schema.getProperties());
    Map<String, Schema> properties = schema.getProperties();
    assertTrue(properties.containsKey("filename"));
    assertTrue(properties.containsKey("columnId"));
    assertTrue(properties.containsKey("base64Image"));

    // Check specific properties
    Schema<?> filenameSchema = properties.get("filename");
    assertEquals("The name of the file", filenameSchema.getDescription());
    assertEquals("image.jpg", filenameSchema.getExample());

    Schema<?> columnIdSchema = properties.get("columnId");
    assertEquals("The column ID where the size and resize configuration is stored", columnIdSchema.getDescription());
    assertEquals(ImageUploadOpenAPI.ETENDO_ID_PATTERN, columnIdSchema.getPattern());

    Schema<?> base64ImageSchema = properties.get("base64Image");
    assertEquals("The base64 encoded image", base64ImageSchema.getDescription());

    // Check required fields
    assertNotNull(schema.getRequired());
    assertTrue(schema.getRequired().contains("filename"));
    assertTrue(schema.getRequired().contains("base64Image"));
  }

  @Test
  void testETENDO_ID_PATTERN() {
    assertNotNull(ImageUploadOpenAPI.ETENDO_ID_PATTERN);
    assertEquals("^[0-9a-fA-F]{1,32}$", ImageUploadOpenAPI.ETENDO_ID_PATTERN);
  }


  @Test
  void POSTEndpointRequestBodySchemaBase64ImagePropertyHasCorrectDescription() {
    ImageUploadOpenAPI imageUploadOpenAPI = new ImageUploadOpenAPI();
    Operation postEndpoint = imageUploadOpenAPI.getPOSTEndpoint();

    RequestBody requestBody = postEndpoint.getRequestBody();
    Content content = requestBody.getContent();
    Schema<?> schema = content.get("application/json").getSchema();
    assertNotNull(schema);

    Schema<?> base64ImageSchema = schema.getProperties().get("base64Image");
    assertEquals("The base64 encoded image", base64ImageSchema.getDescription());
  }

}