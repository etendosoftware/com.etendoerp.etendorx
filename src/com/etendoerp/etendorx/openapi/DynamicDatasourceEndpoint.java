package com.etendoerp.etendorx.openapi;

import com.etendoerp.etendorx.data.OpenAPITab;
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

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

import javax.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.etendoerp.etendorx.services.DataSourceServlet.normalizedName;

/**
 * This class is used to generate OpenAPI documentation for dynamic datasources.
 * It implements the OpenAPIEndpoint interface and provides the necessary methods to generate
 * OpenAPI documentation for dynamic datasources.
 */
@ApplicationScoped
public class DynamicDatasourceEndpoint implements OpenAPIEndpoint {

  public static final String Q_HELP = "## Q Parameter:\n"
      + "The \"q\" parameter is used to construct search queries that filter data according to specified conditions. "
      + "These conditions can include: "
      + "Equality and Inequality: Use operators like == for equality and != for inequality to match exact values. "
      + "Case Sensitivity: Use =c= for case-sensitive matches and =ic= for case-insensitive matches, especially useful "
      + "for string comparisons. "
      + "Range Comparisons: Use operators like >, <, >=, <= to filter data within a certain range. "
      + "Null Checks: Use =is=null to find records with null values and =isnot=null for non-null values. "
      + "String Matching: Use =sw= for \"starts with\", =ew= for \"ends with\", and =c= for \"contains\". "
      + "Case-insensitive versions are also available, such as =isw= and =iew=. "
      + "Set and Existence Checks: Use =ins= to check if a value is in a set, =nis= for not in a set, and =exists to "
      + "check for existence. "
      + "Logical operators like AND (; or and) and OR (, or or) can be used to combine multiple conditions, "
      + "allowing for complex queries that can filter data based on multiple criteria simultaneously. "
      + "This flexible querying system enables precise data retrieval tailored to specific needs."
      + " If a search term has spaces, it should be enclosed in simple quotes. For example, to search for a name "
      + "containing the words \"John Doe\", use q=name=sw='John Doe'. ";

  private ThreadLocal<String> requestedTag = new ThreadLocal<>();

  private static final List<String> extraFields = List.of("_identifier", "$ref", "active",
      "creationDate", "createdBy", "createdBy$_identifier", "updated", "updatedBy",
      "updatedBy$_identifier");

  /**
   * Retrieves a list of OpenApiFlow objects.
   *
   * @return a list of OpenApiFlow objects.
   */
  private List<OpenApiFlow> getFlows() {
    return OBDal.getInstance().createCriteria(OpenApiFlow.class).list();
  }

  /**
   * Retrieves a list of tags from the OpenApiFlow objects.
   *
   * @return a list of tags.
   */
  private List<String> getTags() {
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
          var endpoints = flow.getETAPIOpenApiFlowPointList();
          for (OpenApiFlowPoint endpoint : endpoints) {
            OpenAPIRequest etapiOpenapiReq = endpoint.getEtapiOpenapiReq();
            if (!etapiOpenapiReq.getETRXOpenAPITabList().isEmpty()) {
              addedEndpoints.set(true);
              if (StringUtils.isNotEmpty(etapiOpenapiReq.getDescription())) {
                descriptions.put(etapiOpenapiReq.getName(), etapiOpenapiReq.getDescription());
              }
              addDefinition(openAPI, etapiOpenapiReq.getName(),
                  etapiOpenapiReq, endpoint);
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
  private void fullfillDescription(OpenAPI openAPI, AtomicBoolean addedEndpoints,
      HashMap<String, String> descriptions) {
    var info = openAPI.getInfo();
    StringBuilder sb = new StringBuilder();
    if (addedEndpoints.get()) {
      sb.append("## Dynamic Datasource API endpoints descriptions:\n");
      for (String key : descriptions.keySet()) {
        sb.append(String.format("### %s:\n %s\n", key, descriptions.get(key)));
      }
      // Add help for Q parameter
      sb.append(Q_HELP);
    }
    info.setDescription(String.format("%s\n%s", info.getDescription(), sb.toString()));
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
  private void addDefinition(OpenAPI openAPI, String entityName, OpenAPIRequest etapiOpenapiReq,
      OpenApiFlowPoint endpoint) {

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
      formInitRequestSchema = defineFormInitRequestSchema(tab.getADFieldList());
      formInitResponseSchema = defineFormInitResponseSchema(tab.getADFieldList());

      for (Field adField : tab.getADFieldList()) {
        String fieldConverted = convertField(adField);
        responseJSON.put(fieldConverted, "");
        if (StringUtils.equals(adField.getColumn().getReference().getId(), "19")) {
          responseJSON.put(fieldConverted + "$_identifier", "");
        }
      }

      for (String extraField : extraFields) {
        responseJSON.put(extraField, "");
      }
      formInitResponseExample.put(OpenAPIConstants.RESPONSE, new JSONObject());
      formInitResponseExample.getJSONObject(OpenAPIConstants.RESPONSE).put("status", 0);
      formInitResponseExample.getJSONObject(OpenAPIConstants.RESPONSE).put("data", new JSONArray());
      formInitResponseExample.getJSONObject(OpenAPIConstants.RESPONSE)
          .getJSONArray("data")
          .put(responseJSON);
    } catch (JSONException e) {
      throw new OBException(e);
    }

    String formInitRequestExample = formInitJSON.toString();
    List<Parameter> formInitParams = new ArrayList<>();

    if (endpoint.isPost()) {
      createPOSTEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample, formInitParams,
          formInitRequestSchema, formInitRequestExample);
    }
    if (endpoint.isGet()) {
      createGETEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample);
    }
    if (endpoint.isGetbyid()) {
      createSingleGETEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample, formInitParams,
          formInitRequestSchema, formInitRequestExample);
    }

    if (endpoint.isPut()) {
      createPUTEndpoint(openAPI, entityName, tag, formInitResponseSchema, formInitResponseExample, formInitParams,
          formInitRequestSchema, formInitRequestExample);
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
   */
  private void createPUTEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample, List<Parameter> formInitParams, Schema<?> formInitRequestSchema,
      String formInitRequestExample) {
    EndpointConfig patchConfig = new EndpointConfig.Builder().tag(tag)
        .actionValue(entityName + "/{id}")
        .summary("Save a single record")
        .description(
            "This endpoint is used to save record data. Only send fields which needs changes.")
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(formInitParams)
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
   */
  private void createSingleGETEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample, List<Parameter> formInitParams, Schema<?> formInitRequestSchema,
      String formInitRequestExample) {
    // GET of a single record
    EndpointConfig getIDConfig = new EndpointConfig.Builder().tag(tag)
        .actionValue(entityName + "/{id}")
        .summary("Obtain a single record")
        .description("This endpoint is used to obtain a single record.")
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(formInitParams)
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
   */
  private void createGETEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample) {
    List<Parameter> getParams = new ArrayList<>();
    getParams.add(createParameter("q", false, OpenAPIConstants.STRING,
        "field==A6750F0D15334FB890C254369AC750A8",
        "Search parameter to retrieve filtered data with a criteria"));
    getParams.add(
        createParameter("_startRow", true, OpenAPIConstants.STRING, "0", "Starting row to fetch."));
    getParams.add(
        createParameter("_endRow", true, OpenAPIConstants.STRING, "10", "End row to fetch."));

    // Create EndpointConfig for GET using the Builder
    EndpointConfig getConfig = new EndpointConfig.Builder().tag(tag)
        .actionValue(entityName)
        .summary("Get data from this entity")
        .description(
            "This endpoint is used to initialize a form with default values. This endpoint uses a q parameter to filter data.")
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(getParams)
        .httpMethod(OpenAPIConstants.GET)
        .build();

    createEndpoint(openAPI, getConfig);
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
   */
  private void createPOSTEndpoint(OpenAPI openAPI, String entityName, String tag, Schema<?> formInitResponseSchema,
      JSONObject formInitResponseExample, List<Parameter> formInitParams, Schema<?> formInitRequestSchema,
      String formInitRequestExample) {
    StringBuilder postDescription = new StringBuilder();
    postDescription.append("When using this POST endpoint, only send the fields you want to ");
    postDescription.append("explicitly set; do not include fields whose values you do not know or");
    postDescription.append(" wish to leave as default. The backend will automatically populate ");
    postDescription.append(" unspecified fields with their default values. For example, if the ");
    postDescription.append("resource supports fields like name, age (default: 18), and city ");
    postDescription.append("default: “Unknown”), and you only want to set name, your request ");
    postDescription.append("should be { \"name\": \"John Doe\" }. Avoid sending null values for ");
    postDescription.append("unknown fields or including unnecessary fields, such as { \"name\": ");
    postDescription.append("\"John Doe\", \"age\": null, \"city\": null }, as this could override");
    postDescription.append("default values. The backend will respond with the complete object, ");
    postDescription.append("including defaults for fields you did not specify.");

    // Create EndpointConfig for POST using the Builder
    EndpointConfig postConfig = new EndpointConfig.Builder().tag(tag)
        .actionValue(entityName)
        .summary("Creates a record with default values")
        .description(postDescription.toString())
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
    return adField.getColumn().isMandatory();
  }

  /**
   * Converts a field to a normalized name.
   *
   * @param field
   *     the field to convert.
   * @return the normalized name of the field.
   */
  private String convertField(Field field) {
    return normalizedName(field.getColumn().getName());
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
                config.getResponseExample()))
        .addApiResponse("400", new ApiResponse().description("Unsuccessful request."))
        .addApiResponse("500", new ApiResponse().description("Internal server error."));

    Operation operation = new Operation().summary(config.getSummary())
        .description(config.getDescription());

    if (operation.getTags() == null) {
      operation.setTags(new ArrayList<>());
    }
    operation.getTags().add(config.getTag());

    for (Parameter parameter : config.getParameters()) {
      operation.addParametersItem(parameter);
    }

    if (config.getRequestBodySchema() != null) {
      RequestBody requestBody = new RequestBody().description(
              "Request body for " + config.getActionValue())
          .content(new Content().addMediaType("application/json",
              new MediaType().schema(config.getRequestBodySchema())
                  .example(config.getRequestBodyExample())))
          .required(true);
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

    switch (StringUtils.toRootUpperCase(config.getHttpMethod())) {
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
    return new ApiResponse().description(description)
        .content(new Content().addMediaType("application/json",
            new MediaType().schema(schema).example(example)));
  }

  /**
   * Creates a Parameter object.
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
  private Parameter createParameter(String name, boolean required, String type, String example,
      String description) {
    return new Parameter().in("query")
        .name(name)
        .required(required)
        .schema(new Schema<String>().type(type).example(example))
        .description(description);
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
   * @return the defined schema.
   */
  private Schema<?> defineFormInitRequestSchema(List<Field> fields) {
    Schema<Object> schema = new Schema<>();
    schema.type(OpenAPIConstants.OBJECT);
    List<String> required = new ArrayList<>();

    for (Field field : fields) {
      if (isMandatory(field)) {
        Set<String> numberReferences = Set.of("22", "29","800008");
        boolean isNumber = numberReferences.contains(field.getColumn().getReference().getId());
        schema.addProperty(normalizedName(field.getColumn().getName()),
            isNumber ? new Schema<>().type(OpenAPIConstants.NUMBER).example(0) :
                new Schema<>().type(OpenAPIConstants.STRING).example("N")
        );
      }
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
    completeResponseSchema.addProperty(OpenAPIConstants.RESPONSE, responsePropertySchema);

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
      dataItemSchema.addProperty(extraField,
          new Schema<>().type(OpenAPIConstants.STRING).example(""));
    }
    for (Field field : fields) {
      dataItemSchema.addProperty(normalizedName(field.getColumn().getName()),
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
  private Schema<?> defineResponseSchema(List<Field> fields) {
    Schema<Object> responseSchema = new Schema<>();
    responseSchema.type(OpenAPIConstants.OBJECT);
    responseSchema.description("Main object of the response");

    Schema<Integer> statusSchema = new Schema<>();
    statusSchema.type("integer").format("int32").example(0);
    responseSchema.addProperty("status", statusSchema);

    ArraySchema dataArraySchema = new ArraySchema();
    dataArraySchema.items(defineDataItemSchema(fields));
    responseSchema.addProperty("data", dataArraySchema);

    return responseSchema;
  }

}
