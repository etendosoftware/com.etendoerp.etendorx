package com.etendoerp.etendorx.openapi;

import java.util.List;

import com.etendoerp.openapi.OpenAPIDefaultRequest;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;

public class ImageUploadOpenAPI extends OpenAPIDefaultRequest {
  public static final String ETENDO_ID_PATTERN = "^[0-9a-fA-F]{1,32}$";

  @Override
  protected Class<?>[] getClasses() {
    // class com.etendoerp.etendorx.imageUpload
    return new Class<?>[]{ com.etendoerp.etendorx.services.ImageUploadServlet.class };
  }

  @Override
  protected String getEndpointPath() {
    return "/sws/com.etendoerp.etendorx.imageUpload/";
  }


  @Override
  Operation getPOSTEndpoint() {
    //PathItem
    Operation endpoint = new Operation();
    endpoint.setSummary("Upload an image to EtendoERP");
    endpoint.setDescription("Upload an image to EtendoERP, it can uses a configuration associated to a Column ID to " +
        "automatically resize the image.");

    Schema reqSchema = new Schema()
        .addProperty("filename", new StringSchema().description("The name of the file").example("image.jpg"))
        .addProperty("columnId", new StringSchema()
            .description("The column ID where the size and resize configuration is stored")
            .pattern(ETENDO_ID_PATTERN))
        .addProperty("base64Image", new StringSchema().description("The base64 encoded image"));
    reqSchema.required(List.of("filename", "base64Image"));
    RequestBody requestBody = new RequestBody().content(new Content()
        .addMediaType("application/json", new MediaType().schema(reqSchema))
    );
    endpoint.requestBody(requestBody);
    return endpoint;
  }
}


