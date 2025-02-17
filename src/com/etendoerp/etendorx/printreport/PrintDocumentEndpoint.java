package com.etendoerp.etendorx.printreport;

import com.etendoerp.openapi.model.OpenAPIEndpoint;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.openbravo.base.session.OBPropertiesProvider;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@ApplicationScoped
public class PrintDocumentEndpoint implements OpenAPIEndpoint {

  private static final String PRINT_DOCUMENT_ACTION = "com.etendoerp.client.print.PrintOptionsActionHandler";
  private static final String BASE_PATH = "/orders/PrintOptions.html?stateless=true";
  public static final String POST = "POST";
  public static final List<String> tags = List.of("Print Report");
  public static final List<String> tagsDescription = List.of("Endpoints related to printing reports and documents.");

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
    String url = etendoHost.substring(0, etendoHost.lastIndexOf('/'));

    Schema<?> printDocumentResponseSchema = definePrintDocumentResponseSchema();
    String printDocumentResponseExample = "";

    List<Parameter> printDocumentRequestSchema = definePrintDocumentRequestParameters();
    String printDocumentRequestExample = "Command=PRINT&IsPopUpCall=1&inpLastFieldChanged=&inpKey=&inpwindowId=&inpTabId=&inpDocumentId=%278EEFF40EC8AC45B880A8CED14A927CCA%27&draftDocumentIds=";

    List<Parameter> printDocumentParams = Arrays.asList(createHeaderParameter("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            true, "Specifies the media types that are acceptable for the response."),
        createHeaderParameter("Content-Type", "application/x-www-form-urlencoded", true,
            "Indicates the media type of the resource."),
        createHeaderParameter("Origin", url, false,
            "The origin of the request."), createHeaderParameter("Referer",
                    etendoHost + "/orders/print.html?Commnad=PDF&IsPopUpCall=1", false,
            "The address of the previous web page from which a link to the currently requested page was followed."),
        createHeaderParameter("User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
            false, "The user agent string of the user agent."));


    createEndpoint(openAPI, printDocumentResponseSchema, printDocumentResponseExample,
        printDocumentParams, printDocumentRequestSchema, printDocumentRequestExample);
  }

  private void createEndpoint(OpenAPI openAPI, Schema<?> responseSchema, String responseExample,
      List<Parameter> parameters, List<Parameter> requestBodySchema, String requestBodyExample) {

    ApiResponses apiResponses = new ApiResponses().addApiResponse("200",
            createApiResponse(responseSchema, responseExample))
        .addApiResponse("400", new ApiResponse().description("Bad Request."))
        .addApiResponse("500", new ApiResponse().description("Internal Server Error."));

    Operation operation = new Operation().summary("(Require changes in backend) Initializes print options for an order")
        .description("This endpoint is used to initialize print options for a specific order.")
        .addTagsItem("Print Report");

    for (String tag : tags) {
      operation.addTagsItem(tag);
    }

    for (Parameter parameter : parameters) {
      operation.addParametersItem(parameter);
    }

    if (requestBodySchema != null) {
      RequestBody requestBody = new RequestBody()
          .description("Form data for initializing print options.")
          .content(new Content().addMediaType("application/x-www-form-urlencoded",
              new MediaType()
                  .schema(new ObjectSchema()
                      .addProperties("Command", new StringSchema().example("PRINT"))
                      .addProperties("IsPopUpCall", new StringSchema().example("1"))
                      .addProperties("inpLastFieldChanged", new StringSchema().example(""))
                      .addProperties("inpKey", new StringSchema().example(""))
                      .addProperties("inpwindowId", new StringSchema().example(""))
                      .addProperties("inpTabId", new StringSchema().example(""))
                      .addProperties("inpDocumentId", new StringSchema().example("('0AC230C0DDA4435A949B40602A183F45')"))
                      .addProperties("draftDocumentIds", new StringSchema().example(""))
                  )
                  .example(new HashMap<String, Object>() {{
                    put("Command", "PRINT");
                    put("IsPopUpCall", "1");
                    put("inpLastFieldChanged", "");
                    put("inpKey", "");
                    put("inpwindowId", "");
                    put("inpTabId", "");
                    put("inpDocumentId", "('0AC230C0DDA4435A949B40602A183F45')");
                    put("draftDocumentIds", "");
                  }})))
          .required(true);
      operation.setRequestBody(requestBody);
    }

    operation.responses(apiResponses);

    PathItem pathItem = new PathItem().post(operation);

    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }

    openAPI.getPaths().addPathItem(BASE_PATH, pathItem);

    addSchema(openAPI, responseSchema);
    if(openAPI.getTags() == null) {
      openAPI.setTags(new ArrayList<>());
    }
    tags.forEach(it -> {
      operation.getTags().add(it);
      Tag tag = new Tag().name(it).description(tagsDescription.get(tags.indexOf(it)));
      openAPI.getTags().add(tag);
    });

  }

  private ApiResponse createApiResponse(Schema<?> schema, String example) {
    return new ApiResponse().description("Successful response with print options HTML.")
        .content(new Content().addMediaType("text/html",
            new MediaType().schema(schema).example(example)));
  }

  private Parameter createHeaderParameter(String name, String example, boolean required,
      String description) {
    return new Parameter().in("header")
        .name(name)
        .required(required)
        .schema(new Schema<String>().type("string").example(example))
        .description(description);
  }

  private void addSchema(OpenAPI openAPI, Schema<?> schema) {
    if (openAPI.getComponents() == null) {
      openAPI.setComponents(new io.swagger.v3.oas.models.Components());
    }
    if (openAPI.getComponents().getSchemas() == null) {
      openAPI.getComponents().setSchemas(new HashMap<>());
    }
    if (!openAPI.getComponents().getSchemas().containsKey("PrintDocumentResponse")) {
      openAPI.getComponents().addSchemas("PrintDocumentResponse", schema);
    }
  }

  private List<Parameter> definePrintDocumentRequestParameters() {
    List<Parameter> parameters = new ArrayList<>();

    parameters.add(new Parameter()
        .name("Command")
        .in("formData")
        .required(true)
        .schema(new StringSchema().example("PRINT")));

    parameters.add(new Parameter()
        .name("IsPopUpCall")
        .in("formData")
        .required(true)
        .schema(new StringSchema().example("1")));

    parameters.add(new Parameter()
        .name("inpLastFieldChanged")
        .in("formData")
        .required(false)
        .schema(new StringSchema().example("")));

    parameters.add(new Parameter()
        .name("inpKey")
        .in("formData")
        .required(false)
        .schema(new StringSchema().example("")));

    parameters.add(new Parameter()
        .name("inpwindowId")
        .in("formData")
        .required(false)
        .schema(new StringSchema().example("")));

    parameters.add(new Parameter()
        .name("inpTabId")
        .in("formData")
        .required(false)
        .schema(new StringSchema().example("")));

    parameters.add(new Parameter()
        .name("inpDocumentId")
        .in("formData")
        .required(true)
        .schema(new StringSchema().example("0AC230C0DDA4435A949B40602A183F45"))
        .allowReserved(true));

    parameters.add(new Parameter()
        .name("draftDocumentIds")
        .in("formData")
        .required(false)
        .schema(new StringSchema().example("")));

    return parameters;
  }
  private Schema<?> definePrintDocumentResponseSchema() {
    Schema<String> schema = new Schema<>();
    schema.type("string").format("html").description("HTML content of the print options.");
    return schema;
  }
}
