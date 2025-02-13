package com.etendoerp.etendorx.init;

import com.etendoerp.openapi.model.OpenAPIEndpoint;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class InitialOrganizationSetupEndpoint implements OpenAPIEndpoint {

  private static final String BASE_PATH = "/ad_forms/InitialOrgSetup.html?stateless=true";
  private static final List<String> tags = Arrays.asList("Initial Setup");
  private static final String TYPE_STRING = "string";

  @Override
  public boolean isValid(String tag) {
    if(tag == null) {
      return true;
    }
    return tags.contains(tag);
  }

  @Override
  public void add(OpenAPI openAPI) {
    Schema<?> initialOrgSetupRequestSchema = defineInitialOrgSetupRequestSchema();
    String initialOrgSetupRequestExample = "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"Command\"\r\n\r\n" +
        "OK\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpLastFieldChanged\"\r\n\r\n\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpOrganization\"\r\n\r\n" +
        "Org\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpOrgUser\"\r\n\r\n" +
        "Client\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpPassword\"\r\n\r\n" +
        "admin\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpConfirmPassword\"\r\n\r\n" +
        "admin\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpOrgType\"\r\n\r\n" +
        "3\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpParentOrg\"\r\n\r\n" +
        "0\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpcLocationId\"\r\n\r\n\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpcLocationId_R\"\r\n\r\n\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpCurrency\"\r\n\r\n\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpTreeClass\"\r\n\r\n" +
        "org.openbravo.erpCommon.modules.ModuleReferenceDataOrgTree\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpNodeId\"\r\n\r\n" +
        "7BFA8FF057AB46CAAB2FAAED8B870E32\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7\r\n" +
        "Content-Disposition: form-data; name=\"inpLevel\"\r\n\r\n\r\n" +
        "------WebKitFormBoundaryGFUPc5UZTjrBzWc7--\r\n";

    Schema<?> initialOrgSetupResponseSchema = defineInitialOrgSetupResponseSchema();
    String initialOrgSetupResponseExample = "<html>Initial Organization Setup Configuration</html>";

    List<Parameter> commonHeaders = Arrays.asList(
        createHeaderParameter("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryGFUPc5UZTjrBzWc7", TYPE_STRING, true, "Indicates the media type of the resource."),
        createHeaderParameter("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36", TYPE_STRING, false, "The user agent string of the user agent.")
    );


    // Create endpoint for InitialOrgSetup - POST
    createInitialOrgSetupEndpoint(openAPI,
        "InitialOrgSetup",
        "Handles the initial organization setup form submission",
        "This endpoint processes the initial organization setup form submission, including organization credentials and configuration parameters.",
        initialOrgSetupRequestSchema,
        initialOrgSetupRequestExample,
        initialOrgSetupResponseSchema,
        initialOrgSetupResponseExample,
        commonHeaders, tags,
        "POST");
  }

  /**
   * Method to create the endpoint related to Initial Org Setup
   */
  private void createInitialOrgSetupEndpoint(OpenAPI openAPI, String actionName, String summary, String description,
      Schema<?> requestSchema, String requestExample,
      Schema<?> responseSchema, String responseExample,
      List<Parameter> headers, List<String> tags, String httpMethod) {

    // Define query parameters if any
    // In this case, there are no query parameters in the provided request

    List<Parameter> queryParameters = Collections.emptyList();

    // Define the request body
    RequestBody requestBody = new RequestBody()
        .description("Payload for executing the Initial Organization Setup action.")
        .content(new Content().addMediaType("multipart/form-data",
            new MediaType().schema(requestSchema).example(requestExample)))
        .required(true);

    // Define API responses
    ApiResponses apiResponses = new ApiResponses()
        .addApiResponse("200", createApiResponse("Successful response.", responseSchema, responseExample))
        .addApiResponse("400", new ApiResponse().description("Bad Request."))
        .addApiResponse("500", new ApiResponse().description("Internal Server Error."));

    // Create the operation
    Operation operation = new Operation()
        .summary(summary)
        .description(description)
        .addTagsItem(tags.get(0)); // Assign the first tag from the list

    // Add query parameters if they exist
    for (Parameter param : queryParameters) {
      operation.addParametersItem(param);
    }

    // Add headers
    for (Parameter header : headers) {
      operation.addParametersItem(header);
    }

    // Assign the request body
    operation.setRequestBody(requestBody);

    // Assign the responses
    operation.setResponses(apiResponses);

    // Create the PathItem
    PathItem pathItem = new PathItem();
    if ("GET".equalsIgnoreCase(httpMethod)) {
      pathItem.get(operation);
    } else if ("POST".equalsIgnoreCase(httpMethod)) {
      pathItem.post(operation);
    }
    // You can add other HTTP methods if necessary

    // Add the PathItem to OpenAPI
    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }

    openAPI.getPaths().addPathItem(BASE_PATH, pathItem);

    // Add the schema to the OpenAPI component
    addSchema(openAPI, actionName + "Response", responseSchema);

    // Add tags to OpenAPI to avoid duplicates and provide descriptions
    if (openAPI.getTags() == null) {
      openAPI.setTags(new ArrayList<>());
    }
    for (String tag : tags) {
      if (openAPI.getTags().stream().noneMatch(t -> t.getName().equals(tag))) {
        String tagDescription = "";
        if ("Initial Org Setup".equals(tag)) {
          tagDescription = "Endpoints related to the initial organization setup process.";
        }
        openAPI.addTagsItem(new Tag().name(tag).description(tagDescription));
      }
    }
  }

  /**
   * Method to create query parameters
   */
  private Parameter createQueryParameter(String name, String example, String type, boolean required, String description) {
    return new Parameter()
        .in("query")
        .name(name)
        .required(required)
        .schema(new Schema<String>().type(type).example(example))
        .description(description);
  }

  /**
   * Method to create header parameters
   */
  private Parameter createHeaderParameter(String name, String example, String type, boolean required, String description) {
    return new Parameter()
        .in("header")
        .name(name)
        .required(required)
        .schema(new Schema<String>().type(type).example(example))
        .description(description);
  }

  /**
   * Method to create API responses
   */
  private ApiResponse createApiResponse(String description, Schema<?> schema, String example) {
    // Determine the content type based on the schema
    String mediaType = "text/html";
    // If the response is JSON, you could use "application/json"

    return new ApiResponse()
        .description(description)
        .content(new Content().addMediaType(mediaType,
            new MediaType().schema(schema).example(example)));
  }

  /**
   * Method to add schemas to the OpenAPI component
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
   * Define the request schema for InitialOrgSetup (multipart/form-data)
   */
  private Schema<?> defineInitialOrgSetupRequestSchema() {
    Schema<Object> schema = new Schema<>();
    schema.type("object");
    schema.description("Multipart form data for Initial Organization Setup.");

    schema.addProperties("Command", new Schema<>().type(TYPE_STRING).example("OK"));
    schema.addProperties("inpLastFieldChanged", new Schema<>().type(TYPE_STRING).nullable(true).example(""));
    schema.addProperties("inpOrganization", new Schema<>().type(TYPE_STRING).example("Org"));
    schema.addProperties("inpOrgUser", new Schema<>().type(TYPE_STRING).example("Client"));
    schema.addProperties("inpPassword", new Schema<>().type(TYPE_STRING).example("admin"));
    schema.addProperties("inpConfirmPassword", new Schema<>().type(TYPE_STRING).example("admin"));
    schema.addProperties("inpOrgType", new Schema<>().type(TYPE_STRING).example("3"));
    schema.addProperties("inpParentOrg", new Schema<>().type(TYPE_STRING).example("0"));
    schema.addProperties("inpcLocationId", new Schema<>().type(TYPE_STRING).nullable(true).example(""));
    schema.addProperties("inpcLocationId_R", new Schema<>().type(TYPE_STRING).nullable(true).example(""));
    schema.addProperties("inpCurrency", new Schema<>().type(TYPE_STRING).nullable(true).example(""));
    schema.addProperties("inpTreeClass", new Schema<>().type(TYPE_STRING).example("org.openbravo.erpCommon.modules.ModuleReferenceDataOrgTree"));
    schema.addProperties("inpNodeId", new Schema<>().type(TYPE_STRING).example("7BFA8FF057AB46CAAB2FAAED8B870E32"));
    schema.addProperties("inpLevel", new Schema<>().type(TYPE_STRING).nullable(true).example(""));
    schema.addProperties("inpNodes", new Schema<>().type(TYPE_STRING).example("7BFA8FF057AB46CAAB2FAAED8B870E32"));

    // Define required fields
    schema.required(Arrays.asList(
        "Command",
        "inpOrganization",
        "inpOrgUser",
        "inpPassword",
        "inpConfirmPassword",
        "inpOrgType",
        "inpParentOrg",
        "inpTreeClass",
        "inpNodeId",
        "inpNodes"
    ));

    return schema;
  }

  /**
   * Define the response schema for InitialOrgSetup (HTML)
   */
  private Schema<?> defineInitialOrgSetupResponseSchema() {
    Schema<Object> schema = new Schema<>();
    schema.type("object");
    schema.description("HTML response for Initial Organization Setup.");

    // If the response is HTML, you might not define a detailed schema.
    // However, if you want to document specific parts, you could do so here.

    return schema;
  }
}