package com.etendoerp.etendorx.utils;

/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance
 * with the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;

class DataSourceUtilsTest {

  public static final String VALUE_1 = "value1";
  public static final String VALUE_2 = "value2";
  public static final String VALUE_3 = "value3";
  public static final String CLASSIC_VALUE = "classicValue";
  public static final String DD_MM_YYYY = "dd-MM-yyyy";
  public static final String MY_VALUE = "myValue";
  public static final String VALUE = "value";
  public static final String EXISTING = "existing";
  public static final String OLD_KEY_1 = "oldKey1";
  public static final String OLD_KEY_2 = "oldKey2";
  public static final String YYYY_MM_DD = "yyyy-MM-dd";

  @Test
  void testKeyConvertionWithMappedKeys() throws JSONException {
    JSONObject data = new JSONObject();
    data.put(OLD_KEY_1, VALUE_1);
    data.put(OLD_KEY_2, VALUE_2);
    data.put("unmappedKey", VALUE_3);

    Map<String, String> map = new HashMap<>();
    map.put(OLD_KEY_1, "newKey1");
    map.put(OLD_KEY_2, "newKey2");

    JSONObject result = DataSourceUtils.keyConvertion(data, map);

    assertEquals(VALUE_1, result.get("newKey1"));
    assertEquals(VALUE_2, result.get("newKey2"));
    assertEquals(VALUE_3, result.get("unmappedKey"));
    assertFalse(result.has(OLD_KEY_1));
    assertFalse(result.has(OLD_KEY_2));
  }

  @Test
  void testKeyConvertionWithEmptyMap() throws JSONException {
    JSONObject data = new JSONObject();
    data.put("key1", VALUE_1);

    JSONObject result = DataSourceUtils.keyConvertion(data, new HashMap<>());

    assertEquals(VALUE_1, result.get("key1"));
  }

  @Test
  void testKeyConvertionWithEmptyData() throws JSONException {
    JSONObject result = DataSourceUtils.keyConvertion(new JSONObject(), new HashMap<>());
    assertFalse(result.keys().hasNext());
  }

  @Test
  void testApplyChanges() throws JSONException {
    JSONObject preexistent = new JSONObject();
    preexistent.put(EXISTING, "oldValue");
    preexistent.put("untouched", "stays");

    JSONObject changes = new JSONObject();
    changes.put(EXISTING, "newValue");
    changes.put("newField", "added");

    JSONObject result = DataSourceUtils.applyChanges(preexistent, changes);

    assertEquals("newValue", result.get(EXISTING));
    assertEquals("stays", result.get("untouched"));
    assertEquals("added", result.get("newField"));
  }

  @Test
  void testApplyChangesEmptyChanges() throws JSONException {
    JSONObject preexistent = new JSONObject();
    preexistent.put("key", VALUE);

    JSONObject result = DataSourceUtils.applyChanges(preexistent, new JSONObject());

    assertEquals(VALUE, result.get("key"));
  }

  @Test
  void testExtractDataSourceAndIDWithBothParts() {
    String[] result = DataSourceUtils.extractDataSourceAndID("/MyDataSource/12345");
    assertEquals(2, result.length);
    assertEquals("MyDataSource", result[0]);
    assertEquals("12345", result[1]);
  }

  @Test
  void testExtractDataSourceAndIDWithoutID() {
    String[] result = DataSourceUtils.extractDataSourceAndID("/MyDataSource");
    assertEquals(1, result.length);
    assertEquals("MyDataSource", result[0]);
  }

  @Test
  void testExtractDataSourceAndIDInvalidURI() {
    assertThrows(IllegalArgumentException.class,
        () -> DataSourceUtils.extractDataSourceAndID("/a/b/c/d"));
  }

  @Test
  void testGetValueFromItemDirectFromClassic() throws JSONException {
    JSONObject item = new JSONObject();
    item.put(CLASSIC_VALUE, MY_VALUE);

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, true);
    assertEquals(MY_VALUE, result);
  }

  @Test
  void testGetValueFromItemDirectFromClassicEmpty() throws JSONException {
    JSONObject item = new JSONObject();
    item.put(CLASSIC_VALUE, "");

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, true);
    assertNull(result);
  }

  @Test
  void testGetValueFromItemDirectFromClassicMissing() throws JSONException {
    JSONObject item = new JSONObject();

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, true);
    assertNull(result);
  }

  @Test
  void testGetValueFromItemWithNumericValue() throws JSONException {
    JSONObject item = new JSONObject();
    item.put(VALUE, 42L);

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, false);
    assertEquals(42L, result);
  }

  @Test
  void testGetValueFromItemWithIntegerValue() throws JSONException {
    JSONObject item = new JSONObject();
    item.put(VALUE, 100);

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, false);
    assertEquals(100, result);
  }

  @Test
  void testGetValueFromItemWithDoubleValue() throws JSONException {
    JSONObject item = new JSONObject();
    item.put(VALUE, 3.14);

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, false);
    assertEquals(3.14, result);
  }

  @Test
  void testGetValueFromItemWithDateConversion() throws JSONException {
    JSONObject item = new JSONObject();
    item.put(VALUE, "2024-01-15");

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, false);
    assertEquals("15-01-2024", result);
  }

  @Test
  void testGetValueFromItemWithInvalidDate() throws JSONException {
    JSONObject item = new JSONObject();
    item.put(VALUE, "not-a-date");

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, false);
    assertEquals("not-a-date", result);
  }

  @Test
  void testGetValueFromItemFallbackToClassicValue() throws JSONException {
    JSONObject item = new JSONObject();
    item.put(CLASSIC_VALUE, "fallback");

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, false);
    assertEquals("fallback", result);
  }

  @Test
  void testGetValueFromItemNoValueNoClassic() throws JSONException {
    JSONObject item = new JSONObject();

    Object result = DataSourceUtils.getValueFromItem(item, YYYY_MM_DD, DD_MM_YYYY, false);
    assertNull(result);
  }

  @Test
  void testIsReferenceToAnotherTableSearch() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("30");
    assertTrue(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  void testIsReferenceToAnotherTableTableDir() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("19");
    assertTrue(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  void testIsReferenceToAnotherTableTable() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("18");
    assertTrue(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  void testIsReferenceToAnotherTableID() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("13");
    assertTrue(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  void testIsReferenceToAnotherTableFalse() {
    Reference ref = mock(Reference.class);
    when(ref.getId()).thenReturn("10");
    assertFalse(DataSourceUtils.isReferenceToAnotherTable(ref));
  }

  @Test
  void testValueConvertToInputFormatBigDecimal() throws ParseException {
    Object result = DataSourceUtils.valueConvertToInputFormat(new BigDecimal("123.45"), "BigDecimal");
    assertEquals("123.45", result);
  }

  @Test
  void testValueConvertToInputFormatLong() throws ParseException {
    Object result = DataSourceUtils.valueConvertToInputFormat(100L, "Long");
    assertEquals("100", result);
  }

  @Test
  void testValueConvertToInputFormatLongFromInteger() throws ParseException {
    Object result = DataSourceUtils.valueConvertToInputFormat(42, "Long");
    assertEquals("42", result);
  }

  @Test
  void testValueConvertToInputFormatBooleanTrue() throws ParseException {
    Object result = DataSourceUtils.valueConvertToInputFormat(true, "Boolean");
    assertEquals("Y", result);
  }

  @Test
  void testValueConvertToInputFormatBooleanFalse() throws ParseException {
    Object result = DataSourceUtils.valueConvertToInputFormat(false, "Boolean");
    assertEquals("N", result);
  }

  @Test
  void testValueConvertToInputFormatNullType() throws ParseException {
    Object result = DataSourceUtils.valueConvertToInputFormat("hello", null);
    assertEquals("hello", result);
  }

  @Test
  void testValueConvertToInputFormatDefaultType() throws ParseException {
    Object result = DataSourceUtils.valueConvertToInputFormat("someValue", "UnknownType");
    assertEquals("someValue", result);
  }

  @Test
  void testGetInpNameSimpleColumn() {
    String result = DataSourceUtils.getInpName("AD_Client_ID");
    assertNotNull(result);
    assertTrue(result.startsWith("inp"));
  }

  @Test
  void testGetInpNameLowerCaseColumn() {
    String result = DataSourceUtils.getInpName("name");
    assertNotNull(result);
    assertTrue(result.startsWith("inp"));
  }

  @Test
  void testGetHQLColumnNameNullColumn() {
    String[] result = DataSourceUtils.getHQLColumnName((Column) null);
    assertArrayEquals(new String[]{ "null", "String" }, result);
  }

  @Test
  void testGetHQLColumnNameNullTable() {
    Column column = mock(Column.class);
    when(column.getTable()).thenReturn(null);
    String[] result = DataSourceUtils.getHQLColumnName(column);
    assertArrayEquals(new String[]{ "null", "String" }, result);
  }

  @Test
  void testConstants() {
    assertEquals(CLASSIC_VALUE, DataSourceUtils.CLASSIC_VALUE);
    assertEquals("30", DataSourceUtils.REFERENCE_SEARCH);
    assertEquals("19", DataSourceUtils.REFERENCE_TABLEDIR);
    assertEquals("18", DataSourceUtils.REFERENCE_TABLE);
    assertEquals("13", DataSourceUtils.REFERENCE_ID);
  }
}
