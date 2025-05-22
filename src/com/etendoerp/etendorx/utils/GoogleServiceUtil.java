package com.etendoerp.etendorx.utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for interacting with the Google Sheets and Drive APIs using a Bearer token.
 * Provides methods for authenticating with Google services, extracting spreadsheet information,
 * and reading tabular data from Sheets.
 */
public class GoogleServiceUtil {

  private static final Logger log = LoggerFactory.getLogger(GoogleServiceUtil.class);
  private static final String APPLICATION_NAME = "Google Sheets Java Integration";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  /**
   * Creates a Google Sheets API client instance using the provided OAuth2 access token.
   *
   * @param accessToken OAuth2 Bearer token with sufficient scope (e.g. spreadsheets.readonly).
   * @return an authenticated instance of the Google Sheets client.
   */
  public static Sheets getSheetsService(String accessToken) {
    return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, bearerTokenInitializer(accessToken))
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  /**
   * Creates a Google Drive API client instance using the provided OAuth2 access token.
   *
   * @param accessToken OAuth2 Bearer token with sufficient scope (e.g. drive.metadata.readonly).
   * @return an authenticated instance of the Google Drive client.
   */
  public static Drive getDriveService(String accessToken) {
    return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, bearerTokenInitializer(accessToken))
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  /**
   * Returns an HttpRequestInitializer that sets the Authorization header with the Bearer token.
   *
   * @param accessToken the OAuth2 access token.
   * @return an initializer that adds the Authorization header to each request.
   */
  private static HttpRequestInitializer bearerTokenInitializer(String accessToken) {
    return (HttpRequest request) -> {
      HttpHeaders headers = new HttpHeaders();
      headers.setAuthorization("Bearer " + accessToken);
      request.setHeaders(headers);
    };
  }

  /**
   * Retrieves the title of the tab at a given index in the specified spreadsheet.
   *
   * @param index   the zero-based index of the tab.
   * @param sheetId the ID of the spreadsheet.
   * @param token   the access token.
   * @return the name of the tab at the given index.
   * @throws Exception if the tab index is invalid or the spreadsheet has no tabs.
   */
  public static String getTabName(int index, String sheetId, String token) throws Exception {
    Sheets sheetsService = GoogleServiceUtil.getSheetsService(token);
    Spreadsheet spreadsheet = sheetsService.spreadsheets().get(sheetId).execute();
    List<Sheet> sheets = spreadsheet.getSheets();

    if (sheets == null || sheets.isEmpty()) {
      throw new RuntimeException("La hoja no contiene solapas.");
    }
    if (sheets.size() < index) {
      throw new OBException("Wrong tab number. It not can be greater that the qty of tabs.");
    }
    return sheets.get(index).getProperties().getTitle();
  }

  /**
   * Extracts the spreadsheet ID from a full Google Sheets URL.
   *
   * @param url the full URL of a Google Spreadsheet (e.g. https://docs.google.com/spreadsheets/d/ID/edit).
   * @return the spreadsheet ID as a string.
   * @throws IllegalArgumentException if the URL format is invalid or the ID cannot be extracted.
   */
  public static String extractSheetIdFromUrl(String url) throws IllegalArgumentException {
    String pattern = "https://docs\\.google\\.com/spreadsheets/d/([a-zA-Z0-9-_]+)";
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(url);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalArgumentException("URL inválida. No se pudo extraer el ID del spreadsheet.");
    }
  }

  /**
   * Safely retrieves a cell value from a given row at the specified index.
   *
   * @param row   the row list containing cell values.
   * @param index the zero-based column index.
   * @return the cell value as a string, or an empty string if the cell is missing.
   */
  public static String getCellValue(List<Object> row, int index) {
    if (index < row.size()) {
      return row.get(index).toString();
    }
    return "";
  }


  public static List<List<Object>> findSpreadsheetAndTab(String sheetId, String tabName, String token) throws Exception {
    Sheets sheetsService = GoogleServiceUtil.getSheetsService(token);

    Spreadsheet spreadsheet = sheetsService.spreadsheets().get(sheetId).execute();
    List<Sheet> sheets = spreadsheet.getSheets();

    boolean foundTab = sheets.stream()
        .anyMatch(s -> tabName.equalsIgnoreCase(s.getProperties().getTitle()));

    if (!foundTab) {
      throw new RuntimeException("❌ Solapa no encontrada: " + tabName);
    }

    String range = tabName; // Podés usar: tabName + "!A1:Z1000" si querés limitar
    ValueRange response = sheetsService.spreadsheets().values()
        .get(sheetId, range)
        .execute();

    List<List<Object>> values = response.getValues();

    if (values == null || values.isEmpty()) {
      System.out.println("🔍 No se encontraron datos en la solapa: " + tabName);
      return List.of(); // Lista vacía
    } else {
      System.out.println("✅ Registros obtenidos: " + values.size());
      return values;
    }
  }

  public static List<List<Object>> readSheet(String accessToken, String fileId, String range) throws Exception {
    HttpTransport httpTransport = new NetHttpTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    // Autenticación usando el token obtenido con drive.file
    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

    Sheets service = new Sheets.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("Etendo Google Picker Integration")
        .build();

    range = StringUtils.isBlank(range) ? "A1:Z1000" : range;
    ValueRange response = service.spreadsheets().values()
        .get(fileId, range)
        .execute();

    return response.getValues();
  }

  /**
   * Returns a list of accessible files filtered by a user-friendly file type keyword.
   *
   * <p>This method allows querying Google Drive for files the application has access to,
   * using a simplified keyword (e.g., "spreadsheet", "doc", "slides", "pdf"). It maps the
   * keyword to the appropriate MIME type and delegates the request to the Drive API.</p>
   *
   * <p>Supported types:</p>
   * <ul>
   *   <li>{@code "spreadsheet"} → Google Sheets files</li>
   *   <li>{@code "doc"} → Google Docs documents</li>
   *   <li>{@code "slides"} → Google Slides presentations</li>
   *   <li>{@code "pdf"} or {@code "pdfs"} → Uploaded PDF files</li>
   * </ul>
   *
   * @param type a string representing the file type to retrieve (e.g., "spreadsheet", "doc", "slides", "pdfs")
   * @param accessToken a valid OAuth 2.0 access token (typically with {@code drive.file} scope)
   * @return a JSONArray containing each accessible file as a JSON object with {@code id}, {@code name}, and {@code mimeType}
   * @throws Exception if the request fails or the type is not supported
   */
  public static JSONArray listAccessibleFiles(String type, String accessToken) throws Exception {
    String mimeType;

    switch (type.toLowerCase()) {
      case "spreadsheet":
        mimeType = "application/vnd.google-apps.spreadsheet";
        break;
      case "doc":
        mimeType = "application/vnd.google-apps.document";
        break;
      case "slides":
        mimeType = "application/vnd.google-apps.presentation";
        break;
      case "pdf":
      case "pdfs":
        mimeType = "application/pdf";
        break;
      default:
        // TODO: DBMessage
        throw new IllegalArgumentException("Tipo no soportado: " + type);
    }
    return listAccessibleFilesByMimeType(mimeType, accessToken);
  }

  /**
   * Queries the Google Drive API to retrieve files accessible by the user for a specific MIME type.
   *
   * <p>This method sends a GET request to {@code drive/v3/files} with a MIME type filter and returns
   * a list of files the authenticated user has access to (limited to 100 results).</p>
   *
   * @param mimeType the MIME type to filter files by (e.g., {@code application/vnd.google-apps.spreadsheet})
   * @param accessToken a valid OAuth 2.0 access token (e.g., with {@code drive.file} scope)
   * @return a JSONArray of files, each represented as a JSON object containing {@code id}, {@code name}, and {@code mimeType}
   * @throws Exception if the API request fails or the response cannot be parsed
   */
  private static JSONArray listAccessibleFilesByMimeType(String mimeType, String accessToken) throws Exception {
    String endpoint = "https://www.googleapis.com/drive/v3/files" +
        "?q=mimeType='" + mimeType + "'" +
        "&fields=files(id,name,mimeType)" +
        "&pageSize=100";

    URL url = new URL(endpoint);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setRequestProperty("Accept", "application/json");

    int status = conn.getResponseCode();
    if (status != 200) {
      throw new RuntimeException("Error al listar archivos: HTTP " + status);
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder responseBuilder = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      responseBuilder.append(line);
    }
    reader.close();

    JSONObject json = new JSONObject(responseBuilder.toString());
    return json.getJSONArray("files");
  }

  /**
   * Creates a new file in the user's Google Drive with the specified name and MIME type.
   *
   * <p>Supported MIME types include:
   * <ul>
   *   <li>{@code application/vnd.google-apps.spreadsheet}</li>
   *   <li>{@code application/vnd.google-apps.document}</li>
   *   <li>{@code application/vnd.google-apps.presentation}</li>
   * </ul>
   *
   * @param name the name of the file to be created
   * @param mimeType the MIME type of the file (Google Workspace MIME or standard file)
   * @param accessToken a valid OAuth 2.0 access token (with {@code drive.file} scope)
   * @return a JSONObject representing the created file (id, name, mimeType)
   * @throws Exception if the creation fails
   */
  public static JSONObject createDriveFile(String name, String mimeType, String accessToken) throws Exception {
    URL url = new URL("https://www.googleapis.com/drive/v3/files");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    conn.setDoOutput(true);

    String body = new JSONObject()
        .put("name", name)
        .put("mimeType", mimeType)
        .toString();

    try (OutputStream os = conn.getOutputStream()) {
      os.write(body.getBytes(StandardCharsets.UTF_8));
    }

    if (conn.getResponseCode() != 200) {
      throw new RuntimeException("Failed to create file: HTTP " + conn.getResponseCode());
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();

    return new JSONObject(response.toString());
  }

  /**
   * Updates the content of a Google Spreadsheet in the specified range.
   *
   * @param fileId the ID of the Google Sheet
   * @param accessToken OAuth 2.0 token with permission to access the file
   * @param range the range to write to (e.g., "A1:C5")
   * @param values a 2D List representing the values to insert
   * @return the update response from the Sheets API
   * @throws Exception if the update fails
   */
  public static JSONObject updateSpreadsheetValues(String fileId, String accessToken, String range, List<List<Object>> values) throws Exception {
    URL url = new URL("https://sheets.googleapis.com/v4/spreadsheets/" + fileId + "/values/" + range + "?valueInputOption=RAW");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);

    JSONArray jsonValues = new JSONArray();
    for (List<Object> row : values) {
      JSONArray jsonRow = new JSONArray();
      for (Object cell : row) {
        jsonRow.put(cell);
      }
      jsonValues.put(jsonRow);
    }

    JSONObject body = new JSONObject()
        .put("range", range)
        .put("majorDimension", "ROWS")
        .put("values", jsonValues);


    try (OutputStream os = conn.getOutputStream()) {
      os.write(body.toString().getBytes(StandardCharsets.UTF_8));
    }

    if (conn.getResponseCode() != 200) {
      throw new RuntimeException("Failed to update spreadsheet: HTTP " + conn.getResponseCode());
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();

    return new JSONObject(response.toString());
  }

  public static void runTestGoogleDrive(String accessToken) {
    try {
      System.out.println("1️⃣ List existing spreadsheets:");
      JSONArray initialSheets = GoogleServiceUtil.listAccessibleFiles("spreadsheet", accessToken);
      if (0 == initialSheets.length()) {
        System.out.println("📄 No sheets are accessible");
      }
      for (int i = 0; i < initialSheets.length(); i++) {
        JSONObject sheet = initialSheets.getJSONObject(i);
        System.out.println("📄 " + sheet.getString("name") + " - ID: " + sheet.getString("id"));
      }

      System.out.println("\n2️⃣ Create a new spreadsheet...");
      JSONObject newSheet = GoogleServiceUtil.createDriveFile(
          "DEMO - Sheet created by Etendo",
          "application/vnd.google-apps.spreadsheet",
          accessToken
      );
      String newSheetId = newSheet.getString("id");
      System.out.println("✅ Sheet created: " + newSheet.getString("name") + " - ID: " + newSheetId);

      System.out.println("\n3️⃣ List spreadsheets again:");
      JSONArray updatedSheets = GoogleServiceUtil.listAccessibleFiles("spreadsheet", accessToken);
      for (int i = 0; i < updatedSheets.length(); i++) {
        JSONObject sheet = updatedSheets.getJSONObject(i);
        System.out.println("📄 " + sheet.getString("name") + " - ID: " + sheet.getString("id"));
      }

      System.out.println("\n4️⃣ Update the new sheet with demo data...");
      List<List<Object>> values = Arrays.asList(
          Arrays.asList("Producto", "Precio", "Stock"),
          Arrays.asList("Mouse", "25", "100"),
          Arrays.asList("Teclado", "45", "200")
      );
      JSONObject updateResult = GoogleServiceUtil.updateSpreadsheetValues(newSheetId, accessToken, "A1:C3", values);
      System.out.println("✏️ Update result: " + updateResult.toString(2));

      System.out.println("\n5️⃣ Read back the data:");
      List<List<Object>> rows = GoogleServiceUtil.readSheet(accessToken, newSheetId, "");
      for (List<Object> row : rows) {
        System.out.println("📊 " + row);
      }

      System.out.println("\n🎉 Test completed successfully.");

    } catch (Exception e) {
      System.err.println("❌ Error during test:");
      e.printStackTrace();
    }
  }
}
