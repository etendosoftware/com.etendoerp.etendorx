package com.etendoerp.etendorx.openapi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

import com.etendoerp.etendorx.data.OpenAPITab;
import com.etendoerp.openapi.data.OpenAPIRequest;
import com.etendoerp.openapi.data.OpenApiFlow;
import com.etendoerp.openapi.data.OpenApiFlowPoint;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Unit tests for the DynamicDatasourceEndpoint class.
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicDatasourceEndpointTest extends WeldBaseTest {

  public static final String EXISTING_TAG = "existingTag";
  private DynamicDatasourceEndpoint dynamicDatasourceEndpoint;

  @Mock
  private OpenAPI mockOpenAPI;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    dynamicDatasourceEndpoint = new DynamicDatasourceEndpoint();

    // Mock static methods
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);

    // Set up OpenAPI mock
    when(mockOpenAPI.getInfo()).thenReturn(new Info().description("Initial Description"));

    // Mock OBDal behavior
    OBDal mockOBDal = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    OBCriteria<OpenApiFlow> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.emptyList());
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
  }

  /**
   * Tests the isValid method with a null tag.
   */
  @Test
  public void testIsValid_NullTag() {
    assertTrue(dynamicDatasourceEndpoint.isValid(null));
  }

  /**
   * Tests the isValid method with an existing tag.
   */
  @Test
  public void testIsValid_ExistingTag() {
    // Given
    OpenApiFlow flow = mock(OpenApiFlow.class);
    when(flow.getName()).thenReturn(EXISTING_TAG);
    List<OpenApiFlow> flows = List.of(flow);
    mockedOBDal.when(() -> OBDal.getInstance().createCriteria(OpenApiFlow.class).list())
        .thenReturn(flows);

    // When
    boolean result = dynamicDatasourceEndpoint.isValid(EXISTING_TAG);

    // Then
    assertTrue(result);
  }

  /**
   * Tests the isValid method with a non-existing tag.
   */
  @Test
  public void testIsValid_NonExistingTag() {
    // Given
    OpenApiFlow flow = mock(OpenApiFlow.class);
    when(flow.getName()).thenReturn(EXISTING_TAG);
    List<OpenApiFlow> flows = List.of(flow);
    mockedOBDal.when(() -> OBDal.getInstance().createCriteria(OpenApiFlow.class).list())
        .thenReturn(flows);

    // When
    boolean result = dynamicDatasourceEndpoint.isValid("nonExistingTag");

    // Then
    assertFalse(result);
  }


  /**
   * Mocks an OpenApiFlow object for testing.
   *
   * @return a mocked OpenApiFlow object
   */
  private OpenApiFlow getMockedOpenApiFlow() {
    OpenApiFlow mockFlow = mock(OpenApiFlow.class);
    when(mockFlow.getName()).thenReturn("TestFlow");
    when(mockFlow.getDescription()).thenReturn("Description of TestFlow");

    OpenApiFlowPoint mockFlowPoint = mock(OpenApiFlowPoint.class);
    OpenAPIRequest mockRequest = getMockedRequest();
    when(mockFlowPoint.getEtapiOpenapiReq()).thenReturn(mockRequest);
    when(mockFlow.getETAPIOpenApiFlowPointList()).thenReturn(List.of(mockFlowPoint));
    OpenAPITab mockOpenAPITab = mock(OpenAPITab.class);
    Tab mockTabs = getMockedTab();
    when(mockOpenAPITab.getRelatedTabs()).thenReturn(mockTabs);
    when(mockRequest.getETRXOpenAPITabList()).thenReturn(List.of(mockOpenAPITab));

    return mockFlow;
  }

  /**
   * Mocks a Tab object for testing.
   *
   * @return a mocked Tab object
   */
  private Tab getMockedTab() {
    Tab mockTab = mock(Tab.class);
    Field mockedField = getMockedField();
    when(mockTab.getADFieldList()).thenReturn(List.of(mockedField));
    return mockTab;
  }


  /**
   * Mocks an OpenAPIRequest object for testing.
   *
   * @return a mocked OpenAPIRequest object
   */
  private @NotNull OpenAPIRequest getMockedRequest() {
    OpenAPITab mockOpenApiTab = mock(OpenAPITab.class);
    Tab mockedTab = getMockedTab();
    when(mockOpenApiTab.getRelatedTabs()).thenReturn(mockedTab);

    OpenAPIRequest mockRequest = mock(OpenAPIRequest.class);
    when(mockRequest.getName()).thenReturn("EntityName");
    when(mockRequest.getDescription()).thenReturn("Description of EntityName");
    when(mockRequest.getETRXOpenAPITabList()).thenReturn(List.of(mock(OpenAPITab.class)));
    when(mockRequest.getETRXOpenAPITabList()).thenReturn(List.of(mockOpenApiTab));
    return mockRequest;
  }

  /**
   * Mocks a Field object for testing.
   *
   * @return a mocked Field object
   */
  private Field getMockedField() {
    Column mockCol = mock(Column.class);

    Field mockField1 = mock(Field.class);
    when(mockField1.getColumn()).thenReturn(mockCol);

    return mockField1;
  }

  /**
   * Mocks an OpenApiFlowPoint object for testing.
   *
   * @return a mocked OpenApiFlowPoint object
   */
  private @NotNull OpenApiFlowPoint getMockedOpenApiFlowPoint() {
    OpenApiFlowPoint mockFlowPoint = mock(OpenApiFlowPoint.class);
    when(mockFlowPoint.isPost()).thenReturn(true);
    when(mockFlowPoint.isGet()).thenReturn(true);
    when(mockFlowPoint.isPut()).thenReturn(true);
    when(mockFlowPoint.isGetbyid()).thenReturn(true);
    return mockFlowPoint;
  }

  /**
   * Tests the fullfillDescription method to ensure it updates the OpenAPI info.
   */
  @Test
  public void testFullfillDescription_UpdatesInfo() {
    // Given
    HashMap<String, String> descriptions = new HashMap<>();
    descriptions.put("endpoint1", "Description for endpoint1");
    descriptions.put("endpoint2", "Description for endpoint2");
    AtomicBoolean addedEndpoints = new AtomicBoolean(true);

    // When
    dynamicDatasourceEndpoint.fullfillDescription(mockOpenAPI, addedEndpoints, descriptions);

    // Then
    String description = mockOpenAPI.getInfo().getDescription();
    assertTrue(StringUtils.contains(description, "Dynamic Datasource API endpoints descriptions:"));
    assertTrue(StringUtils.contains(description, "Description for endpoint1"));
    assertTrue(StringUtils.contains(description, "Description for endpoint2"));
    assertTrue(StringUtils.contains(description, "## Q Parameter:"));
  }

  /**
   * Tests the createGETEndpoint method to ensure it creates a GET endpoint.
   */
  @Test
  public void testCreateGETEndpoint() {
    // Given
    Schema<?> mockResponseSchema = mock(Schema.class);
    JSONObject mockExample = new JSONObject();

    OpenAPI myNewOpenAPI = new OpenAPI();
    dynamicDatasourceEndpoint.createGETEndpoint(myNewOpenAPI, "TestEntity", "TestTag", mockResponseSchema, mockExample);

    // Then
    assertNotNull(myNewOpenAPI.getPaths());
    assertTrue(myNewOpenAPI.getPaths().containsKey("/sws/com.etendoerp.etendorx.datasource/TestEntity"));
    assertNotNull(myNewOpenAPI.getPaths().get("/sws/com.etendoerp.etendorx.datasource/TestEntity").getGet());
  }


  /**
   * Tests the defineResponseSchema method to ensure it creates the schema.
   */
  @Test
  public void testDefineResponseSchema() {
    // Given
    List<Field> fields = List.of(getMockedField());

    // When
    Schema<?> schema = dynamicDatasourceEndpoint.defineResponseSchema(fields);

    // Then
    assertNotNull(schema.getProperties().get("status"));
    assertNotNull(schema.getProperties().get("data"));
  }
}