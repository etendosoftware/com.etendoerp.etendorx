package com.etendoerp.etendorx.openapi;

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
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.etendoerp.etendorx.services.DataSourceServlet.normalizedName;

/**
 * This class is used to generate OpenAPI documentation for dynamic datasources.
 * It implements the OpenAPIEndpoint interface and provides the necessary methods to generate
 * OpenAPI documentation for dynamic datasources.
 */
@ApplicationScoped
public class DynamicDatasourceEndpoint implements OpenAPIEndpoint {

  private static final String BASE_PATH = "/etendo/sws/com.etendoerp.etendorx.datasource";
  private static final List<String> extraFields = List.of("_identifier", "$ref", "active",
      "creationDate", "createdBy", "createdBy$_identifier", "updated", "updatedBy",
      "updatedBy$_identifier");
  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String RESPONSE = "response";
  public static final String STRING = "string";

  private List<OpenApiFlow> getFlows() {
    return OBDal.getInstance().createCriteria(OpenApiFlow.class).list();
  }

  private List<String> getTags() {
    return getFlows().stream().map(OpenApiFlow::getName).collect(Collectors.toList());
  }

  @Override
  public boolean isValid(String tag) {
    try {
      OBContext.setAdminMode();
      if (tag == null) {
        return true;
      }
      return getTags().contains(tag);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void add(OpenAPI openAPI) {
    try {
      OBContext.setAdminMode();
      getFlows().forEach(flow -> {
        var endpoints = flow.getETAPIOpenApiFlowPointList();
        for (OpenApiFlowPoint endpoint : endpoints) {
          if (!endpoint.getEtapiOpenapiReq().getETRXOpenAPITabList().isEmpty()) {
            addDefinition(openAPI, flow.getName(), endpoint.getEtapiOpenapiReq().getName(),
                endpoint.getEtapiOpenapiReq().getETRXOpenAPITabList().get(0).getRelatedTabs());
          }
        }
        Tag tag = new Tag().name(flow.getName()).description(flow.getDescription());
        if (openAPI.getTags() == null) {
          openAPI.setTags(new ArrayList<>());
        }
        openAPI.getTags().add(tag);
      });
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void addDefinition(OpenAPI openAPI, String tag, String entityName, Tab tab) {

    // Definir schemas
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
      formInitResponseExample.put(RESPONSE, new JSONObject());
      formInitResponseExample.getJSONObject(RESPONSE).put("status", 0);
      formInitResponseExample.getJSONObject(RESPONSE).put("data", new JSONArray());
      formInitResponseExample.getJSONObject(RESPONSE).getJSONArray("data").put(responseJSON);
    } catch (JSONException e) {
      throw new OBException(e);
    }

    String formInitRequestExample = formInitJSON.toString();
    List<Parameter> formInitParams = new ArrayList<>();

    // Crear EndpointConfig para POST usando el Builder
    EndpointConfig postConfig = new EndpointConfig.Builder().tag(tag)
        .actionValue(entityName)
        .summary("Creates a record with default values")
        .description("This endpoint is used to create a record with default values.")
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(formInitParams)
        .requestBodySchema(formInitRequestSchema)
        .requestBodyExample(formInitRequestExample)
        .httpMethod(POST)
        .build();

    createEndpoint(openAPI, postConfig);

    // Agregar parámetros adicionales
    formInitParams.add(createParameter("_startRow", true, STRING, "0", "Starting row to fetch."));
    formInitParams.add(createParameter("_endRow", true, STRING, "10", "End row to fetch."));

    // Crear EndpointConfig para GET usando el Builder
    EndpointConfig getConfig = new EndpointConfig.Builder().tag(tag)
        .actionValue(entityName)
        .summary("Get data from this entity")
        .description("This endpoint is used to initialize a form with default values.")
        .responseSchema(formInitResponseSchema)
        .responseExample(formInitResponseExample.toString())
        .parameters(formInitParams)
        .httpMethod(GET)
        .build();

    createEndpoint(openAPI, getConfig);
  }

  private boolean isMandatory(Field adField) {
    List<String> references = List.of("");
    Column column = adField.getColumn();
    String adReferenceId = column.getReference().getId();
    boolean hasCallout = references.contains(
        adReferenceId) && column.isValidateOnNew() && column.getCallout() != null;
    boolean hasDefaultValue = column.getDefaultValue() != null;
    boolean isKeyColumn = column.isKeyColumn();
    return (!hasCallout && !hasDefaultValue && !isKeyColumn);
  }

  private String convertField(Field field) {
    return normalizedName(field.getColumn().getName());
  }

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

    String path = BASE_PATH + "/" + config.getActionValue();
    PathItem pathItem;
    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }
    if (openAPI.getPaths().containsKey(path)) {
      pathItem = openAPI.getPaths().get(path);
    } else {
      pathItem = new PathItem();
    }

    switch (config.getHttpMethod().toUpperCase()) {
      case "GET":
        pathItem.get(operation);
        break;
      case "POST":
        pathItem.post(operation);
        break;
      default:
        throw new IllegalArgumentException("HTTP method not supported: " + config.getHttpMethod());
    }

    openAPI.getPaths().addPathItem(path, pathItem);

    addSchema(openAPI, "FormInitResponse", config.getResponseSchema());
  }

  private ApiResponse createApiResponse(String description, Schema<?> schema, String example) {
    return new ApiResponse().description(description)
        .content(new Content().addMediaType("application/json",
            new MediaType().schema(schema).example(example)));
  }

  private Parameter createParameter(String name, boolean required, String type, String example,
      String description) {
    return new Parameter().in("query")
        .name(name)
        .required(required)
        .schema(new Schema<String>().type(type).example(example))
        .description(description);
  }

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

  private Schema<?> defineFormInitRequestSchema(List<Field> fields) {
    Schema<Object> schema = new Schema<>();
    schema.type("object");
    List<String> required = new ArrayList<>();

    for (Field field : fields) {
      if (isMandatory(field)) {
        schema.addProperty(normalizedName(field.getColumn().getName()),
            new Schema<>().type(STRING).example("N"));
        required.add(normalizedName(field.getColumn().getName()));
      }
    }

    schema.required(required);

    return schema;
  }

  private Schema<?> defineFormInitResponseSchema(List<Field> fields) {

    Schema<Object> completeResponseSchema = new Schema<>();
    completeResponseSchema.type("object");
    completeResponseSchema.description("Complete response object");

    Schema<?> responsePropertySchema = defineResponseSchema(fields);
    completeResponseSchema.addProperty(RESPONSE, responsePropertySchema);

    return completeResponseSchema;
  }

  private Schema<?> defineDataItemSchema(List<Field> fields) {
    Schema<Object> dataItemSchema = new Schema<>();
    dataItemSchema.type("object");
    dataItemSchema.description("Entity data");
    for (String extraField : extraFields) {
      dataItemSchema.addProperty(extraField, new Schema<>().type(STRING).example(""));
    }
    for (Field field : fields) {
      dataItemSchema.addProperty(normalizedName(field.getColumn().getName()),
          new Schema<>().type(STRING).example(""));
    }
    return dataItemSchema;
  }

  private Schema<?> defineResponseSchema(List<Field> fields) {
    Schema<Object> responseSchema = new Schema<>();
    responseSchema.type("object");
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
