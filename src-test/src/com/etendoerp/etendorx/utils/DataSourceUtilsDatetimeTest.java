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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Conversion of {@code Datetime} values between the classic input format and the representation the
 * core datasource expects, which is where a mandatory {@code DateTime} column defaulted from a
 * session variable used to lose its time component.
 */
class DataSourceUtilsDatetimeTest {

  private static final String DATETIME_TYPE = "Datetime";
  private static final String DATE_TYPE = "Date";
  private static final String A_DATETIME_FIELD = "aDatetimeField";
  private static final String A_DATE_FIELD = "aDateField";
  private static final String INPUT_DATE = "09-09-2026";
  private static final String INPUT_DATETIME = "09-09-2026 19:00:51";
  private static final String HQL_DATETIME = "2026-09-09T19:00:51";
  private static final String INPUT_DATE_FORMAT = "dd-MM-yyyy";

  /**
   * A {@code Datetime} value coming from the classic input format must reach the core datasource
   * with its time component intact and in the format the JSON converter is able to parse.
   */
  @Test
  void testValuesConvertionDatetimeKeepsTimeComponent() throws ParseException, JSONException {
    JSONObject body = new JSONObject();
    body.put(A_DATETIME_FIELD, INPUT_DATETIME);

    JSONObject result = withDateFormats(
        () -> DataSourceUtils.valuesConvertion(body, Map.of(A_DATETIME_FIELD, DATETIME_TYPE)));

    assertEquals(HQL_DATETIME, result.getString(A_DATETIME_FIELD));
  }

  /**
   * A mandatory {@code Datetime} column defaulted to a date-only session variable must be padded
   * with a time component instead of failing to parse.
   */
  @Test
  void testValuesConvertionDatetimeAcceptsDateOnlyValue() throws ParseException, JSONException {
    JSONObject body = new JSONObject();
    body.put(A_DATETIME_FIELD, INPUT_DATE);

    JSONObject result = withDateFormats(
        () -> DataSourceUtils.valuesConvertion(body, Map.of(A_DATETIME_FIELD, DATETIME_TYPE)));

    assertEquals("2026-09-09T00:00:00", result.getString(A_DATETIME_FIELD));
  }

  /**
   * A plain {@code Date} column keeps its date-only representation, which the core already parses.
   */
  @Test
  void testValuesConvertionDateStaysDateOnly() throws ParseException, JSONException {
    JSONObject body = new JSONObject();
    body.put(A_DATE_FIELD, INPUT_DATE);

    JSONObject result = withDateFormats(
        () -> DataSourceUtils.valuesConvertion(body, Map.of(A_DATE_FIELD, DATE_TYPE)));

    assertEquals("2026-09-09", result.getString(A_DATE_FIELD));
  }

  @Test
  void testValueConvertToInputFormatDatetime() throws ParseException, JSONException {
    String result = withDateFormats(
        () -> DataSourceUtils.valueConvertToInputFormat(HQL_DATETIME, DATETIME_TYPE));

    assertEquals(INPUT_DATETIME, result);
  }

  /**
   * The representation the core emits when reading a datetime back ({@code .S'Z'} suffixed) must
   * still be accepted, so a value obtained through a GET can be sent back unchanged.
   */
  @Test
  void testValueConvertToInputFormatDatetimeWithMillisAndZone()
      throws ParseException, JSONException {
    String result = withDateFormats(
        () -> DataSourceUtils.valueConvertToInputFormat("2026-09-09T19:00:51.0Z", DATETIME_TYPE));

    assertEquals(INPUT_DATETIME, result);
  }

  /**
   * Runs a conversion with {@code dateFormat.java} and {@code dateTimeFormat.java} stubbed, which
   * is what {@link DataSourceUtils} reads to translate between the classic input format and HQL.
   *
   * @param conversion
   *     the conversion to run
   * @param <T>
   *     the type returned by the conversion
   * @return the value returned by the conversion
   * @throws ParseException
   *     if a date in the conversion cannot be read
   * @throws JSONException
   *     if the JSON handling in the conversion fails
   */
  private static <T> T withDateFormats(DateConversion<T> conversion)
      throws ParseException, JSONException {
    Properties props = new Properties();
    props.setProperty("dateFormat.java", INPUT_DATE_FORMAT);
    props.setProperty("dateTimeFormat.java", INPUT_DATE_FORMAT + " HH:mm:ss");
    OBPropertiesProvider instance = mock(OBPropertiesProvider.class);
    when(instance.getOpenbravoProperties()).thenReturn(props);
    try (MockedStatic<OBPropertiesProvider> provider = mockStatic(OBPropertiesProvider.class)) {
      provider.when(OBPropertiesProvider::getInstance).thenReturn(instance);
      return conversion.run();
    }
  }

  /**
   * A conversion of a date value, so the calls under test can be passed to
   * {@link #withDateFormats} as lambdas.
   *
   * @param <T>
   *     the type the conversion returns
   */
  @FunctionalInterface
  private interface DateConversion<T> {
    T run() throws ParseException, JSONException;
  }
}
