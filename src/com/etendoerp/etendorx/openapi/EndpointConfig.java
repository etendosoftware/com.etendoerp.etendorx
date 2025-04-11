package com.etendoerp.etendorx.openapi;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.List;

/**
 * This class represents the configuration of an endpoint.
 */
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

  /**
   * Constructor.
   * @param builder The builder object used to construct the EndpointConfig instance.
   */
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

  /**
   * Gets the tag of the endpoint.
   * @return The tag of the endpoint.
   */
  public String getTag() { return tag; }

  /**
   * Gets the action value of the endpoint.
   * @return The action value of the endpoint.
   */
  public String getActionValue() { return actionValue; }

  /**
   * Gets the summary of the endpoint.
   * @return The summary of the endpoint.
   */
  public String getSummary() { return summary; }

  /**
   * Gets the description of the endpoint.
   * @return The description of the endpoint.
   */
  public String getDescription() { return description; }

  /**
   * Gets the response schema of the endpoint.
   * @return The response schema of the endpoint.
   */
  public Schema<?> getResponseSchema() { return responseSchema; }

  /**
   * Gets the response example of the endpoint.
   * @return The response example of the endpoint.
   */
  public String getResponseExample() { return responseExample; }

  /**
   * Gets the parameters of the endpoint.
   * @return The parameters of the endpoint.
   */
  public List<Parameter> getParameters() { return parameters; }

  /**
   * Gets the request body schema of the endpoint.
   * @return The request body schema of the endpoint.
   */
  public Schema<?> getRequestBodySchema() { return requestBodySchema; }

  /**
   * Gets the request body example of the endpoint.
   * @return The request body example of the endpoint.
   */
  public String getRequestBodyExample() { return requestBodyExample; }

  /**
   * Gets the HTTP method of the endpoint.
   * @return The HTTP method of the endpoint.
   */
  public String getHttpMethod() { return httpMethod; }

  /**
   * Builder class for EndpointConfig.
   */
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

    /**
     * Sets the tag of the endpoint.
     * @param tag The tag of the endpoint.
     * @return The builder instance.
     */
    public Builder tag(String tag) {
      this.tag = tag;
      return this;
    }

    /**
     * Sets the action value of the endpoint.
     * @param actionValue The action value of the endpoint.
     * @return The builder instance.
     */
    public Builder actionValue(String actionValue) {
      this.actionValue = actionValue;
      return this;
    }

    /**
     * Sets the summary of the endpoint.
     * @param summary The summary of the endpoint.
     * @return The builder instance.
     */
    public Builder summary(String summary) {
      this.summary = summary;
      return this;
    }

    /**
     * Sets the description of the endpoint.
     * @param description The description of the endpoint.
     * @return The builder instance.
     */
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /**
     * Sets the response schema of the endpoint.
     * @param responseSchema The response schema of the endpoint.
     * @return The builder instance.
     */
    public Builder responseSchema(Schema<?> responseSchema) {
      this.responseSchema = responseSchema;
      return this;
    }

    /**
     * Sets the response example of the endpoint.
     * @param responseExample The response example of the endpoint.
     * @return The builder instance.
     */
    public Builder responseExample(String responseExample) {
      this.responseExample = responseExample;
      return this;
    }

    /**
     * Sets the parameters of the endpoint.
     * @param parameters The parameters of the endpoint.
     * @return The builder instance.
     */
    public Builder parameters(List<Parameter> parameters) {
      this.parameters = parameters;
      return this;
    }

    /**
     * Sets the request body schema of the endpoint.
     * @param requestBodySchema The request body schema of the endpoint.
     * @return The builder instance.
     */
    public Builder requestBodySchema(Schema<?> requestBodySchema) {
      this.requestBodySchema = requestBodySchema;
      return this;
    }

    /**
     * Sets the request body example of the endpoint.
     * @param requestBodyExample The request body example of the endpoint.
     * @return The builder instance.
     */
    public Builder requestBodyExample(String requestBodyExample) {
      this.requestBodyExample = requestBodyExample;
      return this;
    }

    /**
     * Sets the HTTP method of the endpoint.
     * @param httpMethod The HTTP method of the endpoint.
     * @return The builder instance.
     */
    public Builder httpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    /**
     * Builds and returns an EndpointConfig instance.
     * @return The constructed EndpointConfig instance.
     */
    public EndpointConfig build() {
      return new EndpointConfig(this);
    }
  }
}
