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
 * All portions are Copyright (C) 2021-2024 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package com.etendoerp.etendorx.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TokenEncryptionUtil: encrypt/decrypt round-trip, format validation,
 * wrong-key failure, invalid ciphertext, and missing/invalid key handling.
 */
@ExtendWith(MockitoExtension.class)
class TokenEncryptionUtilTest {

  private static final String ENCRYPTION_KEY_PROPERTY = "etrx.token.encryption.key";

  /**
   * A valid 64-character hex key (32 bytes) for use in tests.
   */
  private static final String VALID_KEY = "a".repeat(64);
  /**
   * A second valid key used to test cross-key decryption failure.
   */
  private static final String OTHER_KEY = "b".repeat(64);

  private MockedStatic<OBPropertiesProvider> obPropsStatic;
  private Properties props;

  @BeforeEach
  void setUp() {
    props = new Properties();
    props.setProperty(ENCRYPTION_KEY_PROPERTY, VALID_KEY);

    OBPropertiesProvider providerMock = mock(OBPropertiesProvider.class);
    lenient().when(providerMock.getOpenbravoProperties()).thenReturn(props);

    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(providerMock);
  }

  @AfterEach
  void tearDown() {
    if (obPropsStatic != null) {
      obPropsStatic.close();
    }
  }

  // ---------------------------------------------------------------------------
  // Round-trip
  // ---------------------------------------------------------------------------

  /**
   * Encrypting and then decrypting returns the original plaintext.
   */
  @Test
  void testEncryptDecryptRoundTripReturnsOriginalPlaintext() {
    String plaintext = "my-secret-access-token";
    String ciphertext = TokenEncryptionUtil.encrypt(plaintext);
    String decrypted = TokenEncryptionUtil.decrypt(ciphertext);
    assertEquals(plaintext, decrypted);
  }

  /**
   * An empty string round-trips cleanly.
   */
  @Test
  void testEncryptDecryptRoundTripEmptyString() {
    String ciphertext = TokenEncryptionUtil.encrypt("");
    assertEquals("", TokenEncryptionUtil.decrypt(ciphertext));
  }

  /**
   * A long token string (1 000 chars) round-trips cleanly.
   */
  @Test
  void testEncryptDecryptRoundTripLongString() {
    String longToken = "x".repeat(1000);
    assertEquals(longToken, TokenEncryptionUtil.decrypt(TokenEncryptionUtil.encrypt(longToken)));
  }

  /**
   * Unicode content round-trips cleanly.
   */
  @Test
  void testEncryptDecryptRoundTripUnicode() {
    String unicode = "token-\u00e9\u00e0\u00fc-\u4e2d\u6587";
    assertEquals(unicode, TokenEncryptionUtil.decrypt(TokenEncryptionUtil.encrypt(unicode)));
  }

  // ---------------------------------------------------------------------------
  // Output format
  // ---------------------------------------------------------------------------

  /**
   * Ciphertext has exactly two colon-separated segments (base64(iv):base64(ciphertext+authTag)).
   */
  @Test
  void testEncryptOutputFormatTwoColonSeparatedSegments() {
    String ciphertext = TokenEncryptionUtil.encrypt("hello");
    String[] parts = ciphertext.split(":", 2);
    assertEquals(2, parts.length,
        "Ciphertext must have exactly two colon-separated segments: base64(iv):base64(ciphertext+authTag)");
  }

  /**
   * Both segments are non-empty Base64 strings.
   */
  @Test
  void testEncryptOutputFormatNonEmptyBase64Segments() {
    String ciphertext = TokenEncryptionUtil.encrypt("hello");
    String[] parts = ciphertext.split(":", 2);
    assertTrue(parts[0].length() > 0, "IV segment must not be empty");
    assertTrue(parts[1].length() > 0, "Ciphertext+authTag segment must not be empty");
  }

  // ---------------------------------------------------------------------------
  // Random IV — same plaintext produces different ciphertexts
  // ---------------------------------------------------------------------------

  /**
   * Encrypting the same plaintext twice yields different ciphertexts (random IV).
   */
  @Test
  void testEncryptRandomIVDifferentCiphertextsForSamePlaintext() {
    String c1 = TokenEncryptionUtil.encrypt("same-plaintext");
    String c2 = TokenEncryptionUtil.encrypt("same-plaintext");
    assertNotEquals(c1, c2,
        "Each call to encrypt() must produce a different ciphertext due to random IV");
  }

  // ---------------------------------------------------------------------------
  // Wrong key
  // ---------------------------------------------------------------------------

  /**
   * Decrypting with a different key returns null.
   */
  @Test
  void testDecryptWrongKeyReturnsNull() {
    String ciphertext = TokenEncryptionUtil.encrypt("secret");

    // Switch to a different key
    props.setProperty(ENCRYPTION_KEY_PROPERTY, OTHER_KEY);

    assertNull(TokenEncryptionUtil.decrypt(ciphertext),
        "decrypt() must return null when the key differs from the one used to encrypt");
  }

  // ---------------------------------------------------------------------------
  // Invalid / malformed ciphertext
  // ---------------------------------------------------------------------------

  /**
   * A ciphertext with no colon (single segment) returns null.
   */
  @Test
  void testDecryptInvalidInputNoColonReturnsNull() {
    assertNull(TokenEncryptionUtil.decrypt("notvalidatall"));
  }

  /**
   * A ciphertext with a corrupted payload (zeros) returns null due to GCM auth-tag failure.
   */
  @Test
  void testDecryptInvalidInputCorruptedPayloadReturnsNull() {
    String ciphertext = TokenEncryptionUtil.encrypt("data");
    String[] parts = ciphertext.split(":", 2);
    // Replace the ciphertext+authTag segment with zeros of the same length
    String corrupted = parts[0] + ":" + "A".repeat(parts[1].length());
    assertNull(TokenEncryptionUtil.decrypt(corrupted));
  }

  /**
   * An empty string returns null.
   */
  @Test
  void testDecryptInvalidInputEmptyStringReturnsNull() {
    assertNull(TokenEncryptionUtil.decrypt(""));
  }

  // ---------------------------------------------------------------------------
  // Missing / invalid encryption key
  // ---------------------------------------------------------------------------

  /**
   * encrypt() throws OBException when the property is not set.
   */
  @Test
  void testEncryptMissingKeyThrowsOBException() {
    props.remove(ENCRYPTION_KEY_PROPERTY);
    assertThrows(OBException.class, () -> TokenEncryptionUtil.encrypt("test"),
        "encrypt() must throw OBException when etrx.token.encryption.key is not set");
  }

  /**
   * encrypt() throws OBException when the key is shorter than 64 chars.
   */
  @Test
  void testEncryptShortKeyThrowsOBException() {
    props.setProperty(ENCRYPTION_KEY_PROPERTY, "tooshort");
    assertThrows(OBException.class, () -> TokenEncryptionUtil.encrypt("test"),
        "encrypt() must throw OBException when the key is shorter than 64 chars");
  }

  /**
   * encrypt() throws OBException when the key is longer than 64 chars.
   */
  @Test
  void testEncryptLongKeyThrowsOBException() {
    props.setProperty(ENCRYPTION_KEY_PROPERTY, "a".repeat(65));
    assertThrows(OBException.class, () -> TokenEncryptionUtil.encrypt("test"),
        "encrypt() must throw OBException when the key is longer than 64 chars");
  }

  /**
   * decrypt() returns null when the property is not set (key lookup throws; catch returns null).
   */
  @Test
  void testDecryptMissingKeyReturnsNull() {
    String ciphertext = TokenEncryptionUtil.encrypt("test");
    props.remove(ENCRYPTION_KEY_PROPERTY);
    assertNull(TokenEncryptionUtil.decrypt(ciphertext),
        "decrypt() must return null when etrx.token.encryption.key is not set");
  }

  // ---------------------------------------------------------------------------
  // Non-Base64 input
  // ---------------------------------------------------------------------------

  /**
   * A ciphertext whose IV segment is not valid Base64 returns null.
   * Base64.getDecoder().decode() throws IllegalArgumentException for "!!!not-base64!!!" —
   * this must be caught and return null rather than propagating.
   */
  @Test
  void testDecryptWithNonBase64IvSegmentReturnsNull() {
    // "!!!not-base64!!!" is not valid Base64 — Base64.getDecoder().decode() throws IllegalArgumentException
    // This should be caught and return null
    assertNull(TokenEncryptionUtil.decrypt("!!!not-base64!!!:validBase64Payload=="));
  }

  /**
   * A ciphertext whose payload segment is not valid Base64 returns null.
   */
  @Test
  void testDecryptWithNonBase64PayloadSegmentReturnsNull() {
    String validIv = Base64.getEncoder().encodeToString(new byte[12]);
    assertNull(TokenEncryptionUtil.decrypt(validIv + ":!!!not-base64!!!"));
  }

  // ---------------------------------------------------------------------------
  // Utility-class constructor guard
  // ---------------------------------------------------------------------------

  /**
   * Attempting to instantiate TokenEncryptionUtil via reflection throws IllegalStateException.
   */
  @Test
  void testConstructorThrowsIllegalStateException() throws Exception {
    Constructor<TokenEncryptionUtil> constructor =
        TokenEncryptionUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
        constructor::newInstance);
    assertTrue(thrown.getCause() instanceof IllegalStateException);
    assertEquals("Utility class", thrown.getCause().getMessage());
  }
}
