package com.etendoerp.etendorx.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;

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
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.etendoerp.etendorx.data.OpenAPITab;
import com.etendoerp.openapi.data.OpenAPIRequest;
import com.etendoerp.openapi.data.OpenApiFlow;
import com.etendoerp.openapi.data.OpenApiFlowPoint;

@RunWith(MockitoJUnitRunner.class)
public class DynamicDatasourceEndpointTest extends WeldBaseTest {

  private DynamicDatasourceEndpoint dynamicDatasourceEndpoint;

  @Mock
  private OpenAPI mockOpenAPI;

  @Mock
  private Tab mockTab;

  @Mock
  private Field mockField;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;

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

  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
  }

  @Test
  public void testIsValid_NullTag() {
    assertTrue(dynamicDatasourceEndpoint.isValid(null));
  }

  @Test
  public void testIsValid_ExistingTag() {
    // Given
    OpenApiFlow flow = mock(OpenApiFlow.class);
    when(flow.getName()).thenReturn("existingTag");
    List<OpenApiFlow> flows = List.of(flow);
    mockedOBDal.when(() -> OBDal.getInstance().createCriteria(OpenApiFlow.class).list())
        .thenReturn(flows);

    // When
    boolean result = dynamicDatasourceEndpoint.isValid("existingTag");

    // Then
    assertTrue(result);
  }

  @Test
  public void testIsValid_NonExistingTag() {
    // Given
    OpenApiFlow flow = mock(OpenApiFlow.class);
    when(flow.getName()).thenReturn("existingTag");
    List<OpenApiFlow> flows = List.of(flow);
    mockedOBDal.when(() -> OBDal.getInstance().createCriteria(OpenApiFlow.class).list())
        .thenReturn(flows);

    // When
    boolean result = dynamicDatasourceEndpoint.isValid("nonExistingTag");

    // Then
    assertFalse(result);
  }

  @Test
  public void testAdd_WithFlows() {
    // Given
    OpenApiFlow mockFlow = getMockedOpenApiFlow();
    mockedOBDal.when(() -> OBDal.getInstance().createCriteria(OpenApiFlow.class).list())
        .thenReturn(List.of(mockFlow));
    // When
    OpenAPI myOpenAPI = new OpenAPI();
    dynamicDatasourceEndpoint.add(myOpenAPI);

    // Then
    assertNotNull(myOpenAPI.getTags());
    assertEquals(1, myOpenAPI.getTags().size());
    assertEquals("TestFlow", myOpenAPI.getTags().get(0).getName());
    assertTrue(myOpenAPI.getInfo().getDescription().contains("Description of EntityName"));
  }

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

  private Tab getMockedTab() {
    Tab mockTab = mock(Tab.class);
    Field mockedField = getMockedField();
    when(mockTab.getADFieldList()).thenReturn(List.of(mockedField));
    return mockTab;
  }

  @Test
  public void testAddDefinition_CreatesSchemaAndEndpoints() {
    // Given
    OpenAPIRequest mockRequest = getMockedRequest();

    OpenApiFlowPoint mockFlowPoint = getMockedOpenApiFlowPoint();


    // Mock tab fields


    // When

    OpenAPI myOpenAPi = new OpenAPI();
    dynamicDatasourceEndpoint.addDefinition(myOpenAPi, "EntityName", mockRequest, mockFlowPoint);

    // Then
    assertNotNull(myOpenAPi.getPaths());
    assertTrue(myOpenAPi.getPaths().containsKey("/sws/com.etendoerp.etendorx.datasource/EntityName"));
    assertTrue(myOpenAPi.getPaths().containsKey("/sws/com.etendoerp.etendorx.datasource/EntityName/{id}"));
    assertNotNull(myOpenAPi.getComponents().getSchemas().get("FormInitResponse"));
  }

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

  private Field getMockedField() {
    Reference mockRef = mock(Reference.class);
    when(mockRef.getId()).thenReturn("TestRefId");

    Column mockCol = mock(Column.class);
    when(mockCol.isMandatory()).thenReturn(true);
    when(mockCol.getName()).thenReturn("TestColumn");
    when(mockCol.getReference()).thenReturn(mockRef);

    Field mockField1 = mock(Field.class);
    when(mockField1.getColumn()).thenReturn(mockCol);


    return mockField1;
  }

  private @NotNull OpenApiFlowPoint getMockedOpenApiFlowPoint() {
    OpenApiFlowPoint mockFlowPoint = mock(OpenApiFlowPoint.class);
    when(mockFlowPoint.isPost()).thenReturn(true);
    when(mockFlowPoint.isGet()).thenReturn(true);
    when(mockFlowPoint.isPut()).thenReturn(true);
    when(mockFlowPoint.isGetbyid()).thenReturn(true);
    return mockFlowPoint;
  }

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
    assertTrue(description.contains("Dynamic Datasource API endpoints descriptions:"));
    assertTrue(description.contains("Description for endpoint1"));
    assertTrue(description.contains("Description for endpoint2"));
    assertTrue(description.contains("## Q Parameter:"));
  }

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

  @Test
  public void testDefineFormInitRequestSchema_CreatesSchema() {
    // Given
    Field mockedField = getMockedField();

    List<Field> fields = List.of(mockedField);

    // When
    Schema<?> schema = dynamicDatasourceEndpoint.defineFormInitRequestSchema(fields);

    // Then
    assertNotNull(schema.getProperties().get("testColumn"));
  }

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
