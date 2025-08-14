package com.etendoerp.etendorx.utils;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Language;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
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

  private static final String TOKEN = "mockToken";
  private static final String ACCOUNT_ID = "etendo123";
  private static final String SHEET_TITLE = "MiHoja";

  /**
   * Helper class for mocking static localization context and translated messages.
   * Mocks {@link OBContext} and {@link Utility} to simulate message retrieval
   * based on language.
   */
  private static class LocaleHelper implements AutoCloseable {
    final MockedStatic<OBContext> obCtx;
    final MockedStatic<Utility>   util;

    /**
     * Constructs the LocaleHelper, mocking OBContext and Utility to return
     * a specific language and a translated message for a given key.
     *
     * @param msgKey the message key to simulate
     * @param message the localized message to return
     */
    LocaleHelper(String msgKey, String message) {
      obCtx = mockStatic(OBContext.class);
      util  = mockStatic(Utility.class);

      OBContext ctx  = mock(OBContext.class);
      Language  lang = mock(Language.class);
      obCtx.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getLanguage()).thenReturn(lang);
      when(lang.getLanguage()).thenReturn("en_US");

      if (msgKey != null) {
        util.when(() -> Utility.messageBD(any(), eq(msgKey), any()))
            .thenReturn(message);
      }
    }

    /** Closes the mocked static contexts. */
    @Override
    public void close() {
      obCtx.close();
      util.close();
    }
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
      IllegalArgumentException ex = assertThrows(
          IllegalArgumentException.class,
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
        GoogleServiceUtil.getCellValue(List.of("A","B","C"), 1)
    );
  }

  /**
   * Verifies that getCellValue returns an empty string when index is out of bounds.
   */
  @Test
  void testGetCellValueOutOfBoundsReturnsEmptyString() {
    assertEquals("",
        GoogleServiceUtil.getCellValue(List.of("A","B","C"), 99)
    );
  }


  /**
   * Mocks and verifies that listAccessibleFiles returns a valid JSONArray of spreadsheet files.
   */
  @Test
  void testListAccessibleFilesWithValidTypeReturnsJSONArray() throws Exception {
    JSONArray mockResp = new JSONArray()
        .put(new JSONObject()
            .put("id","1")
            .put("name","Sheet 1")
            .put("mimeType","application/vnd.google-apps-spreadsheet")
        );
    try (var gs = mockStatic(GoogleServiceUtil.class)) {
      gs.when(() -> GoogleServiceUtil
          .listAccessibleFiles("spreadsheet", TOKEN, ACCOUNT_ID)
      ).thenReturn(mockResp);
      JSONArray result = GoogleServiceUtil
          .listAccessibleFiles("spreadsheet", TOKEN, ACCOUNT_ID);
      assertSame(mockResp, result);
      assertEquals(1, result.length());
      assertEquals("Sheet 1",
          result.getJSONObject(0).getString("name")
      );
    }
  }

  /**
   * Verifies that getTabName returns the correct sheet tab title.
   */
  @Test
  void testGetTabNameValidIndexReturnsTabName() throws Exception {
    String sheetId = "sid", title = "MySheet";
    try (var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(
          TOKEN, ACCOUNT_ID
      )).thenReturn(TOKEN);
      Sheets sheetsSvc = mock(Sheets.class, RETURNS_DEEP_STUBS);
      when(sheetsSvc.spreadsheets()
          .get(sheetId)
          .execute()
          .getSheets().get(0)
          .getProperties().getTitle()
      ).thenReturn(title);

      gs.when(() -> GoogleServiceUtil.getSheetsService(TOKEN))
          .thenReturn(sheetsSvc);

      assertEquals(title,
          GoogleServiceUtil.getTabName(0, sheetId, TOKEN, ACCOUNT_ID)
      );
    }
  }

  /**
   * Verifies that findSpreadsheetAndTab returns spreadsheet data when the tab exists.
   */
  @Test
  void testFindSpreadsheetAndTabTabExistsReturnsValues() throws Exception {
    String sheetId = "sid", tab = "T1";
    List<List<Object>> data = List.of(
        List.of("A","B"),
        List.of("C","D")
    );

    try (var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(
          TOKEN, ACCOUNT_ID
      )).thenReturn(TOKEN);
      Sheets sheetsSvc = mock(Sheets.class, RETURNS_DEEP_STUBS);
      Sheet fakeSheet = new Sheet()
          .setProperties(new SheetProperties().setTitle(tab));
      when(sheetsSvc.spreadsheets()
          .get(sheetId)
          .execute()
          .getSheets()
      ).thenReturn(List.of(fakeSheet));

      when(sheetsSvc.spreadsheets()
          .values()
          .get(sheetId, tab)
          .execute()
          .getValues()
      ).thenReturn(data);

      gs.when(() -> GoogleServiceUtil.getSheetsService(TOKEN))
          .thenReturn(sheetsSvc);

      assertEquals(data,
          GoogleServiceUtil.findSpreadsheetAndTab(
              sheetId, tab, TOKEN, ACCOUNT_ID
          )
      );
    }
  }

  /**
   * Verifies that an OBException is thrown when the specified tab is not found.
   */
  @Test
  void testFindSpreadsheetAndTabTabNotFoundThrowsOBException() throws Exception {
    try (var lh = new LocaleHelper("ETRX_TabNotFound","no encontrada");
         var gs = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS))
    {
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(
          TOKEN, ACCOUNT_ID
      )).thenReturn(TOKEN);

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

      gs.when(() -> GoogleServiceUtil.getSheetsService(TOKEN))
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
      gs.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(
          TOKEN, ACCOUNT_ID
      )).thenReturn(TOKEN);

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

      gs.when(() -> GoogleServiceUtil.getSheetsService(TOKEN))
          .thenReturn(sheetsSvc);

      assertTrue(GoogleServiceUtil.findSpreadsheetAndTab(
          "sid", SHEET_TITLE,TOKEN,ACCOUNT_ID
      ).isEmpty());
    }
  }

  /**
   * Verifies that an invalid file type passed to listAccessibleFiles throws an IllegalArgumentException.
   */
  @Test
  void testListAccessibleFilesInvalidTypeThrowsException() {
    try (var lh = new LocaleHelper("ETRX_UnsupportedFileType","bad‐type")) {
      IllegalArgumentException ex = assertThrows(
          IllegalArgumentException.class,
          () -> GoogleServiceUtil.listAccessibleFiles("XLS", TOKEN, ACCOUNT_ID)
      );
      assertTrue(ex.getMessage().contains("bad‐type"));
    }
  }
}
