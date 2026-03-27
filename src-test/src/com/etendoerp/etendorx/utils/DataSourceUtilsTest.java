package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.model.ad.domain.Reference;

/**
 * Unit tests for DataSourceUtils utility methods.
 */
public class DataSourceUtilsTest extends WeldBaseTest {

  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
  }

  // ========== extractDataSourceAndID ==========

  @Test
  public void testExtractDataSourceAndID_WithID() {
    String[] result = DataSourceUtils.extractDataSourceAndID("/SalesOrder/ABC123");
    assertEquals(2, result.length);
    assertEquals("SalesOrder", result[0]);
    assertEquals("ABC123", result[1]);
  }

  @Test
  public void testExtractDataSourceAndID_WithoutID() {
    String[] result = DataSourceUtils.extractDataSourceAndID("/SalesOrder");
    assertEquals(1, result.length);
    assertEquals("SalesOrder", result[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExtractDataSourceAndID_TooManyParts() {
    DataSourceUtils.extractDataSourceAndID("/a/b/c/d");
  }

  // ========== keyConvertion ==========

  @Test
  public void testKeyConvertion_AllKeysMapped() throws JSONException {
    JSONObject data = new JSONObject();
    data.put("name", "Test");
    data.put("value", 100);

    Map<String, String> map = new HashMap<>();
    map.put("name", "nombre");
    map.put("value", "valor");

    JSONObject result = DataSourceUtils.keyConvertion(data, map);
    assertEquals("Test", result.getString("nombre"));
    assertEquals(100, result.getInt("valor"));
    assertFalse(result.has("name"));
  }

  @Test
  public void testKeyConvertion_SomeKeysUnmapped() throws JSONException {
    JSONObject data = new JSONObject();
    data.put("mapped", "yes");
    data.put("unmapped", "no");

    Map<String, String> map = new HashMap<>();
    map.put("mapped", "converted");

    JSONObject result = DataSourceUtils.keyConvertion(data, map);
    assertEquals("yes", result.getString("converted"));
    assertEquals("no", result.getString("unmapped"));
  }

  @Test
  public void testKeyConvertion_EmptyData() throws JSONException {
    JSONObject result = DataSourceUtils.keyConvertion(new JSONObject(), new HashMap<>());
    assertEquals(0, result.length());
  }

  // ========== applyChanges ==========

  @Test
  public void testApplyChanges_NewKeys() throws JSONException {
    JSONObject existing = new JSONObject();
    existing.put("a", 1);

    JSONObject changes = new JSONObject();
    changes.put("b", 2);

    JSONObject result = DataSourceUtils.applyChanges(existing, changes);
    assertEquals(1, result.getInt("a"));
    assertEquals(2, result.getInt("b"));
  }

  @Test
  public void testApplyChanges_OverwriteExisting() throws JSONException {
    JSONObject existing = new JSONObject();
    existing.put("key", "old");

    JSONObject changes = new JSONObject();
    changes.put("key", "new");

    JSONObject result = DataSourceUtils.applyChanges(existing, changes);
    assertEquals("new", result.getString("key"));
  }

  @Test
  public void testApplyChanges_EmptyChanges() throws JSONException {
    JSONObject existing = new JSONObject();
    existing.put("keep", "this");

    JSONObject result = DataSourceUtils.applyChanges(existing, new JSONObject());
    assertEquals("this", result.getString("keep"));
  }

  // ========== isReferenceToAnotherTable ==========

  @Test
  public void testIsReferenceToAnotherTable_Search() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("30");
    assertTrue(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  public void testIsReferenceToAnotherTable_TableDir() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("19");
    assertTrue(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  public void testIsReferenceToAnotherTable_Table() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("18");
    assertTrue(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  public void testIsReferenceToAnotherTable_ID() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("13");
    assertTrue(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  public void testIsReferenceToAnotherTable_Other() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("10");
    assertFalse(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  // ========== valueConvertToInputFormat ==========

  @Test
  public void testValueConvertToInputFormat_NullType() throws Exception {
    assertEquals("hello", DataSourceUtils.valueConvertToInputFormat("hello", null));
  }

  @Test
  public void testValueConvertToInputFormat_BigDecimal() throws Exception {
    assertEquals("123.45", DataSourceUtils.valueConvertToInputFormat(new java.math.BigDecimal("123.45"), "BigDecimal"));
  }

  @Test
  public void testValueConvertToInputFormat_LongFromInteger() throws Exception {
    assertEquals("42", DataSourceUtils.valueConvertToInputFormat(42, "Long"));
  }

  @Test
  public void testValueConvertToInputFormat_LongFromLong() throws Exception {
    assertEquals("999999", DataSourceUtils.valueConvertToInputFormat(999999L, "Long"));
  }

  @Test
  public void testValueConvertToInputFormat_BooleanTrue() throws Exception {
    assertEquals("Y", DataSourceUtils.valueConvertToInputFormat(true, "Boolean"));
  }

  @Test
  public void testValueConvertToInputFormat_BooleanFalse() throws Exception {
    assertEquals("N", DataSourceUtils.valueConvertToInputFormat(false, "Boolean"));
  }

  @Test
  public void testValueConvertToInputFormat_DefaultType() throws Exception {
    assertEquals("anyValue", DataSourceUtils.valueConvertToInputFormat("anyValue", "UnknownType"));
  }

  // ========== getInpName ==========

  @Test
  public void testGetInpName_SimpleColumn() {
    String result = DataSourceUtils.getInpName("AD_Client_ID");
    assertNotNull(result);
    assertTrue(result.startsWith("inp"));
  }

  @Test
  public void testGetInpName_LowerCaseColumn() {
    String result = DataSourceUtils.getInpName("name");
    assertNotNull(result);
    assertTrue(result.startsWith("inp"));
  }

  // ========== getValueFromItem ==========

  @Test
  public void testGetValueFromItem_DirectFromClassic() throws JSONException {
    JSONObject item = new JSONObject();
    item.put("classicValue", "myValue");
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", true);
    assertEquals("myValue", result);
  }

  @Test
  public void testGetValueFromItem_DirectFromClassic_Empty() throws JSONException {
    JSONObject item = new JSONObject();
    item.put("classicValue", "");
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", true);
    assertNull(result);
  }

  @Test
  public void testGetValueFromItem_WithLongValue() throws JSONException {
    JSONObject item = new JSONObject();
    item.put("value", 42L);
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", false);
    assertEquals(42L, result);
  }

  @Test
  public void testGetValueFromItem_WithIntegerValue() throws JSONException {
    JSONObject item = new JSONObject();
    item.put("value", 10);
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", false);
    assertEquals(10, result);
  }

  @Test
  public void testGetValueFromItem_WithDoubleValue() throws JSONException {
    JSONObject item = new JSONObject();
    item.put("value", 3.14);
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", false);
    assertEquals(3.14, result);
  }

  @Test
  public void testGetValueFromItem_WithDateString() throws JSONException {
    JSONObject item = new JSONObject();
    item.put("value", "2026-01-15");
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", false);
    assertEquals("15-01-2026", result);
  }

  @Test
  public void testGetValueFromItem_WithNonDateString() throws JSONException {
    JSONObject item = new JSONObject();
    item.put("value", "notADate");
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", false);
    assertEquals("notADate", result);
  }

  @Test
  public void testGetValueFromItem_NoValueFallsBackToClassic() throws JSONException {
    JSONObject item = new JSONObject();
    item.put("classicValue", "fallback");
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", false);
    assertEquals("fallback", result);
  }

  @Test
  public void testGetValueFromItem_NoValueNoClassic() throws JSONException {
    JSONObject item = new JSONObject();
    Object result = DataSourceUtils.getValueFromItem(item, "yyyy-MM-dd", "dd-MM-yyyy", false);
    assertNull(result);
  }
}
