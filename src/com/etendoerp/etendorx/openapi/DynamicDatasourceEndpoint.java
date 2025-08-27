package com.etendoerp.etendorx.openapi;

import static com.etendoerp.etendorx.utils.DataSourceUtils.getHQLColumnName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.data.OpenAPIRequestField;
import com.etendoerp.etendorx.data.OpenAPITab;
import com.etendoerp.etendorx.utils.DataSourceUtils;
import com.etendoerp.openapi.data.OpenAPIRequest;
import com.etendoerp.openapi.data.OpenApiFlow;
import com.etendoerp.openapi.data.OpenApiFlowPoint;
import com.etendoerp.openapi.model.OpenAPIEndpoint;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * This class is used to generate OpenAPI documentation for dynamic datasources.
 * It implements the OpenAPIEndpoint interface and provides the necessary methods to generate
 * OpenAPI documentation for dynamic datasources.
 */
@ApplicationScoped
public class DynamicDatasourceEndpoint implements OpenAPIEndpoint {

  public static final String Q_HELP = "## Q Parameter:\n" + "The \"q\" parameter is used to construct search queries that filter data according to specified conditions. " + "These conditions can include: " + "Equality and Inequality: Use operators like == for equality and != for inequality to match exact values. " + "Case Sensitivity: Use =c= for case-sensitive matches and =ic= for case-insensitive matches, especially useful " + "for string comparisons. " + "Range Comparisons: Use operators like >, <, >=, <= to filter data within a certain range. " + "Null Checks: Use =is=null to find records with null values and =isnot=null for non-null values. " + "String Matching: Use =sw= for \"starts with\", =ew= for \"ends with\", and =c= for \"contains\". " + "Case-insensitive versions are also available, such as =isw= and =iew=. " + "Set and Existence Checks: Use =ins= to check if a value is in a set, =nis= for not in a set, and =exists to " + "check for existence. " + "Logical operators like AND (; or and) and OR (, or or) can be used to combine multiple conditions, " + "allowing for complex queries that can filter data based on multiple criteria simultaneously. " + "This flexible querying system enables precise data retrieval tailored to specific needs." + " If a search term has spaces, it should be enclosed in simple quotes. For example, to search for a name " + "containing the words \"John Doe\", use q=name=sw='John Doe'. ";

  // Templates for summaries
  public static final String GET_SUMMARY_TEMPLATE = "Get data from %s entity";
  public static final String GET_BY_ID_SUMMARY_TEMPLATE = "Obtain a single %s record";
  public static final String POST_SUMMARY_TEMPLATE = "Create a new %s record";
  public static final String PUT_SUMMARY_TEMPLATE = "Update a %s record";

  // Templates for descriptions
  public static final String GET_DESCRIPTION_TEMPLATE = "This endpoint retrieves multiple records from the %s entity. " +
      "Use the 'q' parameter to filter data with various criteria. %s";
  public static final String GET_BY_ID_DESCRIPTION_TEMPLATE = "This endpoint retrieves a single record from the %s entity by its ID. %s";
  public static final String POST_DESCRIPTION_TEMPLATE = "This endpoint creates new record(s) in the %s entity. " +
      "Only send the fields you want to explicitly set; do not include fields whose values you do not know or " +
      "wish to leave as default. The backend will automatically populate unspecified fields with their default values. %s";
  public static final String PUT_DESCRIPTION_TEMPLATE = "This endpoint updates existing record(s) in the %s entity. " +
      "Only send fields which need changes. This endpoint works like a PATCH operation, meaning it will only update the fields you provide. %s";

  private static final Logger log = LoggerFactory.getLogger(DynamicDatasourceEndpoint.class);

  private ThreadLocal<String> requestedTag = new ThreadLocal<>();

  static final List<String> extraFields = List.of("_identifier", "$ref", "active", "creationDate", "createdBy",
      "createdBy$_identifier", "updated", "updatedBy", "updatedBy$_identifier");

  /**
   * Retrieves a list of OpenApiFlow objects.
   *
   * @return a list of OpenApiFlow objects.
   */
  public static List<OpenApiFlow> getFlows() {
    return OBDal.getInstance().createCriteria(OpenApiFlow.class).list();
  }

  /**
   * Retrieves a list of tags from the OpenApiFlow objects.
   *
   * @return a list of tags.
   */
  static List<String> getTags() {
    return getFlows().stream().map(OpenApiFlow::getName).collect(Collectors.toList());
  }

  /**
   * Checks if the provided tag is valid.
   *
   * @param tag
   *     the tag to check.
   * @return true if the tag is valid, false otherwise.
   */
  @Override
  public boolean isValid(String tag) {
    try {
      OBContext.setAdminMode();
      if (tag == null) {
        return true;
      }
      if (getTags().contains(tag)) {
        requestedTag.set(tag);
        return true;
      }
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Adds OpenAPI documentation for the dynamic datasources.
   *
   * @param openAPI
   *     the OpenAPI object to add documentation to.
   */
  @Override
  public void add(OpenAPI openAPI) {
    try {
      OBContext.setAdminMode();
      HashMap<String, String> descriptions = new HashMap<>();
      AtomicBoolean addedEndpoints = new AtomicBoolean(false);
      getFlows().forEach(flow -> {
        if (requestedTag.get() == null || StringUtils.equals(requestedTag.get(), flow.getName())) {
          OBDal.getInstance().refresh(flow);
          var endpoints = flow.getETAPIOpenApiFlowPointList();
          for (OpenApiFlowPoint endpoint : endpoints) {
            OpenAPIRequest etapiOpenapiReq = endpoint.getEtapiOpenapiReq();
            OBDal.getInstance().refresh(etapiOpenapiReq);
            if (!etapiOpenapiReq.getETRXOpenAPITabList().isEmpty()) {
              addedEndpoints.set(true);
              if (StringUtils.isNotEmpty(etapiOpenapiReq.getDescription())) {
                descriptions.put(etapiOpenapiReq.getName(), etapiOpenapiReq.getDescription());
              }
              addDefinition(openAPI, etapiOpenapiReq.getName(), etapiOpenapiReq, endpoint);
            }
          }
          Tag tag = new Tag().name(flow.getName()).description(flow.getDescription());
          if (openAPI.getTags() == null) {
            openAPI.setTags(new ArrayList<>());
          }
          openAPI.getTags().add(tag);
        }
      });
      fullfillDescription(openAPI, addedEndpoints, descriptions);
    } finally {
      requestedTag.remove();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Fulfills the description of the OpenAPI object with endpoint details and additional information.
   *
   * @param openAPI
   *     the OpenAPI object to update.
   * @param addedEndpoints
   *     an AtomicBoolean indicating if endpoints were added.
   * @param descriptions
   *     a HashMap containing endpoint descriptions.
   */
  public void fullfillDescription(OpenAPI openAPI, AtomicBoolean addedEndpoints, Map<String, String> descriptions) {
    var info = openAPI.getInfo();
    if (openAPI.getInfo() == null) {
      info = new io.swagger.v3.oas.models.info.Info();
      openAPI.setInfo(info);
    }
    StringBuilder sb = new StringBuilder();
    if (addedEndpoints.get()) {
      sb.append("## Dynamic Datasource API endpoints descriptions:\n");
      for (String key : descriptions.keySet()) {
        sb.append(String.format("### %s:%n %s%n", key, descriptions.get(key)));
      }
      // Add help for Q parameter
      sb.append(Q_HELP);
    }
    info.setDescription(String.format("%s%n%s", info.getDescription(), sb));
  }

  /**
   * Adds a definition to the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the definition to.
   * @param entityName
   *     the name of the entity.
   * @param etapiOpenapiReq
   * @param endpoint
   */
  void addDefinition(OpenAPI openAPI, String entityName, OpenAPIRequest etapiOpenapiReq, OpenApiFlowPoint endpoint) {

    String tag = etapiOpenapiReq.getName();
    OpenAPITab openAPIRXTab = etapiOpenapiReq.getETRXOpenAPITabList().get(0);
    Tab tab = openAPIRXTab.getRelatedTabs();


    // Define schemas
    Schema<?> formInitResponseSchema;
    Schema<?> formInitRequestSchema;

    JSONObject formInitJSON = new JSONObject();
    JSONObject responseJSON = new JSONObject();
    JSONObject formInitResponseExample = new JSONObject();
    try {
      List<OpenAPIRequestField> etrxOpenapiFieldList = openAPIRXTab.getEtrxOpenapiFieldList();
      HashMap<String, String> fieldDescriptions = new HashMap<>();
      for (OpenAPIRequestField opf : etrxOpenapiFieldList) {
        if (StringUtils.isNotEmpty(opf.getDescription())) {
          fieldDescriptions.put(opf.getField().getId(), opf.getDescription());
        }
      }
      boolean defaultMode = etrxOpenapiFieldList.isEmpty();
      var requestTabList = getFieldList(openAPIRXTab, defaultMode);
      formInitRequestSchema = defineFormInitRequestSchema(requestTabList, defaultMode, fieldDescriptions);
      formInitResponseSchema = defineFormInitResponseSchema(tab.getADFieldList());

      getRequestBody(responseJSON, requestTabList);
      formInitResponseExample.put(OpenAPIConstants.RESPONSE, new JSONObject());
      formInitResponseExample.getJSONObject(OpenAPIConstants.RESPONSE).put("status", 0);
      formInitResponseExample.getJSONObject(OpenAPIConstants.RESPONSE).put("data", new JSONArray());
      formInitResponseExample.getJSONObject(OpenAPIConstants.RESPONSE).getJSONArray("data").put(responseJSON);
    } catch (JSONException e) {
      throw new OBException(e);
    }

    String formInitRequestExample = formInitJSON.toString();
    List<Parameter> formInitParams = new ArrayList<>();


    if (Boolean.TRUE.equals(endpoint.isGet())) {
      createGETEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample,
          endpoint.getEtapiOpenapiReq().getGETDescription());
    }
    if (Boolean.TRUE.equals(endpoint.isGetbyid())) {
      createSingleGETEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample, formInitParams,
          formInitRequestSchema, formInitRequestExample, endpoint.getEtapiOpenapiReq().getGetbyidDescription());
    }
    if (Boolean.TRUE.equals(endpoint.isPost())) {
      createPOSTEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample, formInitParams,
          formInitRequestSchema, formInitRequestExample, endpoint.getEtapiOpenapiReq().getPostDescription());
    }
    if (Boolean.TRUE.equals(endpoint.isPut())) {
      createPUTEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample, formInitParams,
          formInitRequestSchema, formInitRequestExample, endpoint.getEtapiOpenapiReq().getPUTDescription());
    }
  }

  private static void getRequestBody(JSONObject responseJSON, List<Field> fields) throws JSONException {

    for (Field adField : fields) {
      Column column = adField.getColumn();
      if (column == null) {
        continue;
      }
      String fieldConverted = getHQLColumnName(false, column.getTable().getDBTableName(), column.getDBColumnName())[0];
      responseJSON.put(fieldConverted, "");
      if (DataSourceUtils.isReferenceToAnotherTable(column.getReference())) {
        responseJSON.put(fieldConverted + "$_identifier", "");
      }
    }

    for (String extraField : extraFields) {
      responseJSON.put(extraField, "");
    }
  }

  private static List<Field> getFieldList(OpenAPITab openAPIRXTab, boolean defaultMode) {
    try {
      OBContext.setAdminMode();
      Tab tab;
      List<OpenAPIRequestField> specifiedFields = openAPIRXTab.getEtrxOpenapiFieldList();
      List<Field> fieldList;
      if (defaultMode) {
        tab = openAPIRXTab.getRelatedTabs();
        fieldList = tab.getADFieldList();
      } else {
        fieldList = specifiedFields.stream().map(OpenAPIRequestField::getField).collect(Collectors.toList());
      }
      return fieldList;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Creates a PUT endpoint in the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the endpoint to
   * @param entityName
   *     the name of the entity
   * @param tag
   *     the tag associated with the endpoint
   * @param formInitResponseSchema
   *     the schema for the response
   * @param formInitResponseExample
   *     the example of the response
   * @param formInitParams
   *     the list of parameters for the endpoint
   * @param formInitRequestSchema
   *     the schema for the request body
   * @param formInitRequestExample
   *     the example of the request body
   * @param description
   *     custom description for the endpoint
   */
  private void createPUTEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample, List<Parameter> formInitParams, Schema<?> formInitRequestSchema,
      String formInitRequestExample, String description) {

    String customDescription = StringUtils.isNotEmpty(description) ? description : "";
    String finalDescription = String.format(PUT_DESCRIPTION_TEMPLATE, entityName, customDescription);

    // Add path parameter for ID
    List<Parameter> putParams = new ArrayList<>(formInitParams);
    putParams.add(createPathParameter("id", "Entity ID to update"));

    EndpointConfig patchConfig = new EndpointConfig.Builder()
        .tag(tag)
        .actionValue(entityName + "/{id}")
        .summary(String.format(PUT_SUMMARY_TEMPLATE, entityName))
        .description(finalDescription)
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(putParams)
        .requestBodySchema(formInitRequestSchema)
        .requestBodyExample(formInitRequestExample)
        .httpMethod(OpenAPIConstants.PUT)
        .build();

    createEndpoint(openAPI, patchConfig);
  }

  /**
   * Creates a GET endpoint for a single record in the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the endpoint to
   * @param entityName
   *     the name of the entity
   * @param tag
   *     the tag associated with the endpoint
   * @param formInitResponseSchema
   *     the schema for the response
   * @param formInitResponseExample
   *     the example of the response
   * @param formInitParams
   *     the list of parameters for the endpoint
   * @param formInitRequestSchema
   *     the schema for the request body
   * @param formInitRequestExample
   *     the example of the request body
   * @param description
   *     custom description for the endpoint
   */
  private void createSingleGETEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample, List<Parameter> formInitParams, Schema<?> formInitRequestSchema,
      String formInitRequestExample, String description) {

    String customDescription = StringUtils.isNotEmpty(description) ? description : "";
    String finalDescription = String.format(GET_BY_ID_DESCRIPTION_TEMPLATE, entityName, customDescription);

    // Add path parameter for ID
    List<Parameter> getByIdParams = new ArrayList<>(formInitParams);
    getByIdParams.add(createPathParameter("id", "Entity ID to retrieve"));

    EndpointConfig getIDConfig = new EndpointConfig.Builder()
        .tag(tag)
        .actionValue(entityName + "/{id}")
        .summary(String.format(GET_BY_ID_SUMMARY_TEMPLATE, entityName))
        .description(finalDescription)
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(getByIdParams)
        .requestBodySchema(formInitRequestSchema)
        .requestBodyExample(formInitRequestExample)
        .httpMethod(OpenAPIConstants.GET)
        .build();

    createEndpoint(openAPI, getIDConfig);
  }

  /**
   * Creates a GET endpoint in the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the endpoint to
   * @param entityName
   *     the name of the entity
   * @param tag
   *     the tag associated with the endpoint
   * @param formInitResponseSchema
   *     the schema for the response
   * @param formInitResponseExample
   *     the example of the response
   * @param description
   *     custom description for the endpoint
   */
  void createGETEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample, String description) {
    List<Parameter> getParams = new ArrayList<>();
    getParams.add(createParameter("q", false, OpenAPIConstants.STRING, "field==A6750F0D15334FB890C254369AC750A8",
        "Search parameter to retrieve filtered data with a criteria"));
    getParams.add(createParameter("_startRow", true, OpenAPIConstants.STRING, "0", "Starting row to fetch."));
    getParams.add(createParameter("_endRow", true, OpenAPIConstants.STRING, "10", "End row to fetch."));

    String customDescription = StringUtils.isNotEmpty(description) ? description : "";
    String finalDescription = String.format(GET_DESCRIPTION_TEMPLATE, entityName, customDescription);

    EndpointConfig getConfig = new EndpointConfig.Builder()
        .tag(tag)
        .actionValue(entityName)
        .summary(String.format(GET_SUMMARY_TEMPLATE, entityName))
        .description(finalDescription)
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(getParams)
        .httpMethod(OpenAPIConstants.GET)
        .build();

    createEndpoint(openAPI, getConfig);
  }

  void createPOSTEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample, List<Parameter> formInitParams, Schema<?> formInitRequestSchema,
      String formInitRequestExample) {
    createPOSTEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample, formInitParams,
        formInitRequestSchema, formInitRequestExample, null);
  }

  /**
   * Creates a POST endpoint in the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the endpoint to
   * @param entityName
   *     the name of the entity
   * @param tag
   *     the tag associated with the endpoint
   * @param formInitResponseSchema
   *     the schema for the response
   * @param formInitResponseExample
   *     the example of the response
   * @param formInitParams
   *     the list of parameters for the endpoint
   * @param formInitRequestSchema
   *     the schema for the request body
   * @param formInitRequestExample
   *     the example of the request body
   * @param description
   *     custom description for the endpoint
   */
  void createPOSTEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample, List<Parameter> formInitParams, Schema<?> formInitRequestSchema,
      String formInitRequestExample, String description) {

    String customDescription = StringUtils.isNotEmpty(description) ? description : "";
    String finalDescription = String.format(POST_DESCRIPTION_TEMPLATE, entityName, customDescription);

    EndpointConfig postConfig = new EndpointConfig.Builder()
        .tag(tag)
        .actionValue(entityName)
        .summary(String.format(POST_SUMMARY_TEMPLATE, entityName))
        .description(finalDescription)
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(formInitParams)
        .requestBodySchema(formInitRequestSchema)
        .requestBodyExample(formInitRequestExample)
        .httpMethod(OpenAPIConstants.POST)
        .build();

    createEndpoint(openAPI, postConfig);
  }

  /**
   * Checks if a field is mandatory.
   *
   * @param adField
   *     the field to check.
   * @return true if the field is mandatory, false otherwise.
   */
  private boolean isMandatory(Field adField) {
    return adField.getColumn() != null && adField.getColumn().isMandatory();
  }


  /**
   * Creates an endpoint in the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the endpoint to.
   * @param config
   *     the configuration for the endpoint.
   */
  private void createEndpoint(OpenAPI openAPI, EndpointConfig config) {

    ApiResponses apiResponses = new ApiResponses().addApiResponse("200",
        createApiResponse("Successful response.", config.getResponseSchema(),
            config.getResponseExample())).addApiResponse("400",
        new ApiResponse().description("Unsuccessful request.")).addApiResponse("500",
        new ApiResponse().description("Internal server error."));

    Operation operation = new Operation().summary(config.getSummary()).description(config.getDescription());

    if (operation.getTags() == null) {
      operation.setTags(new ArrayList<>());
    }
    operation.getTags().add(config.getTag());

    for (Parameter parameter : config.getParameters()) {
      operation.addParametersItem(parameter);
    }

    if (config.getRequestBodySchema() != null) {
      RequestBody requestBody = new RequestBody().description("Request body for " + config.getActionValue()).content(
          new Content().addMediaType("application/json",
              new MediaType().schema(config.getRequestBodySchema()).example(config.getRequestBodyExample()))).required(
          true);
      operation.setRequestBody(requestBody);
    }

    operation.responses(apiResponses);

    String path = OpenAPIConstants.BASE_PATH + config.getActionValue();
    PathItem pathItem;
    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }
    if (openAPI.getPaths().containsKey(path)) {
      pathItem = openAPI.getPaths().get(path);
    } else {
      pathItem = new PathItem();
    }

    switch (StringUtils.upperCase(config.getHttpMethod())) {
      case "GET":
        pathItem.get(operation);
        break;
      case "POST":
        pathItem.post(operation);
        break;
      case "PUT":
        pathItem.put(operation);
        break;
      default:
        throw new IllegalArgumentException("HTTP method not supported: " + config.getHttpMethod());
    }

    openAPI.getPaths().addPathItem(path, pathItem);

    addSchema(openAPI, "FormInitResponse", config.getResponseSchema());
  }

  /**
   * Creates an ApiResponse object.
   *
   * @param description
   *     the description of the response.
   * @param schema
   *     the schema of the response.
   * @param example
   *     the example of the response.
   * @return the created ApiResponse object.
   */
  private ApiResponse createApiResponse(String description, Schema<?> schema, String example) {
    return new ApiResponse().description(description).content(
        new Content().addMediaType("application/json", new MediaType().schema(schema).example(example)));
  }

  /**
   * Creates a Parameter object for path parameters.
   *
   * @param name
   *     the name of the parameter.
   * @param description
   *     the description of the parameter.
   * @return the created Parameter object.
   */
  private Parameter createPathParameter(String name, String description) {
    return new Parameter()
        .in("path")
        .name(name)
        .required(true)
        .schema(new Schema<String>().type(OpenAPIConstants.STRING))
        .description(description);
  }

  /**
   * Creates a Parameter object for query parameters.
   *
   * @param name
   *     the name of the parameter.
   * @param required
   *     whether the parameter is required.
   * @param type
   *     the type of the parameter.
   * @param example
   *     the example of the parameter.
   * @param description
   *     the description of the parameter.
   * @return the created Parameter object.
   */
  private Parameter createParameter(String name, boolean required, String type, String example, String description) {
    return new Parameter().in("query").name(name).required(required).schema(
        new Schema<String>().type(type).example(example)).description(description);
  }

  /**
   * Adds a schema to the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the schema to.
   * @param key
   *     the key for the schema.
   * @param schema
   *     the schema to add.
   */
  private void addSchema(OpenAPI openAPI, String key, Schema<?> schema) {
    if (openAPI.getComponents() == null) {
      openAPI.setComponents(new io.swagger.v3.oas.models.Components());
    }
    if (openAPI.getComponents().getSchemas() == null) {
      openAPI.getComponents().setSchemas(new HashMap<>());
    }
    if (!openAPI.getComponents().getSchemas().containsKey(key)) {
      openAPI.getComponents().addSchemas(key, schema);
    }
  }

  /**
   * Defines the schema for the form initialization request.
   *
   * @param fields
   *     the list of fields.
   * @param defaultMode
   *     whether to use default mode
   * @param fieldDescriptions
   *     descriptions for fields
   * @return the defined schema.
   */
  Schema<?> defineFormInitRequestSchema(List<Field> fields, boolean defaultMode,
      HashMap<String, String> fieldDescriptions) {
    Schema<Object> schema = new Schema<>();
    schema.type(OpenAPIConstants.OBJECT);
    List<String> required = new ArrayList<>();

    var filteredFields = defaultMode ? fields.stream().filter(this::isMandatory).collect(Collectors.toList()) : fields;

    for (Field field : filteredFields) {
      String[] info = getHQLColumnName(field.getColumn());
      String hqlname = info[0];
      String type = info[1];

      Schema fieldSchema;

      if (StringUtils.equalsIgnoreCase("Long", type) || StringUtils.equalsIgnoreCase("BigDecimal", type)) {
        fieldSchema = new Schema<>().type(OpenAPIConstants.NUMBER).example(0);
      } else if (StringUtils.equalsIgnoreCase("Boolean", type)) {
        fieldSchema = new Schema<>().type("boolean").example(false);
      } else if (StringUtils.equalsIgnoreCase("Date", type)) {
        fieldSchema = new Schema<>().type("string").format("date").example("2021-01-01");
      } else if (StringUtils.equalsIgnoreCase("Datetime", type)) {
        fieldSchema = new Schema<>().type("string").format("date-time").example("2021-01-01T00:00:00Z");
      } else { // String
        fieldSchema = new Schema<>().type(OpenAPIConstants.STRING);
        Reference ref = field.getColumn().getReference();
        if (DataSourceUtils.isReferenceToAnotherTable(ref)) {
          //the field must be a STRING of 32 alphanumeric characters
          fieldSchema.maxLength(32);
          fieldSchema.pattern("^[a-zA-Z0-9]*$");
          fieldSchema.example("");
        }
      }
      String description = fieldDescriptions.get(field.getId());
      if (StringUtils.isNotEmpty(description)) {
        fieldSchema.description(description);
      }
      schema.addProperties(hqlname, fieldSchema);

    }

    schema.required(required);

    return schema;
  }

  /**
   * Defines the schema for the form initialization response.
   *
   * @param fields
   *     the list of fields.
   * @return the defined schema.
   */
  private Schema<?> defineFormInitResponseSchema(List<Field> fields) {

    Schema<Object> completeResponseSchema = new Schema<>();
    completeResponseSchema.type(OpenAPIConstants.OBJECT);
    completeResponseSchema.description("Complete response object");

    Schema<?> responsePropertySchema = defineResponseSchema(fields);
    completeResponseSchema.addProperties(OpenAPIConstants.RESPONSE, responsePropertySchema);

    return completeResponseSchema;
  }

  /**
   * Defines the schema for a data item.
   *
   * @param fields
   *     the list of fields.
   * @return the defined schema.
   */
  private Schema<?> defineDataItemSchema(List<Field> fields) {
    Schema<Object> dataItemSchema = new Schema<>();
    dataItemSchema.type(OpenAPIConstants.OBJECT);
    dataItemSchema.description("Entity data");
    for (String extraField : extraFields) {
      dataItemSchema.addProperties(extraField, new Schema<>().type(OpenAPIConstants.STRING).example(""));
    }
    for (Field field : fields) {
      dataItemSchema.addProperties(getHQLColumnName(field.getColumn())[0],
          new Schema<>().type(OpenAPIConstants.STRING).example(""));
    }
    return dataItemSchema;
  }

  /**
   * Defines the schema for the response.
   *
   * @param fields
   *     the list of fields.
   * @return the defined schema.
   */
  Schema<?> defineResponseSchema(List<Field> fields) {
    Schema<Object> responseSchema = new Schema<>();
    responseSchema.type(OpenAPIConstants.OBJECT);
    responseSchema.description("Main object of the response");

    Schema<Integer> statusSchema = new Schema<>();
    statusSchema.type("integer").format("int32").example(0);
    responseSchema.addProperties("status", statusSchema);

    ArraySchema dataArraySchema = new ArraySchema();
    dataArraySchema.items(defineDataItemSchema(fields));
    responseSchema.addProperties("data", dataArraySchema);

    return responseSchema;
  }

}
