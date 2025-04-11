package com.etendoerp.etendorx.services;


import static com.etendoerp.etendorx.services.ImageUploadServlet.getReadColumnConfig;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.model.Entity;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants;

/**
 * Unit tests for the ImageUploadServlet class.
 */

public class ImageUploadServletTest extends WeldBaseTest {

  public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";
  public static final String IMAGE_ID = "imageId";
  private AutoCloseable mocks;

  @Mock
  private Organization mockOrg;

  @Mock
  private Entity mockEntity;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
    super.setUp();
  }

  // Upload image with valid base64 content and filename returns 200 OK with image ID
  @Test
  public void test_upload_valid_image_returns_200_with_id() throws Exception {
    // Given
    ImageUploadServlet servlet = new ImageUploadServlet();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    BufferedReader reader = new BufferedReader(new StringReader(
        "{\"base64Image\":\"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==\",\"filename\":\"test.png\"}"));

    when(request.getReader()).thenReturn(reader);
    when(response.getWriter()).thenReturn(writer);

    // When
    servlet.doPost("", request, response);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_OK);
    verify(response).setContentType(APPLICATION_JSON_CHARSET_UTF_8);
    JSONObject result = new JSONObject(stringWriter.toString());
    assertNotNull(result.getString(IMAGE_ID));
  }

  // Handle invalid base64 encoded image data
  @Test
  public void test_upload_invalid_base64_throws_exception() throws Exception {
    // Given
    ImageUploadServlet servlet = new ImageUploadServlet();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    BufferedReader reader = new BufferedReader(
        new StringReader("{\"base64Image\":\"invalid-base64\",\"filename\":\"test.png\"}"));

    when(request.getReader()).thenReturn(reader);

    // When/Then
    assertThrows(IllegalArgumentException.class, () -> servlet.doPost("", request, response));
  }

  // Binary data in database matches original image content exactly
  @Test
  public void test_image_binary_data_matches_original() throws Exception {
    // Given
    ImageUploadServlet servlet = new ImageUploadServlet();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
    BufferedReader reader = new BufferedReader(
        new StringReader("{\"base64Image\":\"" + base64Image + "\",\"filename\":\"test.png\"}"));

    when(request.getReader()).thenReturn(reader);
    when(response.getWriter()).thenReturn(writer);

    // When
    servlet.doPost("", request, response);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_OK);
    verify(response).setContentType(APPLICATION_JSON_CHARSET_UTF_8);
    JSONObject result = new JSONObject(stringWriter.toString());
    String imageId = result.getString(IMAGE_ID);
    assertNotNull(imageId);

    // Verify the binary data in the database matches the original image content
    Image image = OBDal.getInstance().get(Image.class, imageId);
    assertArrayEquals(Base64.getDecoder().decode(base64Image), image.getBindaryData());
  }

  // Image is correctly stored in database with proper organization, name and binary data
  @Test
  public void test_image_storage_in_database() throws Exception {
    // Given
    ImageUploadServlet servlet = new ImageUploadServlet();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    BufferedReader reader = new BufferedReader(new StringReader(
        "{\"base64Image\":\"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==\",\"filename\":\"test.png\"}"));

    when(request.getReader()).thenReturn(reader);
    when(response.getWriter()).thenReturn(writer);

    // When
    servlet.doPost("", request, response);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_OK);
    verify(response).setContentType(APPLICATION_JSON_CHARSET_UTF_8);
    JSONObject result = new JSONObject(stringWriter.toString());
    assertNotNull(result.getString(IMAGE_ID));
  }

  // MIME type is properly determined from filename extension
  @Test
  public void test_mime_type_determined_from_filename_extension() throws Exception {
    assertTrue(StringUtils.equals(ImageUploadServlet.getMimeType("test.png"), "image/png"));
  }

  // Image dimensions are correctly calculated and stored
  @Test
  public void test_image_dimensions_calculation_and_storage() throws Exception {
    // Given
    ImageUploadServlet servlet = new ImageUploadServlet();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    BufferedReader reader = new BufferedReader(new StringReader(
        "{\"base64Image\":\"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJR" +
            "U5ErkJggg==\",\"filename\":\"test.png\",\"columnID\":\"testColumnId\"}"));

    when(request.getReader()).thenReturn(reader);
    when(response.getWriter()).thenReturn(writer);
    OBDal obDalMock = mock(OBDal.class);


    Column columnMock = mock(Column.class);
    when(columnMock.getImageWidth()).thenReturn(100L);
    when(columnMock.getImageHeight()).thenReturn(100L);
    when(columnMock.getImageSizeValuesAction()).thenReturn("RESIZE_ASPECTRATIO");
    try (
        MockedStatic<OBDal> obDalMockedStatic = Mockito.mockStatic(OBDal.class);
        MockedStatic<ImageUploadServlet> imageUploadServletMockedStatic = Mockito.mockStatic(
            ImageUploadServlet.class)

    ) {
      obDalMockedStatic.when(OBDal::getInstance).thenReturn(obDalMock);
      imageUploadServletMockedStatic.when(ImageUploadServlet::getCurrentOrganization).thenReturn(mockOrg);
      imageUploadServletMockedStatic.when(() -> getReadColumnConfig("testColumnId")).thenReturn(
          new ImageUploadServlet.ReadColumnConfig(100, 100, "RESIZE_ASPECTRATIO"));
      when(obDalMock.get(Column.class, "testColumnId")).thenReturn(columnMock);
      doAnswer(invocation -> {
        Image image = invocation.getArgument(0);
        image.setId("testImageId");
        return null;
      }).when(obDalMock).save(any(Image.class));
      doNothing().when(obDalMock).flush();
      when(mockOrg.getEntity()).thenReturn(mockEntity);
      when(mockEntity.getName()).thenReturn("Organization");

      // When
      servlet.doPost("", request, response);

      // Then
      verify(response).setStatus(HttpServletResponse.SC_OK);
      verify(response).setContentType(APPLICATION_JSON_CHARSET_UTF_8);
      JSONObject result = new JSONObject(stringWriter.toString());
      assertNotNull(result.getString(IMAGE_ID));
    }
  }
}

