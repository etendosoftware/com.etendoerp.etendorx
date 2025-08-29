package com.etendoerp.etendorx.utils;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility class for interacting with the Google Sheets and Drive APIs using a Bearer token.
 * Provides methods for authenticating with Google services, extracting spreadsheet information,
 * and reading tabular data from Sheets.
 */
public class GoogleServiceUtil {

  public static final String BEARER = "Bearer ";
  public static final String SPREADSHEET = "spreadsheet";
  public static final String DOC = "doc";
  public static final String SLIDES = "slides";
  public static final String PDF = "pdf";
  public static final String PDFS = "pdfs";
  public static final String AUTHORIZATION = "Authorization";
  public static final String APPLICATION_JSON = "application/json";
  public static final String ACCEPT = "Accept";
  public static final String MIMETYPE_SPREADSHEET = "application/vnd.google-apps.spreadsheet";
  public static final String MIMETYPE_DOC = "application/vnd.google-apps.document";
  public static final String MIMETYPE_SLIDES = "application/vnd.google-apps.presentation";
  public static final String MIMETYPE_PDF = "application/pdf";
  private static final String APPLICATION_NAME = "Google Sheets Java Integration";
  private static final Pattern SHEET_ID = Pattern.compile(
      "(?:https?://)?(?:docs\\.google\\.com/(?:spreadsheets|a/[^/]+/spreadsheets)/d/|drive\\.google\\.com/open\\?id=)([A-Za-z0-9_-]+)"
  );
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final Logger LOG = LogManager.getLogger(GoogleServiceUtil.class);

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
    return request -> {
      HttpHeaders headers = new HttpHeaders();
      headers.setAuthorization(BEARER + accessToken);
      request.setHeaders(headers);

      // Timeouts razonables
      request.setConnectTimeout(20_000);
      request.setReadTimeout(60_000);

      // Backoff automático para 429/5xx
      request.setUnsuccessfulResponseHandler(
          new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()));
    };
  }

  /**
   * Retrieves the name (title) of a specific tab (sheet) in a Google Spreadsheet by its index.
   *
   * <p>This method performs the following steps:
   * <ul>
   *   <li>Validates or refreshes the given access token associated with the specified account.</li>
   *   <li>Builds an authorized Google Sheets service instance.</li>
   *   <li>Fetches the spreadsheet data using the provided {@code sheetId}.</li>
   *   <li>Checks whether the spreadsheet contains any tabs (sheets).</li>
   *   <li>Ensures that the given index is within the valid range of existing sheets.</li>
   *   <li>Returns the title of the sheet at the specified index.</li>
   * </ul>
   *
   * @param index     Zero-based index of the tab to retrieve.
   * @param sheetId   The ID of the target Google Spreadsheet.
   * @param token     The access token used to authenticate with the Google Sheets API.
   * @param accountID The ID of the account used to validate or refresh the token.
   * @return The title of the sheet at the given index.
   * @throws OBException If the spreadsheet has no sheets or if the index is out of bounds.
   * @throws IOException If an error occurs while communicating with the Google Sheets API.
   */
  public static String getTabName(int index, String sheetId, ETRXTokenInfo token, String accountID) throws OBException, IOException {
    try {
      ETRXTokenInfo validToken = getValidAccessTokenOrRefresh(token, accountID);
      Sheets sheetsService = GoogleServiceUtil.getSheetsService(validToken.getToken());
      Spreadsheet spreadsheet = sheetsService.spreadsheets().get(sheetId).execute();
      List<Sheet> sheets = spreadsheet.getSheets();
      if (sheets == null || sheets.isEmpty()) {
        String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_SheetHasNoTabs",
            OBContext.getOBContext().getLanguage().getLanguage());
        throw new OBException(errorMessage);
      }
      if (index < 0 || index >= sheets.size()) {
        String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_WrongTabNumber",
            OBContext.getOBContext().getLanguage().getLanguage());
        throw new OBException(errorMessage);
      }
      return sheets.get(index).getProperties().getTitle();
    } catch (Exception e) {
      LOG.error("Error reading tab name", e);
      throw new OBException("Failed to get tab name", e);
    }
  }

  /**
   * Retrieves a middleware access token associated with the given provider, scope, user,
   * and organization. If a valid token exists in the database, it will be returned;
   * otherwise, the method attempts to refresh the token before returning it.
   *
   * <p>The method performs the following steps:</p>
   * <ul>
   *   <li>Searches for an existing {@link ETRXTokenInfo} matching the specified
   *       {@code provider}, {@code scope}, {@code user}, and {@code organization}.</li>
   *   <li>Obtains the system identifier from {@link SystemInfo} to be used as the
   *       account ID. Logs a warning if the identifier is empty.</li>
   *   <li>Returns a valid token, refreshing it if required, via
   *       {@link #getValidAccessTokenOrRefresh(ETRXTokenInfo, String)}.</li>
   * </ul>
   *
   * @param provider the OAuth provider associated with the token (not {@code null}).
   * @param scope    the scope of the token, used to identify the middleware provider (case-insensitive).
   * @param user     the user linked to the token.
   * @param org      the organization linked to the token.
   * @return a valid {@link ETRXTokenInfo} object containing access and refresh token information,
   * or {@code null} if no matching token exists and refresh cannot be performed.
   * @throws OBException if an error occurs while retrieving the system identifier
   *                     or during token validation/refresh.
   */
  public static ETRXTokenInfo getMiddlewareToken(ETRXoAuthProvider provider, String scope, User user, Organization org) {
    ETRXTokenInfo actualToken = (ETRXTokenInfo) OBDal.getInstance().createCriteria(ETRXTokenInfo.class)
        .add(Restrictions.eq(ETRXTokenInfo.PROPERTY_ETRXOAUTHPROVIDER, provider))
        .add(Restrictions.ilike(ETRXTokenInfo.PROPERTY_MIDDLEWAREPROVIDER, scope))
        .add(Restrictions.eq(ETRXTokenInfo.PROPERTY_ORGANIZATION, org))
        .add(Restrictions.eq(ETRXTokenInfo.PROPERTY_USER, user))
        .setMaxResults(1)
        .uniqueResult();
    if (actualToken == null) {
      return null;
    }
    String accountID = "";
    try {
      accountID = SystemInfo.getSystemIdentifier();
      if (StringUtils.isBlank(accountID)) {
        LOG.warn("[SSO] - Empty System Identifier, account id to middleware will be empty");
      }
    } catch (ServletException e) {
      throw new OBException(e);
    }
    return getValidAccessTokenOrRefresh(actualToken, accountID);
  }


  /**
   * Safely retrieves a cell value from a given row at the specified index.
   *
   * @param row   the row list containing cell values.
   * @param index the zero-based column index.
   * @return the cell value as a string, or an empty string if the cell is missing.
   */
  public static String getCellValue(List<Object> row, int index) {
    if (index >= 0 && index < row.size()) {
      return row.get(index).toString();
    }
    return "";
  }


  /**
   * Extracts the spreadsheet ID from a full Google Sheets URL.
   *
   * @param url the full URL of a Google Spreadsheet (e.g. <a href="https://docs.google.com/spreadsheets/d/SheetID/edit">https://docs.google.com/spreadsheets/d/SheetID/edit</a>).
   * @return the spreadsheet ID as a string.
   * @throws IllegalArgumentException if the URL format is invalid or the ID cannot be extracted.
   */
  public static String extractSheetIdFromUrl(String url) {
    Matcher m = SHEET_ID.matcher(url);
    if (m.find()) return m.group(1);
    String msg = Utility.messageBD(new DalConnectionProvider(), "ETRX_WrongSheetURL",
        OBContext.getOBContext().getLanguage().getLanguage());
    throw new OBException(msg);
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
   * @param sheetId   the ID of the Google Spreadsheet (not the full URL)
   * @param tabName   the name of the tab (sheet) to search for (case-insensitive)
   * @param token     a valid OAuth 2.0 access token with Sheets read access
   * @param accountID the internal account identifier used to refresh the token if necessary.
   * @return a list of rows from the tab; each row is a list of cell values. Returns an empty list if the tab is found but contains no data.
   * @throws OBException if the specified tab name does not exist in the spreadsheet
   * @throws IOException if an error occurs while communicating with the Google Sheets API
   */
  public static List<List<Object>> findSpreadsheetAndTab(String sheetId, String tabName,
                                                         ETRXTokenInfo token, String accountID) throws IOException {
    ETRXTokenInfo validToken = getValidAccessTokenOrRefresh(token, accountID);
    Sheets sheetsService = GoogleServiceUtil.getSheetsService(validToken.getToken());
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
      LOG.warn("Empty tab: {}", tabName);
      return List.of();
    } else {
      LOG.debug("Obtained rows: {}", values.size());
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
   * @param accountID   the internal account identifier used to refresh the token if necessary.
   * @param fileId      the ID of the Google Spreadsheet to read from
   * @param range       the A1-notation range to read (e.g., {@code "Sheet1!A1:C10"}); if blank or null, defaults to {@code "A1:Z1000"}
   * @return a {@link List} of rows, where each row is a {@link List} of cell values ({@code Object})
   * @throws IOException if a network error occurs or the API request fails
   */
  public static List<List<Object>> readSheet(ETRXTokenInfo accessToken, String accountID, String fileId, String range)
      throws IOException {
    HttpTransport httpTransport = new NetHttpTransport();
    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    ETRXTokenInfo validToken = getValidAccessTokenOrRefresh(accessToken, accountID);
    Sheets service = new Sheets.Builder(httpTransport, jsonFactory, bearerTokenInitializer(validToken.getToken()))
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
   * @param type        a simplified keyword representing the file type to retrieve
   * @param accessToken a valid OAuth 2.0 access token with access to the user's Drive
   *                    (e.g., {@code drive.file} or {@code drive.readonly})
   * @param accountID   the internal account identifier used to refresh the token if necessary.
   * @return a {@link JSONArray} containing the matching files,
   * where each file is represented as a {@link JSONObject}
   * with fields {@code id}, {@code name}, and {@code mimeType}
   * @throws IllegalArgumentException if the provided type keyword is not supported
   * @throws IOException              if a network or API error occurs during the request
   * @throws JSONException            if an error occurs parsing the API response
   */
  public static JSONArray listAccessibleFiles(String type, ETRXTokenInfo accessToken, String accountID)
      throws JSONException, IOException {
    String mimeType;
    switch (type.toLowerCase()) {
      case SPREADSHEET:
        mimeType = MIMETYPE_SPREADSHEET;
        break;
      case DOC:
        mimeType = MIMETYPE_DOC;
        break;
      case SLIDES:
        mimeType = MIMETYPE_SLIDES;
        break;
      case PDF:
      case PDFS:
        mimeType = MIMETYPE_PDF;
        break;
      default:
        String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_UnsupportedFileType",
            OBContext.getOBContext().getLanguage().getLanguage());
        errorMessage = String.format(errorMessage, type);

        throw new IllegalArgumentException(errorMessage);
    }
    return listAccessibleFilesByMimeType(mimeType, accessToken, accountID);
  }

  /**
   * Retrieves a list of files from the user's Google Drive that match a specific MIME type.
   * <p>
   * This method sends a GET request to the Google Drive API's {@code /drive/v3/files} endpoint,
   * using a query parameter to filter files by the specified MIME type. It returns up to 100 files
   * that the authenticated user has access to.
   * </p>
   *
   * @param mimeType    the MIME type to filter files by (e.g., {@code application/vnd.google-apps.spreadsheet} for Google Sheets)
   * @param accessToken a valid OAuth 2.0 access token with appropriate permissions (e.g., {@code drive.file} or {@code drive.readonly})
   * @param accountID   the internal account identifier used to refresh the token if necessary.
   * @return a {@link JSONArray} containing metadata for the matching files; each file is represented
   * as a {@link JSONObject} with keys {@code id}, {@code name}, and {@code mimeType}
   * @throws IOException   if a network error occurs while sending or receiving the API request
   * @throws JSONException if there is an error parsing the API response
   * @throws OBException   if the API returns a non-200 HTTP status code, indicating a request failure
   */
  protected static JSONArray listAccessibleFilesByMimeType(String mimeType, ETRXTokenInfo accessToken, String accountID)
      throws IOException, JSONException {

    ETRXTokenInfo valid = getValidAccessTokenOrRefresh(accessToken, accountID);
    Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, bearerTokenInitializer(valid.getToken()))
        .setApplicationName(APPLICATION_NAME)
        .build();

    FileList page = drive.files().list()
        .setQ("mimeType='" + mimeType + "'")
        .setFields("nextPageToken, files(id,name,mimeType)")
        .setPageSize(100)
        .setSupportsAllDrives(true)
        .setIncludeItemsFromAllDrives(true)
        .setCorpora("allDrives")
        .execute();

    JSONArray out = new JSONArray();
    if (page.getFiles() != null) {
      for (File f : page.getFiles()) {
        out.put(new JSONObject()
            .put("id", f.getId())
            .put("name", f.getName())
            .put("mimeType", f.getMimeType()));
      }
    }
    return out;
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
   * @param name        the desired name for the new file
   * @param mimeType    the MIME type of the file to be created; determines the file type in Drive
   * @param accessToken a valid OAuth 2.0 access token with sufficient permissions (e.g., {@code drive.file} scope)
   * @param accountID   the internal account identifier used to refresh the token if necessary.
   * @return a {@link JSONObject} containing metadata of the created file, such as {@code id}, {@code name}, and {@code mimeType}
   * @throws IOException   if an I/O error occurs during communication with the API
   * @throws JSONException if there is an error constructing the request or parsing the response
   * @throws OBException   if the API returns a non-200 HTTP status code, indicating the file creation failed
   */
  public static JSONObject createDriveFile(String name, String mimeType, ETRXTokenInfo accessToken, String accountID)
      throws IOException, JSONException {
    ETRXTokenInfo valid = getValidAccessTokenOrRefresh(accessToken, accountID);
    Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, bearerTokenInitializer(valid.getToken()))
        .setApplicationName(APPLICATION_NAME)
        .build();

    if (!mimeType.startsWith("application/vnd.google-apps.")) {
      String msg = Utility.messageBD(new DalConnectionProvider(), "ETRX_FailedToCreateFile",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(String.format(msg, 400) + " - Non Google apps mimeType requires media upload");
    }

    File meta = new File();
    meta.setName(name);
    meta.setMimeType(mimeType);

    File created = drive.files().create(meta)
        .setFields("id,name,mimeType")
        .execute();

    return new JSONObject()
        .put("id", created.getId())
        .put("name", created.getName())
        .put("mimeType", created.getMimeType());
  }


  /**
   * Validates the token and refreshes it automatically if expired.
   *
   * @param accessToken The current access token
   * @param accountId   The account ID used for refreshing (needed for /refresh-token)
   * @return A valid access token (either original or refreshed)
   */
  public static ETRXTokenInfo getValidAccessTokenOrRefresh(ETRXTokenInfo accessToken, String accountId) {
    final long SKEW_MS = 120_000L; // 2 minutos de margen
    try {
      Date vu = accessToken.getValidUntil();
      long now = System.currentTimeMillis();

      // Si tenemos validUntil y queda margen, usamos el token tal cual
      if (vu != null && (vu.getTime() - now) > SKEW_MS) {
        return accessToken;
      }

      LOG.warn(() -> Utility.messageBD(
          new DalConnectionProvider(), "ETRX_RefreshingToken",
          OBContext.getOBContext().getLanguage().getLanguage()
      ));
      accessToken.setToken(refreshAccessToken(accountId));

      // Renovamos 58 min por defecto (ajustá si tu middleware devuelve la expiración real)
      Calendar cal = Calendar.getInstance();
      cal.setTime(new Date());
      cal.add(Calendar.MINUTE, 58);
      accessToken.setValidUntil(cal.getTime());

      OBDal.getInstance().save(accessToken);
      return accessToken;
    } catch (Exception e) {
      throw new OBException("Failed to ensure a valid access token", e);
    }
  }


  /**
   * Checks if the provided OAuth 2.0 access token is valid and not expired.
   * Uses Google's public token info endpoint.
   *
   * @param accessToken the OAuth access token to validate
   * @throws OBException if the token is invalid or expired
   */
  public static void validateAccessToken(String accessToken) throws OBException {
    try {
      URL url = new URL("https://oauth2.googleapis.com/tokeninfo?access_token=" +
          URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty(ACCEPT, APPLICATION_JSON);

      int status = conn.getResponseCode();
      if (status != HttpURLConnection.HTTP_OK) {
        String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_ExpiredToken",
            OBContext.getOBContext().getLanguage().getLanguage());
        throw new OBException(errorMessage);
      }
    } catch (IOException e) {
      throw new OBException("Error validating access token: " + e.getMessage(), e);
    }
  }

  /**
   * Refreshes the access token using the refresh_token stored in EtendoMiddleware.
   *
   * @param accountId the ID used to associate the refresh_token (e.g. user/org ID)
   * @return the new access token
   * @throws OBException if the refresh fails
   */
  public static String refreshAccessToken(String accountId) throws OBException {
    try {
      String middlewareUrl = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("sso.middleware.url");
      String refreshUrl = middlewareUrl + "/oauth-integrations/refresh-token?account_id=" + accountId;

      HttpURLConnection conn = (HttpURLConnection) new URL(refreshUrl).openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty(ACCEPT, APPLICATION_JSON);
      conn.setConnectTimeout(20_000);
      conn.setReadTimeout(60_000);

      String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_ErrorRefreshingAccessToken",
          OBContext.getOBContext().getLanguage().getLanguage());

      JSONObject json = getResponseJSONObject(conn, errorMessage);
      return json.getString("access_token");
    } catch (Exception e) {
      throw new OBException("Could not refresh access token: " + e.getMessage(), e);
    }
  }


  /**
   * Reads the response from an HTTP connection and returns it as a {@link JSONObject}.
   * <p>
   * This method checks the response status code. If it is not 200 (OK), it throws an {@link OBException}
   * with a custom error message containing the status code and response message. If the status code is 200,
   * the response body is read from the input stream and converted into a {@link JSONObject}.
   * </p>
   *
   * @param conn         the {@link HttpURLConnection} from which the response will be read.
   * @param errorMessage a custom error message to be included in the exception if the status code is not 200.
   * @return a {@link JSONObject} representing the response body.
   * @throws IOException   if an I/O error occurs while reading from the connection.
   * @throws JSONException if the response body cannot be converted into a {@link JSONObject}.
   * @throws OBException   if the status code is not 200, indicating a failure in the request.
   */
  private static JSONObject getResponseJSONObject(HttpURLConnection conn, String errorMessage) throws IOException, JSONException {
    int status = conn.getResponseCode();
    if (status != 200) {
      throw new OBException(errorMessage + " - " + status + ": " + conn.getResponseMessage());
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      return new JSONObject(response.toString());
    }
  }


  /**
   * Updates the content of a Google Spreadsheet within a specified cell range.
   * <p>
   * This method sends a PUT request to the Google Sheets API to overwrite the values
   * in the given range with the provided 2D list of objects. The update is performed using
   * the "RAW" input option, meaning values are written exactly as they are passed without formatting.
   * </p>
   *
   * @param fileId      the unique identifier of the Google Spreadsheet (found in the URL of the sheet)
   * @param accessToken a valid OAuth 2.0 access token with permissions to edit the spreadsheet
   * @param accountID   the internal account identifier used to refresh the token if necessary
   * @param range       the A1 notation of the range to update (e.g., "Sheet1!A1:C5")
   * @param values      a 2D list of values to write, where each inner list represents a row
   * @return a {@link JSONObject} containing the response from the Google Sheets API,
   * including updated range and cell count information
   * @throws IOException   if an I/O error occurs while sending or receiving data
   * @throws JSONException if there is an error parsing the API response or constructing the request body
   * @throws OBException   if the API responds with a non-200 HTTP status code, indicating failure
   */
  public static JSONObject updateSpreadsheetValues(String fileId, ETRXTokenInfo accessToken, String accountID, String range,
                                                   List<List<Object>> values) throws IOException, JSONException {
    ETRXTokenInfo valid = getValidAccessTokenOrRefresh(accessToken, accountID);
    Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, bearerTokenInitializer(valid.getToken()))
        .setApplicationName(APPLICATION_NAME)
        .build();

    ValueRange body = new ValueRange()
        .setRange(range)
        .setMajorDimension("ROWS")
        .setValues(values);

    UpdateValuesResponse resp = service.spreadsheets().values()
        .update(fileId, range, body)
        .setValueInputOption("RAW")          // mantenemos comportamiento actual
        .execute();

    return new JSONObject()
        .put("updatedRange", resp.getUpdatedRange())
        .put("updatedRows", resp.getUpdatedRows())
        .put("updatedColumns", resp.getUpdatedColumns())
        .put("updatedCells", resp.getUpdatedCells());
  }
}
