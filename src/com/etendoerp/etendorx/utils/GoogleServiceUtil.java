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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openbravo.base.secureApp.LoginUtils.log4j;

/**
 * Utility class for interacting with the Google Sheets and Drive APIs using a Bearer token.
 * Provides methods for authenticating with Google services, extracting spreadsheet information,
 * and reading tabular data from Sheets.
 */
public class GoogleServiceUtil {

  private static final String APPLICATION_NAME = "Google Sheets Java Integration";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  public static final String BEARER = "Bearer ";
  public static final String SPREADSHEET = "spreadsheet";
  public static final String DOC = "doc";
  public static final String SLIDES = "slides";
  public static final String PDF = "pdf";
  public static final String PDFS = "pdfs";
  public static final String AUTHORIZATION = "Authorization";

  private GoogleServiceUtil() {
    throw new IllegalStateException("Utility class");
  }

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
      headers.setAuthorization(BEARER + accessToken);
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
   * @throws OBException if the tab index is invalid or the spreadsheet has no tabs.
   */
  public static String getTabName(int index, String sheetId, String token) throws OBException, IOException {
    Sheets sheetsService = GoogleServiceUtil.getSheetsService(token);
    Spreadsheet spreadsheet = sheetsService.spreadsheets().get(sheetId).execute();
    List<Sheet> sheets = spreadsheet.getSheets();

    if (sheets == null || sheets.isEmpty()) {
      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_SheetHasNoTabs",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(errorMessage);
    }
    if (sheets.size() < index) {
      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_WrongTabNumber",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(errorMessage);
    }
    return sheets.get(index).getProperties().getTitle();
  }

  /**
   * Extracts the spreadsheet ID from a full Google Sheets URL.
   *
   * @param url the full URL of a Google Spreadsheet (e.g. https://docs.google.com/spreadsheets/d/<ID>/edit).
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
      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_WrongSheetURL",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new IllegalArgumentException(errorMessage); // ETRX_WrongSheetURL
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

  /**
   * Retrieves the cell values from a specific tab (sheet) within a Google Spreadsheet.
   * <p>
   * This method connects to the Google Sheets API using the provided access token and attempts to find
   * a tab (sheet) by name in the specified spreadsheet. The tab name comparison is case-insensitive.
   * If the tab exists, it returns all cell values from that tab. If the tab does not exist,
   * an {@link OBException} is thrown. If the tab exists but has no data, an empty list is returned.
   * </p>
   *
   * @param sheetId  the ID of the Google Spreadsheet (not the full URL)
   * @param tabName  the name of the tab (sheet) to search for (case-insensitive)
   * @param token    a valid OAuth 2.0 access token with Sheets read access
   * @return a list of rows from the tab; each row is a list of cell values. Returns an empty list if the tab is found but contains no data.
   *
   * @throws OBException if the specified tab name does not exist in the spreadsheet
   * @throws IOException if an error occurs while communicating with the Google Sheets API
   */
  public static List<List<Object>> findSpreadsheetAndTab(String sheetId, String tabName, String token) throws IOException {
    Sheets sheetsService = GoogleServiceUtil.getSheetsService(token);

    Spreadsheet spreadsheet = sheetsService.spreadsheets().get(sheetId).execute();
    List<Sheet> sheets = spreadsheet.getSheets();

    boolean foundTab = sheets.stream()
        .anyMatch(s -> tabName.equalsIgnoreCase(s.getProperties().getTitle()));

    if (!foundTab) {
      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_TabNotFound",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(String.format(errorMessage, tabName));
    }

    ValueRange response = sheetsService.spreadsheets().values()
        .get(sheetId, tabName)
        .execute();

    List<List<Object>> values = response.getValues();

    if (values == null || values.isEmpty()) {
      log4j.warn("Empty tab: {}", tabName);
      return List.of();
    } else {
      log4j.debug("Obtained rows: {}", values.size());
      return values;
    }
  }

  /**
   * Reads cell values from a specified range in a Google Spreadsheet.
   * <p>
   * This method uses the Google Sheets API to retrieve the contents of a given range
   * from a spreadsheet identified by its file ID. If no range is provided, it defaults to {@code "A1:Z1000"}.
   * The method returns the values as a list of rows, where each row is a list of cell values.
   * </p>
   *
   * @param accessToken a valid OAuth 2.0 access token with permission to read from Google Sheets (e.g., {@code https://www.googleapis.com/auth/spreadsheets.readonly})
   * @param fileId      the ID of the Google Spreadsheet to read from
   * @param range       the A1-notation range to read (e.g., {@code "Sheet1!A1:C10"}); if blank or null, defaults to {@code "A1:Z1000"}
   * @return            a {@link List} of rows, where each row is a {@link List} of cell values ({@code Object})
   *
   * @throws IOException if a network error occurs or the API request fails
   */
  public static List<List<Object>> readSheet(String accessToken, String fileId, String range) throws IOException {
    HttpTransport httpTransport = new NetHttpTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

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
   * Retrieves a list of accessible files from the user's Google Drive, filtered by a simplified file type keyword.
   * <p>
   * This method maps a user-friendly type keyword (e.g., {@code "spreadsheet"}, {@code "doc"}, {@code "slides"}, {@code "pdf"})
   * to its corresponding MIME type and queries the Google Drive API for matching files. It simplifies Drive file filtering
   * for common file types supported by Google Workspace and standard uploads.
   * </p>
   *
   * <p>Supported keywords and their corresponding file types:</p>
   * <ul>
   *   <li>{@code "spreadsheet"} → Google Sheets ({@code application/vnd.google-apps.spreadsheet})</li>
   *   <li>{@code "doc"} → Google Docs ({@code application/vnd.google-apps.document})</li>
   *   <li>{@code "slides"} → Google Slides ({@code application/vnd.google-apps.presentation})</li>
   *   <li>{@code "pdf"}, {@code "pdfs"} → PDF files uploaded to Drive ({@code application/pdf})</li>
   * </ul>
   *
   * @param type         a simplified keyword representing the file type to retrieve
   * @param accessToken  a valid OAuth 2.0 access token with access to the user's Drive (e.g., {@code drive.file} or {@code drive.readonly})
   * @return             a {@link JSONArray} containing the matching files, where each file is represented as a {@link JSONObject}
   *                     with fields {@code id}, {@code name}, and {@code mimeType}
   *
   * @throws IllegalArgumentException if the provided type keyword is not supported
   * @throws IOException              if a network or API error occurs during the request
   * @throws JSONException            if an error occurs parsing the API response
   */
  public static JSONArray listAccessibleFiles(String type, String accessToken) throws JSONException, IOException {
    String mimeType;

    switch (type.toLowerCase()) {
      case SPREADSHEET:
        mimeType = "application/vnd.google-apps.spreadsheet";
        break;
      case DOC:
        mimeType = "application/vnd.google-apps.document";
        break;
      case SLIDES:
        mimeType = "application/vnd.google-apps.presentation";
        break;
      case PDF:
      case PDFS:
        mimeType = "application/pdf";
        break;
      default:
        String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_UnsupportedFileType",
            OBContext.getOBContext().getLanguage().getLanguage());
        errorMessage = String.format(errorMessage, type);

        throw new IllegalArgumentException(errorMessage);
    }
    return listAccessibleFilesByMimeType(mimeType, accessToken);
  }

   /**
   * Retrieves a list of files from the user's Google Drive that match a specific MIME type.
   * <p>
   * This method sends a GET request to the Google Drive API's {@code /drive/v3/files} endpoint,
   * using a query parameter to filter files by the specified MIME type. It returns up to 100 files
   * that the authenticated user has access to.
   * </p>
   *
   * @param mimeType     the MIME type to filter files by (e.g., {@code application/vnd.google-apps.spreadsheet} for Google Sheets)
   * @param accessToken  a valid OAuth 2.0 access token with appropriate permissions (e.g., {@code drive.file} or {@code drive.readonly})
   * @return             a {@link JSONArray} containing metadata for the matching files; each file is represented
   *                     as a {@link JSONObject} with keys {@code id}, {@code name}, and {@code mimeType}
   *
   * @throws IOException   if a network error occurs while sending or receiving the API request
   * @throws JSONException if there is an error parsing the API response
   * @throws OBException   if the API returns a non-200 HTTP status code, indicating a request failure
   */
  private static JSONArray listAccessibleFilesByMimeType(String mimeType, String accessToken) throws IOException, JSONException {
    String endpoint = "https://www.googleapis.com/drive/v3/files" +
        "?q=mimeType='" + mimeType + "'" +
        "&fields=files(id,name,mimeType)" +
        "&pageSize=100";

    URL url = new URL(endpoint);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty(AUTHORIZATION, BEARER + accessToken);
    conn.setRequestProperty("Accept", "application/json");

    int status = conn.getResponseCode();
    if (status != 200) {
      throw new OBException("Error getting files: " + status);
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
   * <p>
   * This method sends a POST request to the Google Drive API to create a file in the root
   * directory of the user's Drive. The file is created with the given name and MIME type.
   * It supports both Google Workspace file types (like spreadsheets, documents, and presentations)
   * and standard MIME types (like plain text or PDF).
   * </p>
   *
   * <p>Supported Google Workspace MIME types include:
   * <ul>
   *   <li>{@code application/vnd.google-apps.spreadsheet} – Google Sheets</li>
   *   <li>{@code application/vnd.google-apps.document} – Google Docs</li>
   *   <li>{@code application/vnd.google-apps.presentation} – Google Slides</li>
   * </ul>
   * </p>
   *
   * @param name         the desired name for the new file
   * @param mimeType     the MIME type of the file to be created; determines the file type in Drive
   * @param accessToken  a valid OAuth 2.0 access token with sufficient permissions (e.g., {@code drive.file} scope)
   * @return             a {@link JSONObject} containing metadata of the created file, such as {@code id}, {@code name}, and {@code mimeType}
   *
   * @throws IOException   if an I/O error occurs during communication with the API
   * @throws JSONException if there is an error constructing the request or parsing the response
   * @throws OBException   if the API returns a non-200 HTTP status code, indicating the file creation failed
   */
  public static JSONObject createDriveFile(String name, String mimeType, String accessToken) throws IOException, JSONException {
    URL url = new URL("https://www.googleapis.com/drive/v3/files");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty(AUTHORIZATION, BEARER + accessToken);
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    conn.setDoOutput(true);

    String body = new JSONObject()
        .put("name", name)
        .put("mimeType", mimeType)
        .toString();

    try (OutputStream os = conn.getOutputStream()) {
      os.write(body.getBytes(StandardCharsets.UTF_8));
    }

    if (conn.getResponseCode() == 401) {
      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_401RefreshToken",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(errorMessage);
    }
    if (conn.getResponseCode() != 200) {
      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_FailedToCreateFile",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(String.format(errorMessage, conn.getResponseCode()));
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
   * Updates the content of a Google Spreadsheet within a specified cell range.
   * <p>
   * This method sends a PUT request to the Google Sheets API to overwrite the values
   * in the given range with the provided 2D list of objects. The update is performed using
   * the "RAW" input option, meaning values are written exactly as they are passed without formatting.
   * </p>
   *
   * @param fileId       the unique identifier of the Google Spreadsheet (found in the URL of the sheet)
   * @param accessToken  a valid OAuth 2.0 access token with permissions to edit the spreadsheet
   * @param range        the A1 notation of the range to update (e.g., "Sheet1!A1:C5")
   * @param values       a 2D list of values to write, where each inner list represents a row
   * @return             a {@link JSONObject} containing the response from the Google Sheets API,
   *                     including updated range and cell count information
   *
   * @throws IOException   if an I/O error occurs while sending or receiving data
   * @throws JSONException if there is an error parsing the API response or constructing the request body
   * @throws OBException   if the API responds with a non-200 HTTP status code, indicating failure
   */
  public static JSONObject updateSpreadsheetValues(String fileId, String accessToken, String range,
       List<List<Object>> values) throws IOException, JSONException {
    URL url = new URL("https://sheets.googleapis.com/v4/spreadsheets/" +
        fileId + "/values/" + range + "?valueInputOption=RAW");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty(AUTHORIZATION, BEARER + accessToken);
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

    if (conn.getResponseCode() == 401) {
      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_401RefreshToken",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(errorMessage);
    }
    if (conn.getResponseCode() != 200) {
      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_FailedToUpdateSheet",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(String.format(errorMessage, conn.getResponseCode()));
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
}
