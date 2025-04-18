package com.etendoerp.etendorx.init;

import com.etendoerp.etendorx.init.InitialOrganizationSetupEndpoint;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Initial organization setup endpoint test class.
 */
@RunWith(MockitoJUnitRunner.class)
public class InitialOrganizationSetupEndpointTest {

    @InjectMocks
    private InitialOrganizationSetupEndpoint endpoint;

    private OpenAPI openAPI;

    private static final String INITIAL_SETUP = "Initial Setup";

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        openAPI = new OpenAPI();
    }

    /**
     * Test is valid with null tag returns true.
     */
    @Test
    public void testIsValidWithNullTagReturnsTrue() {
        assertTrue(endpoint.isValid(null));
    }

    /**
     * Test is valid with valid tag returns true.
     */
    @Test
    public void testIsValidWithValidTagReturnsTrue() {
        assertTrue(endpoint.isValid(INITIAL_SETUP));
    }

    /**
     * Test is valid with invalid tag returns false.
     */
    @Test
    public void testIsValidWithInvalidTagReturnsFalse() {
        assertFalse(endpoint.isValid("Invalid Tag"));
    }

    /**
     * Test add creates valid open api definition.
     */
    @Test
    public void testAddCreatesValidOpenAPIDefinition() {
        // When
        endpoint.add(openAPI);

        // Then
        assertNotNull(openAPI.getPaths());
        PathItem pathItem = openAPI.getPaths().get("/ad_forms/InitialOrgSetup.html?stateless=true");
        assertNotNull(pathItem);

        // Verify POST operation
        Operation postOperation = pathItem.getPost();
        assertNotNull(postOperation);
        assertEquals("Handles the initial organization setup form submission", postOperation.getSummary());
        assertTrue(postOperation.getTags().contains(INITIAL_SETUP));

        // Verify request body
        RequestBody requestBody = postOperation.getRequestBody();
        assertNotNull(requestBody);
        assertTrue(requestBody.getRequired());
        Content content = requestBody.getContent();
        assertNotNull(content);
        MediaType mediaType = content.get("multipart/form-data");
        assertNotNull(mediaType);

        // Verify schema
        Schema<?> schema = mediaType.getSchema();
        assertNotNull(schema);
        assertEquals("object", schema.getType());
        assertTrue(schema.getRequired().contains("Command"));
        assertTrue(schema.getRequired().contains("inpOrganization"));
        assertTrue(schema.getRequired().contains("inpOrgUser"));

        // Verify responses
        ApiResponse response200 = postOperation.getResponses().get("200");
        assertNotNull(response200);
        ApiResponse response400 = postOperation.getResponses().get("400");
        assertNotNull(response400);
        ApiResponse response500 = postOperation.getResponses().get("500");
        assertNotNull(response500);

        // Verify headers
        boolean hasContentTypeHeader = false;
        boolean hasUserAgentHeader = false;
        for (Parameter parameter : postOperation.getParameters()) {
            if (StringUtils.equals("Content-Type", parameter.getName())) {
                hasContentTypeHeader = true;
                assertTrue(parameter.getRequired());
            }
            if (StringUtils.equals("User-Agent", parameter.getName())) {
                hasUserAgentHeader = true;
                assertFalse(parameter.getRequired());
            }
        }
        assertTrue(hasContentTypeHeader);
        assertTrue(hasUserAgentHeader);
    }

    /**
     * Test add creates valid schema definitions.
     */
    @Test
    public void testAddCreatesValidSchemaDefinitions() {
        // When
        endpoint.add(openAPI);

        // Then
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSchemas());
        assertTrue(openAPI.getComponents().getSchemas().containsKey("InitialOrgSetupResponse"));
    }

    /**
     * Test add creates valid tags.
     */
    @Test
    public void testAddCreatesValidTags() {
        // When
        endpoint.add(openAPI);

        // Then
        assertNotNull(openAPI.getTags());
        boolean hasInitialSetupTag = openAPI.getTags().stream()
                .anyMatch(tag -> StringUtils.equals(INITIAL_SETUP, tag.getName()));
        assertTrue(hasInitialSetupTag);
    }
}