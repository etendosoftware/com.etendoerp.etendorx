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

import java.math.BigDecimal;
import java.text.ParseException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;

/**
 * Tests of the number rendering done by
 * {@link DataSourceUtils#valueConvertToInputFormat(Object, String)}.
 * <p>
 * The text this method produces is fed back into the classic UI form initialization and re-parsed
 * with the separators configured in {@code Format.xml}, so both sides have to agree on which
 * character is the decimal separator.
 */
class DataSourceUtilsNumberFormatTest {

  private static final String QTY_EDITION_DECIMAL_SEPARATOR = "#DecimalSeparator|qtyEdition";
  private static final String BIG_DECIMAL = "BigDecimal";
  private static final String DECIMAL_COMMA = ",";
  private static final String TWELVE_POINT_THIRTY_FOUR = "12.34";

  /**
   * Regression test for ETP-5036: on an instance whose {@code Format.xml} follows the
   * Spanish/European convention (decimal {@code ","}, grouping {@code "."}) the decimal point
   * emitted by {@code BigDecimal.toString()} was stripped downstream as a grouping separator, so
   * the value was persisted multiplied by a power of ten.
   */
  @Test
  void bigDecimalIsRenderedWithTheConfiguredCommaSeparator() throws ParseException {
    assertInputFormat(TWELVE_POINT_THIRTY_FOUR, DECIMAL_COMMA, "12,34");
  }

  /**
   * The corruption did not need the amount to exceed one thousand: it was the decimal point
   * itself, and not a real grouping separator, that was being stripped.
   */
  @Test
  void bigDecimalBelowThousandIsRenderedWithTheConfiguredCommaSeparator() throws ParseException {
    assertInputFormat("1.81", DECIMAL_COMMA, "1,81");
  }

  /**
   * On an instance configured with the English convention the decimal point is kept.
   */
  @Test
  void bigDecimalIsRenderedWithTheConfiguredDotSeparator() throws ParseException {
    assertInputFormat(TWELVE_POINT_THIRTY_FOUR, ".", TWELVE_POINT_THIRTY_FOUR);
  }

  /**
   * Converts the given value against a session configured with the given decimal separator and
   * checks the resulting input format text.
   *
   * @param value
   *     The value to convert, written with a decimal point.
   * @param decimalSeparator
   *     The decimal separator configured for the {@code qtyEdition} format.
   * @param expected
   *     The expected input format text.
   * @throws ParseException
   *     If the conversion fails, which never happens for a numeric value.
   */
  private void assertInputFormat(String value, String decimalSeparator, String expected)
      throws ParseException {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getSessionValue(QTY_EDITION_DECIMAL_SEPARATOR)).thenReturn(decimalSeparator);
    RequestContext requestContext = mock(RequestContext.class);
    when(requestContext.getVariablesSecureApp()).thenReturn(vars);
    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class)) {
      requestContextMock.when(RequestContext::get).thenReturn(requestContext);
      assertEquals(expected,
          DataSourceUtils.valueConvertToInputFormat(new BigDecimal(value), BIG_DECIMAL));
    }
  }
}
