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

@ApplicationScoped
public class DynamicDatasourceEndpoint implements OpenAPIEndpoint {

  private static final String BASE_PATH = "/etendo/sws/com.etendoerp.etendorx.datasource";
  private String requestedTag = null;
  private static final List<String> extraFields = List.of("_identifier", "$ref", "active",
      "creationDate", "createdBy", "createdBy$_identifier", "updated", "updatedBy",
      "updatedBy$_identifier");
  public static final String GET = "GET";
  public static final String POST = "POST";

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
          if(!endpoint.getEtapiOpenapiReq().getETRXOpenAPITabList().isEmpty()) {
            addDefinition(openAPI, flow.getName(), endpoint.getEtapiOpenapiReq().getName(), endpoint.getEtapiOpenapiReq().getETRXOpenAPITabList().get(0).getRelatedTabs());
          }
        }
        Tag tag = new Tag().name(flow.getName()).description(flow.getDescription());
        if(openAPI.getTags() == null) {
          openAPI.setTags(new ArrayList<>());
        }
        openAPI.getTags().add(tag);
      });
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void addDefinition(OpenAPI openAPI, String tag, String entityName, Tab tab) {

    // Form init
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
      formInitResponseExample.put("response", new JSONObject());
      formInitResponseExample.getJSONObject("response").put("status", 0);
      formInitResponseExample.getJSONObject("response").put("data", new JSONArray());
      formInitResponseExample.getJSONObject("response").getJSONArray("data").put(responseJSON);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }

    String formInitRequestExample = formInitJSON.toString();
    List<Parameter> formInitParams = new ArrayList<>();

    createEndpoint(openAPI, tag, entityName, "Creates a record with default values",
        "This endpoint is used to create a record with default values.", formInitResponseSchema,
        formInitResponseExample.toString(), "FormInitResponse", formInitParams,
        formInitRequestSchema, formInitRequestExample, POST);

    // Add extra params
    formInitParams.add(createParameter("_startRow", true, "string", "0", "Starting row to fetch."));
    formInitParams.add(createParameter("_endRow", true, "string", "10", "End row to fetch."));

    createEndpoint(openAPI, tag, entityName, "Get data from this entity",
        "This endpoint is used to initialize a form with default values.", formInitResponseSchema,
        formInitResponseExample.toString(), "FormInitResponse", formInitParams, null, null, GET);

  }

  private boolean isMandatory(Field adField) {
    List<String> references = List.of("");
    Column column = adField.getColumn();
    String adReferenceId = column.getReference().getId();
    boolean hasCallout = references.contains(adReferenceId) && column.isValidateOnNew() && column.getCallout() != null;
    boolean hasDefaultValue = column.getDefaultValue() != null;
    boolean isKeyColumn = column.isKeyColumn();
    return (!hasCallout && !hasDefaultValue && !isKeyColumn);
  }

  private String convertField(Field field) {
    return normalizedName(field.getColumn().getName());
  }

  private void createEndpoint(OpenAPI openAPI, String tag, String actionValue, String summary,
      String description, Schema<?> responseSchema, String responseExample, String schemaKey,
      List<Parameter> parameters, Schema<?> requestBodySchema, String requestBodyExample,
      String httpMethod) {

    ApiResponses apiResponses = new ApiResponses().addApiResponse("200",
            createApiResponse("Successful response.", responseSchema, responseExample))
        .addApiResponse("400", new ApiResponse().description("Unsuccessful request."))
        .addApiResponse("500", new ApiResponse().description("Internal server error."));

    Operation operation = new Operation().summary(summary).description(description);
    if (operation.getTags() == null) {
      operation.setTags(new ArrayList<>());
    }
    operation.getTags().add(tag);

    for (Parameter parameter : parameters) {
      operation.addParametersItem(parameter);
    }

    if (requestBodySchema != null) {
      RequestBody requestBody = new RequestBody().description(
              "Request body for request " + actionValue)
          .content(new Content().addMediaType("application/json",
              new MediaType().schema(requestBodySchema).example(requestBodyExample)))
          .required(true);
      operation.setRequestBody(requestBody);
    }

    operation.responses(apiResponses);

    String path = BASE_PATH + "/" + actionValue;
    PathItem pathItem;
    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }
    if (openAPI.getPaths().containsKey(path)) {
      pathItem = openAPI.getPaths().get(path);
    } else {
      pathItem = new PathItem();
    }

    if (StringUtils.equals(httpMethod, GET)) {
      pathItem.get(operation);
    }
    if (StringUtils.equals(httpMethod, POST)) {
      pathItem.post(operation);
    }

    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }

    if (openAPI.getPaths().containsKey(path)) {
      openAPI.getPaths().addPathItem(path, pathItem);
    } else {
      openAPI.getPaths().addPathItem(path, pathItem);
    }

    addSchema(openAPI, schemaKey, responseSchema);
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
      if(isMandatory(field)) {
        schema.addProperty(normalizedName(field.getColumn().getName()),
            new Schema<>().type("string").example("N"));
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
    completeResponseSchema.addProperty("response", responsePropertySchema);

    return completeResponseSchema;
  }

  private Schema<?> defineDataItemSchema(List<Field> fields) {
    Schema<Object> dataItemSchema = new Schema<>();
    dataItemSchema.type("object");
    dataItemSchema.description("Entity data");
    for (String extraField : extraFields) {
      dataItemSchema.addProperty(extraField, new Schema<>().type("string").example(""));
    }
    for (Field field : fields) {
      dataItemSchema.addProperty(normalizedName(field.getColumn().getName()),
          new Schema<>().type("string").example(""));
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
