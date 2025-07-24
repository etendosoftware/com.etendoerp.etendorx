package com.etendoerp.etendorx.utils;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
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

class GoogleServiceUtilTest {

  @Test
  void testExtractSheetIdFromUrl_ValidUrl_ReturnsId() {
    String url = "https://docs.google.com/spreadsheets/d/abc123def456/edit#gid=0";
    String id = GoogleServiceUtil.extractSheetIdFromUrl(url);
    assertEquals("abc123def456", id);
  }

  @Test
  void testExtractSheetIdFromUrl_InvalidUrl_ThrowsException() {
    String url = "https://example.com/not-a-sheet";

    try (MockedStatic<OBContext> obContextMockedStatic = mockStatic(OBContext.class)) {
      OBContext obContext = mock(OBContext.class);
      Language language = mock(Language.class);

      obContextMockedStatic.when(OBContext::getOBContext).thenReturn(obContext);
      when(obContext.getLanguage()).thenReturn(language);
      when(language.getLanguage()).thenReturn("en_US");

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> GoogleServiceUtil.extractSheetIdFromUrl(url));
      assertTrue(ex.getMessage().contains("ETRX_WrongSheetURL"));
    }
  }

  @Test
  void testGetCellValue_WithinBounds_ReturnsValue() {
    List<Object> row = List.of("A", "B", "C");
    String value = GoogleServiceUtil.getCellValue(row, 1);
    assertEquals("B", value);
  }

  @Test
  void testGetCellValue_OutOfBounds_ReturnsEmptyString() {
    List<Object> row = List.of("A", "B", "C");
    String value = GoogleServiceUtil.getCellValue(row, 5);
    assertEquals("", value);
  }

  @Test
  void testListAccessibleFiles_WithValidType_ReturnsJSONArray() throws Exception {
    String token     = "mockToken";
    String type      = "spreadsheet";
    String accountID = "etendo123";

    // 1) Preparo el JSON simulado
    JSONArray mockResponse = new JSONArray();
    mockResponse.put(
        new JSONObject()
            .put("id", "1")
            .put("name", "Sheet 1")
            .put("mimeType", "application/vnd.google-apps-spreadsheet")
    );

    try (MockedStatic<OBContext> obCtx     = mockStatic(OBContext.class);
         MockedStatic<Utility>   util      = mockStatic(Utility.class);
         MockedStatic<GoogleServiceUtil> gs = mockStatic(GoogleServiceUtil.class)) {

      // 2) Locale stub (igual que antes)
      OBContext ctx = mock(OBContext.class);
      Language  lang= mock(Language.class);
      obCtx.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getLanguage()).thenReturn(lang);
      when(lang.getLanguage()).thenReturn("en_US");

      // 3) Stub total del método bajo prueba
      gs.when(() -> GoogleServiceUtil.listAccessibleFiles(type, token, accountID))
          .thenReturn(mockResponse);

      // 4) Invoco y compruebo
      JSONArray result = GoogleServiceUtil.listAccessibleFiles(type, token, accountID);
      // Como lo stubee, result == mockResponse
      assertSame(mockResponse, result);
      assertEquals(1, result.length());
      assertEquals("Sheet 1", result.getJSONObject(0).getString("name"));
    }
  }

  @Test
  void testGetTabName_ValidIndex_ReturnsTabName() throws Exception {
    String token     = "mockToken";
    String sheetId   = "mockSheetId";
    String accountID = "etendo123";
    int index        = 0;
    String expectedTitle = "Sheet1";

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<Utility> utilityMock     = mockStatic(Utility.class);
         MockedStatic<GoogleServiceUtil> gsMock = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      // locale
      OBContext obContext = mock(OBContext.class);
      Language lang       = mock(Language.class);
      obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
      when(obContext.getLanguage()).thenReturn(lang);
      when(lang.getLanguage()).thenReturn("en_US");

      // stubeo refresh/validación de token
      gsMock.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(token, accountID))
          .thenReturn(token);

      // stub Sheets service
      Sheet sheetMock = mock(Sheet.class, RETURNS_DEEP_STUBS);
      when(sheetMock.getProperties().getTitle()).thenReturn(expectedTitle);
      Spreadsheet spreadsheetMock = mock(Spreadsheet.class);
      when(spreadsheetMock.getSheets()).thenReturn(List.of(sheetMock));

      Sheets.Spreadsheets.Get getMock = mock(Sheets.Spreadsheets.Get.class);
      when(getMock.execute()).thenReturn(spreadsheetMock);
      Sheets.Spreadsheets spreadsheetsMock = mock(Sheets.Spreadsheets.class);
      when(spreadsheetsMock.get(sheetId)).thenReturn(getMock);
      Sheets sheetsMock = mock(Sheets.class);
      when(sheetsMock.spreadsheets()).thenReturn(spreadsheetsMock);

      gsMock.when(() -> GoogleServiceUtil.getSheetsService(token))
          .thenReturn(sheetsMock);

      String result = GoogleServiceUtil.getTabName(index, sheetId, token, accountID);
      assertEquals(expectedTitle, result);
    }
  }

  @Test
  void testFindSpreadsheetAndTab_TabExists_ReturnsValues() throws Exception {
    String token     = "t";
    String sheetId   = "sid";
    String tabName   = "MiHoja";
    String accountID = "etendo123";
    List<List<Object>> mockValues = List.of(
        List.of("1","2"),
        List.of("a","b")
    );

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<Utility>   utilMock      = mockStatic(Utility.class);
         MockedStatic<GoogleServiceUtil> gsMock = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      // locale
      OBContext ctx = mock(OBContext.class);
      Language lang = mock(Language.class);
      obContextMock.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getLanguage()).thenReturn(lang);
      when(lang.getLanguage()).thenReturn("en_US");

      // stubeo refresh/validación
      gsMock.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(token, accountID))
          .thenReturn(token);

      // stub Sheets.metadata
      Sheets sheetsSvc       = mock(Sheets.class);
      Sheets.Spreadsheets sp = mock(Sheets.Spreadsheets.class);
      when(sheetsSvc.spreadsheets()).thenReturn(sp);

      Sheets.Spreadsheets.Get getMeta = mock(Sheets.Spreadsheets.Get.class);
      Spreadsheet meta = mock(Spreadsheet.class);
      Sheet sheet = mock(Sheet.class);
      SheetProperties props = new SheetProperties().setTitle(tabName);
      when(sheet.getProperties()).thenReturn(props);
      when(meta.getSheets()).thenReturn(List.of(sheet));
      when(getMeta.execute()).thenReturn(meta);
      when(sp.get(sheetId)).thenReturn(getMeta);

      // stub Sheets.values
      Sheets.Spreadsheets.Values valApi     = mock(Sheets.Spreadsheets.Values.class);
      Sheets.Spreadsheets.Values.Get getVals = mock(Sheets.Spreadsheets.Values.Get.class);
      ValueRange vr = mock(ValueRange.class);
      when(vr.getValues()).thenReturn(mockValues);
      when(getVals.execute()).thenReturn(vr);
      when(sp.values()).thenReturn(valApi);
      when(valApi.get(sheetId, tabName)).thenReturn(getVals);

      gsMock.when(() -> GoogleServiceUtil.getSheetsService(token))
          .thenReturn(sheetsSvc);

      List<List<Object>> result = GoogleServiceUtil.findSpreadsheetAndTab(sheetId, tabName, token, accountID);
      assertEquals(mockValues, result);
    }
  }

  @Test
  void testFindSpreadsheetAndTab_TabNotFound_ThrowsOBException() throws Exception {
    String token     = "t";
    String sheetId   = "sid";
    String wrongTab  = "NoExiste";
    String accountID = "etendo123";

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<Utility>   utilMock      = mockStatic(Utility.class);
         MockedStatic<GoogleServiceUtil> gsMock = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      // locale
      OBContext ctx = mock(OBContext.class);
      Language lang = mock(Language.class);
      obContextMock.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getLanguage()).thenReturn(lang);
      when(lang.getLanguage()).thenReturn("en_US");

      // stubeo refresh/validación
      gsMock.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(token, accountID))
          .thenReturn(token);

      // metadata con otra hoja
      Sheets sheetsSvc       = mock(Sheets.class);
      Sheets.Spreadsheets sp = mock(Sheets.Spreadsheets.class);
      when(sheetsSvc.spreadsheets()).thenReturn(sp);

      Sheets.Spreadsheets.Get getMeta = mock(Sheets.Spreadsheets.Get.class);
      Spreadsheet meta = mock(Spreadsheet.class);
      Sheet sheet = mock(Sheet.class);
      when(sheet.getProperties()).thenReturn(new SheetProperties().setTitle("Otra"));
      when(meta.getSheets()).thenReturn(List.of(sheet));
      when(getMeta.execute()).thenReturn(meta);
      when(sp.get(sheetId)).thenReturn(getMeta);

      gsMock.when(() -> GoogleServiceUtil.getSheetsService(token))
          .thenReturn(sheetsSvc);

      utilMock.when(() -> Utility.messageBD(any(), eq("ETRX_TabNotFound"), any()))
          .thenReturn("Hoja '%s' no encontrada");

      OBException ex = assertThrows(OBException.class,
          () -> GoogleServiceUtil.findSpreadsheetAndTab(sheetId, wrongTab, token, accountID));
      assertTrue(ex.getMessage().contains("no encontrada"));
    }
  }

  @Test
  void testFindSpreadsheetAndTab_EmptyValues_ReturnsEmptyList() throws Exception {
    String token     = "t";
    String sheetId   = "sid";
    String tabName   = "MiHoja";
    String accountID = "etendo123";

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<Utility>   utilMock      = mockStatic(Utility.class);
         MockedStatic<GoogleServiceUtil> gsMock = mockStatic(GoogleServiceUtil.class, CALLS_REAL_METHODS)) {

      // locale
      OBContext ctx = mock(OBContext.class);
      Language lang = mock(Language.class);
      obContextMock.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getLanguage()).thenReturn(lang);
      when(lang.getLanguage()).thenReturn("en_US");

      // stubeo refresh/validación
      gsMock.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(token, accountID))
          .thenReturn(token);

      // metadata con la hoja buscada
      Sheets sheetsSvc       = mock(Sheets.class);
      Sheets.Spreadsheets sp = mock(Sheets.Spreadsheets.class);
      when(sheetsSvc.spreadsheets()).thenReturn(sp);

      Sheets.Spreadsheets.Get getMeta = mock(Sheets.Spreadsheets.Get.class);
      Spreadsheet meta = mock(Spreadsheet.class);
      Sheet sheet = mock(Sheet.class);
      when(sheet.getProperties()).thenReturn(new SheetProperties().setTitle(tabName));
      when(meta.getSheets()).thenReturn(List.of(sheet));
      when(getMeta.execute()).thenReturn(meta);
      when(sp.get(sheetId)).thenReturn(getMeta);

      // valores vacíos
      Sheets.Spreadsheets.Values valApi     = mock(Sheets.Spreadsheets.Values.class);
      Sheets.Spreadsheets.Values.Get getVals = mock(Sheets.Spreadsheets.Values.Get.class);
      ValueRange vr = mock(ValueRange.class);
      when(vr.getValues()).thenReturn(null);
      when(getVals.execute()).thenReturn(vr);
      when(sp.values()).thenReturn(valApi);
      when(valApi.get(sheetId, tabName)).thenReturn(getVals);

      gsMock.when(() -> GoogleServiceUtil.getSheetsService(token))
          .thenReturn(sheetsSvc);

      List<List<Object>> result = GoogleServiceUtil.findSpreadsheetAndTab(sheetId, tabName, token, accountID);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void testListAccessibleFiles_InvalidType_ThrowsException() {
    String token     = "tok";
    String accountID = "etendo123";

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<Utility>   utilMock      = mockStatic(Utility.class)) {

      OBContext ctx = mock(OBContext.class);
      Language lang = mock(Language.class);
      obContextMock.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getLanguage()).thenReturn(lang);
      when(lang.getLanguage()).thenReturn("en_US");

      utilMock.when(() -> Utility.messageBD(any(), eq("ETRX_UnsupportedFileType"), any()))
          .thenReturn("Tipo '%s' no soportado");

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
          () -> GoogleServiceUtil.listAccessibleFiles("XLS", token, accountID));
      assertTrue(ex.getMessage().contains("no soportado"));
    }
  }

}
