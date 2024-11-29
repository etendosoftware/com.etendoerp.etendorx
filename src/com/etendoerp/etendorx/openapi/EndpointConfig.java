package com.etendoerp.etendorx.openapi;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.List;

public class EndpointConfig {
  private String tag;
  private String actionValue;
  private String summary;
  private String description;
  private Schema<?> responseSchema;
  private String responseExample;
  private List<Parameter> parameters;
  private Schema<?> requestBodySchema;
  private String requestBodyExample;
  private String httpMethod;

  // Constructor privado
  private EndpointConfig(Builder builder) {
    this.tag = builder.tag;
    this.actionValue = builder.actionValue;
    this.summary = builder.summary;
    this.description = builder.description;
    this.responseSchema = builder.responseSchema;
    this.responseExample = builder.responseExample;
    this.parameters = builder.parameters;
    this.requestBodySchema = builder.requestBodySchema;
    this.requestBodyExample = builder.requestBodyExample;
    this.httpMethod = builder.httpMethod;
  }

  // Getters
  public String getTag() { return tag; }
  public String getActionValue() { return actionValue; }
  public String getSummary() { return summary; }
  public String getDescription() { return description; }
  public Schema<?> getResponseSchema() { return responseSchema; }
  public String getResponseExample() { return responseExample; }
  public List<Parameter> getParameters() { return parameters; }
  public Schema<?> getRequestBodySchema() { return requestBodySchema; }
  public String getRequestBodyExample() { return requestBodyExample; }
  public String getHttpMethod() { return httpMethod; }

  // Builder
  public static class Builder {
    private String tag;
    private String actionValue;
    private String summary;
    private String description;
    private Schema<?> responseSchema;
    private String responseExample;
    private List<Parameter> parameters;
    private Schema<?> requestBodySchema;
    private String requestBodyExample;
    private String httpMethod;

    public Builder tag(String tag) {
      this.tag = tag;
      return this;
    }

    public Builder actionValue(String actionValue) {
      this.actionValue = actionValue;
      return this;
    }

    public Builder summary(String summary) {
      this.summary = summary;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder responseSchema(Schema<?> responseSchema) {
      this.responseSchema = responseSchema;
      return this;
    }

    public Builder responseExample(String responseExample) {
      this.responseExample = responseExample;
      return this;
    }

    public Builder parameters(List<Parameter> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder requestBodySchema(Schema<?> requestBodySchema) {
      this.requestBodySchema = requestBodySchema;
      return this;
    }

    public Builder requestBodyExample(String requestBodyExample) {
      this.requestBodyExample = requestBodyExample;
      return this;
    }

    public Builder httpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    public EndpointConfig build() {
      return new EndpointConfig(this);
    }
  }
}
