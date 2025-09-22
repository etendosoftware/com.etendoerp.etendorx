package com.etendoerp.etendorx.utils;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Criterion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Language;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link GoogleServiceUtil} utility class, which provides methods
 * for interacting with Google Sheets and Drive.
 *
 * <p>This test class uses JUnit 5 and Mockito to validate the behavior of
 * utility methods related to:
 * <ul>
 *   <li>Extracting sheet IDs from URLs</li>
 *   <li>Accessing and validating Google Sheet tab names</li>
 *   <li>Reading spreadsheet data</li>
 *   <li>Listing accessible Google Drive files</li>
 *   <li>Handling localization and exception messages through OBContext and Utility</li>
 * </ul>
 *
 * <p>The class includes mocked interactions with static methods using {@link MockedStatic},
 * and simulates Google Sheets API responses using deep stubs.
 *
 * @see GoogleServiceUtil
 */
@ExtendWith(MockitoExtension.class)
class GoogleServiceUtilTest {

  private static final ETRXTokenInfo TOKEN = token("mockAccessToken");
  private static final String ACCOUNT_ID = "etendo123";
  private static final String SHEET_TITLE = "MiHoja";

  private static ETRXTokenInfo token(String value) {
    ETRXTokenInfo t = new ETRXTokenInfo();
    t.setToken(value);
    // opcional, por si tu código usa la validez
    t.setValidUntil(new Date(System.currentTimeMillis() + 60 * 60 * 1000));
    return t;
  }

  /**
   * Verifies that a valid Google Sheets URL returns the correct sheet ID.
   */
  @Test
  void testExtractSheetIdFromUrlValidUrlReturnsId() {
    String id = GoogleServiceUtil.extractSheetIdFromUrl(
        "https://docs.google.com/spreadsheets/d/abc123def456/edit#gid=0"
    );
    assertEquals("abc123def456", id);
  }

  /**
   * Verifies that an invalid Google Sheets URL throws an IllegalArgumentException.
   */
  @Test
  void testExtractSheetIdFromUrlInvalidUrlThrowsException() {
    try (var lh = new LocaleHelper("ETRX_WrongSheetURL", "bad‐url")) {
      OBException ex = assertThrows(
          OBException.class,
          () -> GoogleServiceUtil.extractSheetIdFromUrl("https://foo/not-sheet")
      );
      assertTrue(ex.getMessage().contains("bad‐url"));
    }
  }

  /**
   * Verifies that getCellValue returns the correct value when the index is in range.
   */
  @Test
  void testGetCellValueWithinBoundsReturnsValue() {
    assertEquals("B",
        GoogleServiceUtil.getCellValue(List.of("A", "B", "C"), 1)
    );
  }

  /**
   * Verifies that getCellValue returns an empty string when index is out of bounds.
   */
  @Test
  void testGetCellValueOutOfBoundsReturnsEmptyString() {
    assertEquals("",
        GoogleServiceUtil.getCellValue(List.of("A", "B", "C"), 99)
    );
  }

  /**
   * Mocks and verifies that listAccessibleFiles returns a valid JSONArray of spreadsheet files.
   */
  @Test
  void testListAccessibleFilesWithValidTypeReturnsJSONArray() throws Exception {
    JSONArray mockResp = new JSONArray()
        .put(new JSONObject()
            .put("id", "1")
            .put("name", "Sheet 1")
            .put("mimeType", GoogleServiceUtil.MIMETYPE_SPREADSHEET)
        );

    try (var gs = mockStatic(GoogleServiceUtil.class)) {
      gs.when(() -> GoogleServiceUtil.listAccessibleFiles("spreadsheet", TOKEN, ACCOUNT_ID))
          .thenReturn(mockResp);

      JSONArray result = GoogleServiceUtil.listAccessibleFiles("spreadsheet", TOKEN, ACCOUNT_ID);

      assertSame(mockResp, result);
      assertEquals(1, result.length());
      assertEquals("Sheet 1", result.getJSONObject(0).getString("name"));
    }
  }

  /**
   * Verifies that getTabName returns the correct sheet tab title.
   */
  @Test
  void testGetTabNameValidIndexReturnsTabName() throws Exception {
    String sheetId = "sid", title = "MySheet";

    try (var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, ACCOUNT_ID))
          .thenReturn(TOKEN);

      Sheets sheetsSvc = mock(Sheets.class, RETURNS_DEEP_STUBS);

      // Devolvemos una lista real con una Sheet real
      Sheet s = new Sheet().setProperties(new SheetProperties().setTitle(title));
      when(sheetsSvc.spreadsheets().get(sheetId).execute().getSheets())
          .thenReturn(List.of(s));

      gs.when(() -> GoogleServiceUtil.getSheetsService(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(sheetsSvc);

      assertEquals(title, GoogleServiceUtil.getTabName(0, sheetId, TOKEN, ACCOUNT_ID));
    }
  }

  /**
   * Verifies that findSpreadsheetAndTab returns spreadsheet data when the tab exists.
   */
  @Test
  void testFindSpreadsheetAndTabTabExistsReturnsValues() throws Exception {
    String sheetId = "sid", tab = "T1";
    List<List<Object>> data = List.of(List.of("A", "B"), List.of("C", "D"));

    try (var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, ACCOUNT_ID))
          .thenReturn(TOKEN);

      Sheets sheetsSvc = mock(Sheets.class, RETURNS_DEEP_STUBS);
      Sheet fakeSheet = new Sheet().setProperties(new SheetProperties().setTitle(tab));

      when(sheetsSvc.spreadsheets().get(sheetId).execute().getSheets())
          .thenReturn(List.of(fakeSheet));
      when(sheetsSvc.spreadsheets().values().get(sheetId, tab).execute().getValues())
          .thenReturn(data);

      gs.when(() -> GoogleServiceUtil.getSheetsService(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(sheetsSvc);

      assertEquals(data,
          GoogleServiceUtil.findSpreadsheetAndTab(sheetId, tab, TOKEN, ACCOUNT_ID)
      );
    }
  }


  /**
   * Verifies that an OBException is thrown when the specified tab is not found.
   */
  @Test
  void testFindSpreadsheetAndTabTabNotFoundThrowsOBException() throws Exception {
    try (var lh = new LocaleHelper("ETRX_TabNotFound", "no encontrada");
         var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, ACCOUNT_ID))
          .thenReturn(TOKEN);

      Sheets sheetsSvc = mock(Sheets.class, RETURNS_DEEP_STUBS);
      when(sheetsSvc.spreadsheets()
          .get("sid")
          .execute()
          .getSheets()
      ).thenReturn(List.of(
          new Sheet().setProperties(
              new SheetProperties().setTitle("Otra")
          )
      ));

      gs.when(() -> GoogleServiceUtil.getSheetsService(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(sheetsSvc);

      OBException ex = assertThrows(OBException.class, () ->
          GoogleServiceUtil.findSpreadsheetAndTab(
              "sid", "NoExiste", TOKEN, ACCOUNT_ID
          )
      );
      assertTrue(ex.getMessage().contains("no encontrada"));
    }
  }

  /**
   * Verifies that an empty list is returned when no values are found in the sheet tab.
   */
  @Test
  void testFindSpreadsheetAndTabEmptyValuesReturnsEmptyList() throws Exception {
    try (var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, ACCOUNT_ID))
          .thenReturn(TOKEN);

      Sheets sheetsSvc = mock(Sheets.class, RETURNS_DEEP_STUBS);
      when(sheetsSvc.spreadsheets()
          .get("sid")
          .execute()
          .getSheets()
      ).thenReturn(List.of(
          new Sheet().setProperties(
              new SheetProperties().setTitle(SHEET_TITLE)
          )
      ));
      when(sheetsSvc.spreadsheets()
          .values()
          .get("sid", SHEET_TITLE)
          .execute()
          .getValues()
      ).thenReturn(null);

      gs.when(() -> GoogleServiceUtil.getSheetsService(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(sheetsSvc);

      assertTrue(GoogleServiceUtil.findSpreadsheetAndTab(
          "sid", SHEET_TITLE, TOKEN, ACCOUNT_ID
      ).isEmpty());
    }
  }

  /**
   * Verifies that an invalid file type passed to listAccessibleFiles throws an IllegalArgumentException.
   */
  @Test
  void testListAccessibleFilesInvalidTypeThrowsException() {
    try (var lh = new LocaleHelper("ETRX_UnsupportedFileType", "bad‐type")) {
      IllegalArgumentException ex = assertThrows(
          IllegalArgumentException.class,
          () -> GoogleServiceUtil.listAccessibleFiles("XLS", TOKEN, ACCOUNT_ID)
      );
      assertTrue(ex.getMessage().contains("bad‐type"));
    }
  }

  // === Extra coverage: extractSheetIdFromUrl con variantes de URL ===

  @Test
  void testExtractSheetIdFromUrl_SupportsDriveOpenId() {
    String id = GoogleServiceUtil.extractSheetIdFromUrl(
        "https://drive.google.com/open?id=XYZ_123-ABC"
    );
    assertEquals("XYZ_123-ABC", id);

  }

  @Test
  void testExtractSheetIdFromUrl_SupportsDomainAPath() {
    String id = GoogleServiceUtil.extractSheetIdFromUrl(
        "https://docs.google.com/a/mi-empresa.com/spreadsheets/d/IDdominio_987/edit"
    );
    assertEquals("IDdominio_987", id);
  }

// === Extra coverage: getCellValue índice negativo (ver nota más abajo) ===

  @Test
  void testGetCellValueNegativeIndex() {
    assertEquals("", GoogleServiceUtil.getCellValue(List.of("A", "B"), -1));
  }


// === Extra coverage: getTabName errores ===

  @Test
  void testGetTabNameWhenNoSheetsThrowsOBException() throws Exception {
    try (var lh = new LocaleHelper("ETRX_SheetHasNoTabs", "sin pestañas");
         var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, ACCOUNT_ID)).thenReturn(TOKEN);
      Sheets sheetsSvc = mock(Sheets.class, RETURNS_DEEP_STUBS);
      when(sheetsSvc.spreadsheets().get("sid").execute().getSheets()).thenReturn(null);
      gs.when(() -> GoogleServiceUtil.getSheetsService(any())).thenReturn(sheetsSvc);

      OBException ex = assertThrows(OBException.class,
          () -> GoogleServiceUtil.getTabName(0, "sid", TOKEN, ACCOUNT_ID));

      assertEquals("Failed to get tab name", ex.getMessage());
      assertTrue(ex.getCause() instanceof OBException);
      assertTrue(ex.getCause().getMessage().contains("sin pestañas"));
    }
  }

  @Test
  void testGetTabNameOutOfBoundsThrowsOBException() throws Exception {
    try (var lh = new LocaleHelper("ETRX_WrongTabNumber", "índice inválido");
         var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, ACCOUNT_ID)).thenReturn(TOKEN);
      Sheets sheetsSvc = mock(Sheets.class, RETURNS_DEEP_STUBS);
      Sheet s = new Sheet().setProperties(new SheetProperties().setTitle("S1"));
      when(sheetsSvc.spreadsheets().get("sid").execute().getSheets()).thenReturn(List.of(s));
      gs.when(() -> GoogleServiceUtil.getSheetsService(any())).thenReturn(sheetsSvc);

      OBException ex = assertThrows(OBException.class,
          () -> GoogleServiceUtil.getTabName(2, "sid", TOKEN, ACCOUNT_ID));

      assertEquals("Failed to get tab name", ex.getMessage());
      assertTrue(ex.getCause() instanceof OBException);
      assertTrue(ex.getCause().getMessage().contains("índice inválido"));
    }
  }


// === Extra coverage: getValidAccessTokenOrRefresh ===

  @Test
  void testGetValidAccessTokenOrRefresh_KeepsFreshTokenWithoutRefresh() {
    // validUntil >> ahora (10 min), NO debería refrescar
    ETRXTokenInfo fresh = token("freshToken");
    fresh.setValidUntil(new Date(System.currentTimeMillis() + 10 * 60 * 1000));

    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {
      // Si se invoca refreshAccessToken, tiramos para detectar mal uso
      gs.when(() -> GoogleServiceUtil.refreshAccessToken(any())).thenThrow(new AssertionError("Should not refresh"));

      // OBDal.save no importa en este camino, pero evitamos NPE si se llamara por error
      try (MockedStatic<OBDal> obdal = mockStatic(OBDal.class)) {
        OBDal ob = mock(OBDal.class);
        obdal.when(OBDal::getInstance).thenReturn(ob);

        ETRXTokenInfo result = GoogleServiceUtil.getValidAccessTokenOrRefresh(fresh, "acc");
        assertSame(fresh, result);
        assertEquals("freshToken", result.getToken());
      }
    }
  }

  @Test
  void testGetValidAccessTokenOrRefresh_RefreshesAndPersists() {
    // Expira en 30s -> debe refrescar
    ETRXTokenInfo expiring = token("old");
    expiring.setValidUntil(new Date(System.currentTimeMillis() + 30_000));

    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS);
         MockedStatic<OBDal> obdal = mockStatic(OBDal.class);
         var lh = new LocaleHelper("ETRX_RefreshingToken", "refreshing")) {

      gs.when(() -> GoogleServiceUtil.refreshAccessToken("acc")).thenReturn("newToken");

      OBDal ob = mock(OBDal.class);
      obdal.when(OBDal::getInstance).thenReturn(ob);

      ETRXTokenInfo result = GoogleServiceUtil.getValidAccessTokenOrRefresh(expiring, "acc");
      assertEquals("newToken", result.getToken());
      // validUntil debería ser ~+58 min
      long deltaMs = result.getValidUntil().getTime() - System.currentTimeMillis();
      assertTrue(deltaMs > 55 * 60_000L, "validUntil debería moverse ~58m");

      // se persiste
      org.mockito.Mockito.verify(ob).save(result);
    }
  }

// === Extra coverage: listAccessibleFiles mapea keywords y case-insensitive ===

  @Test
  void testListAccessibleFiles_PdfsKeywordMapsToPdfMime() throws Exception {
    JSONArray fake = new JSONArray().put(new JSONObject().put("id", "p").put("name", "Doc").put("mimeType", "application/pdf"));
    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      gs.when(() -> GoogleServiceUtil.listAccessibleFilesByMimeType(eq("application/pdf"), eq(TOKEN), eq(ACCOUNT_ID)))
          .thenReturn(fake);

      JSONArray out = GoogleServiceUtil.listAccessibleFiles("pdfs", TOKEN, ACCOUNT_ID);
      assertSame(fake, out);
    }
  }

  @Test
  void testListAccessibleFiles_CaseInsensitiveType() throws Exception {
    JSONArray fake = new JSONArray().put(new JSONObject().put("id", "1").put("name", "Doc 1").put("mimeType", "application/vnd.google-apps.document"));
    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      gs.when(() -> GoogleServiceUtil.listAccessibleFilesByMimeType(eq("application/vnd.google-apps.document"), eq(TOKEN), eq(ACCOUNT_ID)))
          .thenReturn(fake);

      JSONArray out = GoogleServiceUtil.listAccessibleFiles("DoC", TOKEN, ACCOUNT_ID);
      assertSame(fake, out);
    }
  }

// === Extra coverage: createDriveFile valida mimeType no-Google ===

  @Test
  void testCreateDriveFile_NonGoogleMimeTypeThrowsOBException() {
    try (var lh = new LocaleHelper("ETRX_FailedToCreateFile", "no soportado")) {
      OBException ex = assertThrows(OBException.class, () ->
          GoogleServiceUtil.createDriveFile("x", "application/pdf", TOKEN, ACCOUNT_ID));
      assertTrue(ex.getMessage().contains("no soportado"));
    }
  }

  @Test
  void testGetMiddlewareToken_NoStoredTokenReturnsNull() {
    try (MockedStatic<OBDal> obdal = mockStatic(OBDal.class)) {
      OBDal dal = mock(OBDal.class);
      @SuppressWarnings("unchecked")
      OBCriteria<ETRXTokenInfo> crit = mock(OBCriteria.class);

      obdal.when(OBDal::getInstance).thenReturn(dal);
      when(dal.createCriteria(ETRXTokenInfo.class)).thenReturn(crit);
      when(crit.add(any(Criterion.class))).thenReturn(crit);
      when(crit.setMaxResults(1)).thenReturn(crit);
      when(crit.uniqueResult()).thenReturn(null); // no hay token guardado

      assertNull(GoogleServiceUtil.getMiddlewareToken(null, "scope", null, null));
    }
  }

  @Test
  void testGetMiddlewareToken_ReturnsValidTokenAfterCheck() throws Exception {
    ETRXTokenInfo stored = new ETRXTokenInfo();
    stored.setToken("tok");
    stored.setValidUntil(new Date(System.currentTimeMillis() + 1_000)); // casi vencido

    try (MockedStatic<OBDal> obdal = mockStatic(OBDal.class);
         MockedStatic<SystemInfo> sys = mockStatic(SystemInfo.class);
         MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      OBDal dal = mock(OBDal.class);
      @SuppressWarnings("unchecked")
      OBCriteria<ETRXTokenInfo> crit = mock(OBCriteria.class);

      obdal.when(OBDal::getInstance).thenReturn(dal);
      when(dal.createCriteria(ETRXTokenInfo.class)).thenReturn(crit);
      when(crit.add(any(Criterion.class))).thenReturn(crit);
      when(crit.setMaxResults(1)).thenReturn(crit);
      when(crit.uniqueResult()).thenReturn(stored); // ¡ahora sí devolvemos el token!

      sys.when(SystemInfo::getSystemIdentifier).thenReturn("accid");
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(stored, "accid"))
          .thenReturn(stored);

      assertSame(stored, GoogleServiceUtil.getMiddlewareToken(null, "scope", null, null));
    }
  }

  @Test
  void testReadSheet_DefaultsRangeWhenBlank() throws Exception {
    List<List<Object>> values = List.of(List.of("r1c1"), List.of("r2c1"));
    Sheets svc = mock(Sheets.class, RETURNS_DEEP_STUBS);

    when(svc.spreadsheets().values().get("fid", "A1:Z1000").execute().getValues())
        .thenReturn(values);

    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS);
         MockedConstruction<Sheets.Builder> ctor = mockConstruction(
             Sheets.Builder.class,
             (mock, ctx) -> {
               when(mock.setApplicationName(any())).thenReturn(mock);
               when(mock.build()).thenReturn(svc);
             }
         )) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, "acc")).thenReturn(TOKEN);

      assertEquals(values, GoogleServiceUtil.readSheet(TOKEN, "acc", "fid", null));
    }
  }

  @Test
  void testReadSheet_CustomRange() throws Exception {
    List<List<Object>> values = List.of(List.of("x"));
    Sheets svc = mock(Sheets.class, RETURNS_DEEP_STUBS);
    when(svc.spreadsheets().values().get("fid", "S1!A1:B2").execute().getValues())
        .thenReturn(values);

    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS);
         MockedConstruction<Sheets.Builder> ctor = mockConstruction(
             Sheets.Builder.class,
             (mock, ctx) -> {
               when(mock.setApplicationName(any())).thenReturn(mock);
               when(mock.build()).thenReturn(svc);
             }
         )) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, "acc")).thenReturn(TOKEN);
      assertEquals(values, GoogleServiceUtil.readSheet(TOKEN, "acc", "fid", "S1!A1:B2"));
    }
  }

  @Test
  void testListAccessibleFilesByMimeType_ReturnsJSONArray() throws Exception {
    Drive drv = mock(Drive.class, RETURNS_DEEP_STUBS);

    File f = new File().setId("1").setName("My").setMimeType("m");
    FileList fl = new FileList().setFiles(List.of(f));
    when(drv.files().list()
        .setQ("mimeType='m'")
        .setFields("nextPageToken, files(id,name,mimeType)")
        .setPageSize(100)
        .setSupportsAllDrives(true)
        .setIncludeItemsFromAllDrives(true)
        .setCorpora("allDrives")
        .execute())
        .thenReturn(fl);

    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS);
         MockedConstruction<Drive.Builder> ctor = mockConstruction(
             Drive.Builder.class,
             (mock, ctx) -> {
               when(mock.setApplicationName(any())).thenReturn(mock);
               when(mock.build()).thenReturn(drv);
             }
         )) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, "acc")).thenReturn(TOKEN);

      JSONArray out = GoogleServiceUtil.listAccessibleFilesByMimeType("m", TOKEN, "acc");
      assertEquals(1, out.length());
      assertEquals("1", out.getJSONObject(0).getString("id"));
    }
  }

  @Test
  void testCreateDriveFile_GoogleMime_Succeeds() throws Exception {
    Drive drv = mock(Drive.class, RETURNS_DEEP_STUBS);
    File created = new File().setId("id123").setName("New").setMimeType("application/vnd.google-apps.spreadsheet");

    when(drv.files().create(any(File.class)).setFields("id,name,mimeType").execute())
        .thenReturn(created);

    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS);
         MockedConstruction<Drive.Builder> ctor = mockConstruction(
             Drive.Builder.class,
             (mock, ctx) -> {
               when(mock.setApplicationName(any())).thenReturn(mock);
               when(mock.build()).thenReturn(drv);
             }
         )) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, "acc")).thenReturn(TOKEN);

      var json = GoogleServiceUtil.createDriveFile("New", "application/vnd.google-apps.spreadsheet", TOKEN, "acc");
      assertEquals("id123", json.getString("id"));
      assertEquals("New", json.getString("name"));
    }
  }

  @Test
  void testValidateAccessToken_OkDoesNotThrow() throws Exception {
    FakeHttp conn = new FakeHttp();
    conn.code = HttpURLConnection.HTTP_OK;

    try (MockedConstruction<URL> ctor = mockConstruction(URL.class, (mock, ctx) ->
        when(mock.openConnection()).thenReturn(conn))) {
      assertDoesNotThrow(() -> GoogleServiceUtil.validateAccessToken("abc"));
    }
  }

  @Test
  void testValidateAccessToken_NonOkThrowsOBException() throws Exception {
    FakeHttp conn = new FakeHttp();
    conn.code = 401;
    conn.msg = "Unauthorized";

    try (MockedConstruction<URL> ctor = mockConstruction(URL.class, (mock, ctx) ->
        when(mock.openConnection()).thenReturn(conn));
         var lh = new LocaleHelper("ETRX_ExpiredToken", "token expirado")) {

      OBException ex = assertThrows(OBException.class,
          () -> GoogleServiceUtil.validateAccessToken("bad"));
      assertTrue(ex.getMessage().contains("token expirado"));
    }
  }

  @Test
  void testRefreshAccessToken_Success_UsesGetResponseJSONObject() throws Exception {
    FakeHttp conn = new FakeHttp();
    conn.code = 200;
    conn.in = new ByteArrayInputStream("{\"access_token\":\"AAA\"}".getBytes(StandardCharsets.UTF_8));

    Properties p = new Properties();
    p.setProperty("sso.middleware.url", "https://mw");

    try (MockedStatic<OBPropertiesProvider> prop = mockStatic(OBPropertiesProvider.class);
         MockedConstruction<URL> urlCtor = mockConstruction(URL.class, (mock, ctx) ->
             when(mock.openConnection()).thenReturn(conn));
         var lh = new LocaleHelper("ETRX_RefreshingToken", "refreshing")) {

      OBPropertiesProvider inst = mock(OBPropertiesProvider.class);
      prop.when(OBPropertiesProvider::getInstance).thenReturn(inst);
      when(inst.getOpenbravoProperties()).thenReturn(p);

      assertEquals("AAA", GoogleServiceUtil.refreshAccessToken("acc"));
    }
  }

  @Test
  void testRefreshAccessToken_ErrorStatusWrapsMessage() throws Exception {
    FakeHttp conn = new FakeHttp();
    conn.code = 401;
    conn.msg = "Unauthorized";

    Properties p = new Properties();
    p.setProperty("sso.middleware.url", "https://mw");

    try (MockedStatic<OBPropertiesProvider> prop = mockStatic(OBPropertiesProvider.class);
         MockedConstruction<URL> urlCtor = mockConstruction(URL.class, (mock, ctx) ->
             when(mock.openConnection()).thenReturn(conn));
         var lh = new LocaleHelper("ETRX_ErrorRefreshingAccessToken", "error refrescando")) {

      OBPropertiesProvider inst = mock(OBPropertiesProvider.class);
      prop.when(OBPropertiesProvider::getInstance).thenReturn(inst);
      when(inst.getOpenbravoProperties()).thenReturn(p);

      OBException ex = assertThrows(OBException.class, () -> GoogleServiceUtil.refreshAccessToken("acc"));
      assertTrue(ex.getMessage().contains("error refrescando"));
      assertTrue(ex.getMessage().contains("401"));
    }
  }

  @Test
  void testUpdateSpreadsheetValues_ReturnsSummary() throws Exception {
    Sheets svc = mock(Sheets.class, RETURNS_DEEP_STUBS);

    UpdateValuesResponse resp = new UpdateValuesResponse()
        .setUpdatedRange("S!A1:B2")
        .setUpdatedRows(2)
        .setUpdatedColumns(2)
        .setUpdatedCells(4);

    when(svc.spreadsheets().values()
        .update(eq("fid"), eq("S!A1:B2"), any(ValueRange.class))
        .setValueInputOption("RAW")
        .execute())
        .thenReturn(resp);

    try (MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS);
         MockedConstruction<Sheets.Builder> ctor = mockConstruction(
             Sheets.Builder.class,
             (mock, ctx) -> {
               when(mock.setApplicationName(any())).thenReturn(mock);
               when(mock.build()).thenReturn(svc);
             }
         )) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(TOKEN, "acc")).thenReturn(TOKEN);

      var out = GoogleServiceUtil.updateSpreadsheetValues("fid", TOKEN, "acc", "S!A1:B2",
          List.of(List.of("a", "b")));

      assertEquals("S!A1:B2", out.getString("updatedRange"));
      assertEquals(4, out.getInt("updatedCells"));
    }
  }

  private static class FakeHttp extends HttpURLConnection {
    int code = 200;
    String msg = "OK";
    InputStream in = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));

    FakeHttp() throws java.net.MalformedURLException {
      super(new URL("http://local"));
    }

    @Override
    public int getResponseCode() {
      return code;
    }

    @Override
    public String getResponseMessage() {
      return msg;
    }

    @Override
    public InputStream getInputStream() {
      return in;
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean usingProxy() {
      return false;
    }

    @Override
    public void connect() {
    }
  }


  /**
   * Helper class for mocking static localization context and translated messages.
   * Mocks {@link OBContext} and {@link Utility} to simulate message retrieval
   * based on language.
   */
  private static class LocaleHelper implements AutoCloseable {
    final MockedStatic<OBContext> obCtx;
    final MockedStatic<Utility> util;

    LocaleHelper(String msgKey, String message) {
      obCtx = mockStatic(OBContext.class);
      util = mockStatic(Utility.class);

      if (msgKey != null) {
        OBContext ctx = mock(OBContext.class);
        Language lang = mock(Language.class);
        obCtx.when(OBContext::getOBContext).thenReturn(ctx);
        when(ctx.getLanguage()).thenReturn(lang);
        when(lang.getLanguage()).thenReturn("en_US");

        util.when(() -> Utility.messageBD(any(), eq(msgKey), any()))
            .thenReturn(message);
      } else {
        // Para tests que no necesitan mensaje, devolvemos un contexto vacío
        obCtx.when(OBContext::getOBContext).thenReturn(mock(OBContext.class));
      }
    }

    @Override
    public void close() {
      obCtx.close();
      util.close();
    }
  }
}
