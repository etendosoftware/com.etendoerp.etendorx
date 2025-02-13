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
import org.apache.commons.lang.StringUtils;
import org.openbravo.base.session.OBPropertiesProvider;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class InitialClientSetupEndpoint implements OpenAPIEndpoint {

  private static final String BASE_PATH = "/ad_forms/InitialClientSetup.html?stateless=true";
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
    String etendoHost = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ETENDO_HOST");
    String url = StringUtils.substring(etendoHost, 0, StringUtils.lastIndexOf(etendoHost, '/'));
    // Define the schemas and examples for the InitialClientSetup action
    Schema<?> initialClientSetupRequestSchema = defineInitialClientSetupRequestSchema();
    String initialClientSetupRequestExample = "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"Command\"\r\n\r\n" +
        "OK\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpLastFieldChanged\"\r\n\r\n\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpClient\"\r\n\r\n" +
        "Cliente\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpPassword\"\r\n\r\n" +
        "admin\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpClientUser\"\r\n\r\n" +
        "ClienteAdmin\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpConfirmPassword\"\r\n\r\n" +
        "admin\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpCurrency\"\r\n\r\n" +
        "102\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpFile\"; filename=\"\"\r\n" +
        "Content-Type: application/octet-stream\r\n\r\n\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpTreeClass\"\r\n\r\n" +
        "org.openbravo.erpCommon.modules.ModuleReferenceDataClientTree\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpNodeId\"\r\n\r\n" +
        "0\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpLevel\"\r\n\r\n\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r\r\n" +
        "Content-Disposition: form-data; name=\"inpNodes\"\r\n\r\n" +
        "0\r\n" +
        "------WebKitFormBoundaryESnGV3KpzjPoQw1r--\r\n";

    Schema<?> initialClientSetupResponseSchema = defineInitialClientSetupResponseSchema();
    String initialClientSetupResponseExample = "<html>Configuración Inicial del Cliente</html>";

    List<Parameter> commonHeaders = Arrays.asList(
        createHeaderParameter("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7", TYPE_STRING, true, "Specifies the media types that are acceptable for the response."),
        createHeaderParameter("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryESnGV3KpzjPoQw1r", TYPE_STRING, true, "Indicates the media type of the resource."),
        createHeaderParameter("Origin", url, TYPE_STRING, false, "The origin of the request."),
        createHeaderParameter("Referer", etendoHost + "/ad_forms/InitialClientSetup.html?noprefs=true&hideMenu=true&Command=DEFAULT", TYPE_STRING, false, "The address of the previous web page from which a link to the currently requested page was followed."),
        createHeaderParameter("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36", TYPE_STRING, false, "The user agent string of the user agent.")
    );


    createInitialClientSetupEndpoint(openAPI,
        "InitialClientSetup",
        "Handles the initial client setup form submission",
        "This endpoint processes the initial client setup form submission, including client credentials and configuration parameters.",
        initialClientSetupRequestSchema,
        initialClientSetupRequestExample,
        initialClientSetupResponseSchema,
        initialClientSetupResponseExample,
        commonHeaders, tags,
        "POST");
  }

  /**
   * Method to create the endpoint related to Initial Client Setup
   */
  private void createInitialClientSetupEndpoint(OpenAPI openAPI, String actionName, String summary, String description,
      Schema<?> requestSchema, String requestExample,
      Schema<?> responseSchema, String responseExample,
      List<Parameter> headers, List<String> tags, String httpMethod) {

    List<Parameter> queryParameters = Collections.emptyList();

    RequestBody requestBody = new RequestBody()
        .description("Payload for executing the Initial Client Setup action.")
        .content(new Content().addMediaType("multipart/form-data",
            new MediaType().schema(requestSchema).example(requestExample)))
        .required(true);

    ApiResponses apiResponses = new ApiResponses()
        .addApiResponse("200", createApiResponse("Successful response.", responseSchema, responseExample))
        .addApiResponse("400", new ApiResponse().description("Bad Request."))
        .addApiResponse("500", new ApiResponse().description("Internal Server Error."));

    Operation operation = new Operation()
        .summary(summary)
        .description(description)
        .addTagsItem(tags.get(0)); // Assigne the first tag to the operation

    for (Parameter param : queryParameters) {
      operation.addParametersItem(param);
    }

    for (Parameter header : headers) {
      operation.addParametersItem(header);
    }

    operation.setRequestBody(requestBody);

    operation.setResponses(apiResponses);

    PathItem pathItem = new PathItem();
    if ("GET".equalsIgnoreCase(httpMethod)) {
      pathItem.get(operation);
    } else if ("POST".equalsIgnoreCase(httpMethod)) {
      pathItem.post(operation);
    }
    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }
    openAPI.getPaths().addPathItem(BASE_PATH, pathItem);

    addSchema(openAPI, actionName + "Response", responseSchema);

    if (openAPI.getTags() == null) {
      openAPI.setTags(new ArrayList<>());
    }
    for (String tag : tags) {
      if (openAPI.getTags().stream().noneMatch(t -> t.getName().equals(tag))) {
        String tagDescription = "";
        if ("Initial Client Setup".equals(tag)) {
          tagDescription = "Endpoints related to the initial client setup process.";
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
    String mediaType = "text/html";

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
   * Define the request schema for InitialClientSetup (multipart/form-data)
   */
  private Schema<?> defineInitialClientSetupRequestSchema() {
    Schema<Object> schema = new Schema<>();
    schema.type("object");
    schema.description("Multipart form data for Initial Client Setup.");

    schema.addProperties("Command", new Schema<>().type(TYPE_STRING).example("OK"));
    schema.addProperties("inpLastFieldChanged", new Schema<>().type(TYPE_STRING).nullable(true).example(""));
    schema.addProperties("inpClient", new Schema<>().type(TYPE_STRING).example("Cliente"));
    schema.addProperties("inpPassword", new Schema<>().type(TYPE_STRING).example("admin"));
    schema.addProperties("inpClientUser", new Schema<>().type(TYPE_STRING).example("ClienteAdmin"));
    schema.addProperties("inpConfirmPassword", new Schema<>().type(TYPE_STRING).example("admin"));
    schema.addProperties("inpCurrency", new Schema<>().type(TYPE_STRING).example("102"));
    schema.addProperties("inpFile", new Schema<>().type(TYPE_STRING).format("binary").nullable(true).example(""));
    schema.addProperties("inpTreeClass", new Schema<>().type(TYPE_STRING).example("org.openbravo.erpCommon.modules.ModuleReferenceDataClientTree"));
    schema.addProperties("inpNodeId", new Schema<>().type(TYPE_STRING).example("0"));
    schema.addProperties("inpLevel", new Schema<>().type(TYPE_STRING).nullable(true).example(""));
    schema.addProperties("inpNodes", new Schema<>().type(TYPE_STRING).example("0"));

    // Define required fields
    schema.required(Arrays.asList(
        "Command",
        "inpClient",
        "inpPassword",
        "inpClientUser",
        "inpConfirmPassword",
        "inpCurrency",
        "inpTreeClass",
        "inpNodeId",
        "inpNodes"
    ));

    return schema;
  }

  /**
   * Define the response schema for InitialClientSetup (HTML)
   */
  private Schema<?> defineInitialClientSetupResponseSchema() {
    Schema<Object> schema = new Schema<>();
    schema.type("object");
    schema.description("HTML response for Initial Client Setup.");

    return schema;
  }
}
