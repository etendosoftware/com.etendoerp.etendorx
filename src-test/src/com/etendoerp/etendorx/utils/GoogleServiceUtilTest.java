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

@ExtendWith(MockitoExtension.class)
class GoogleServiceUtilTest {

  private static final String TOKEN      = "mockToken";
  private static final String ACCOUNT_ID = "etendo123";

  private static class LocaleHelper implements AutoCloseable {
    final MockedStatic<OBContext> obCtx;
    final MockedStatic<Utility>   util;

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

    @Override
    public void close() {
      obCtx.close();
      util.close();
    }
  }

  @Test
  void testExtractSheetIdFromUrl_ValidUrl_ReturnsId() {
    String id = GoogleServiceUtil.extractSheetIdFromUrl(
        "https://docs.google.com/spreadsheets/d/abc123def456/edit#gid=0"
    );
    assertEquals("abc123def456", id);
  }

  @Test
  void testExtractSheetIdFromUrl_InvalidUrl_ThrowsException() {
    try (var lh = new LocaleHelper("ETRX_WrongSheetURL", "bad‐url")) {
      IllegalArgumentException ex = assertThrows(
          IllegalArgumentException.class,
          () -> GoogleServiceUtil.extractSheetIdFromUrl("https://foo/not-sheet")
      );
      assertTrue(ex.getMessage().contains("bad‐url"));
    }
  }

  @Test
  void testGetCellValue_WithinBounds_ReturnsValue() {
    assertEquals("B",
        GoogleServiceUtil.getCellValue(List.of("A","B","C"), 1)
    );
  }

  @Test
  void testGetCellValue_OutOfBounds_ReturnsEmptyString() {
    assertEquals("",
        GoogleServiceUtil.getCellValue(List.of("A","B","C"), 99)
    );
  }

  @Test
  void testListAccessibleFiles_WithValidType_ReturnsJSONArray() throws Exception {
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

  @Test
  void testGetTabName_ValidIndex_ReturnsTabName() throws Exception {
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

  @Test
  void testFindSpreadsheetAndTab_TabExists_ReturnsValues() throws Exception {
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

  @Test
  void testFindSpreadsheetAndTab_TabNotFound_ThrowsOBException() throws Exception {
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

  @Test
  void testFindSpreadsheetAndTab_EmptyValues_ReturnsEmptyList() throws Exception {
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
              new SheetProperties().setTitle("MiHoja")
          )
      ));
      when(sheetsSvc.spreadsheets()
          .values()
          .get("sid","MiHoja")
          .execute()
          .getValues()
      ).thenReturn(null);

      gs.when(() -> GoogleServiceUtil.getSheetsService(TOKEN))
          .thenReturn(sheetsSvc);

      assertTrue(GoogleServiceUtil.findSpreadsheetAndTab(
          "sid","MiHoja",TOKEN,ACCOUNT_ID
      ).isEmpty());
    }
  }

  @Test
  void testListAccessibleFiles_InvalidType_ThrowsException() {
    try (var lh = new LocaleHelper("ETRX_UnsupportedFileType","bad‐type")) {
      IllegalArgumentException ex = assertThrows(
          IllegalArgumentException.class,
          () -> GoogleServiceUtil.listAccessibleFiles("XLS", TOKEN, ACCOUNT_ID)
      );
      assertTrue(ex.getMessage().contains("bad‐type"));
    }
  }
}
